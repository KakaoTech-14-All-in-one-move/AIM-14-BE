package com.example.pitching.call.handler;

import com.example.pitching.call.config.RedisConfig;
import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.res.Hello;
import com.example.pitching.call.operation.res.Response;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ServerStreamManager {
    public static final Map<String, Sinks.Many<String>> serverSinkMap = new ConcurrentHashMap<>();
    private final Map<String, Disposable> serverStream = new ConcurrentHashMap<>();
    private final ReactiveStreamOperations<String, Object, Object> streamOperations;
    private final StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;
    private final ConvertService convertService;
    private final ServerProperties serverProperties;
    private final RedisReactiveCommands<String, String> redisCommands;
    private final RedisConfig.RedisProperties redisProperties;

    public ServerStreamManager(ConvertService convertService,
                               ServerProperties serverProperties,
                               ReactiveStringRedisTemplate redisTemplate,
                               ReactiveRedisConnectionFactory redisConnectionFactory,
                               RedisReactiveCommands<String, String> redisCommands,
                               RedisConfig.RedisProperties redisProperties) {
        this.convertService = convertService;
        this.serverProperties = serverProperties;
        this.streamOperations = redisTemplate.opsForStream();
        this.redisCommands = redisCommands;
        this.redisProperties = redisProperties;

        var options = StreamReceiver.StreamReceiverOptions.builder()
                .pollTimeout(Duration.ofMillis(100L))
                .build();

        streamReceiver = StreamReceiver.create(redisConnectionFactory, options);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        // TODO: 모든 serverId를 조회하여 해당 serverId에 대한 Sink 생성 후 Stream 구독 (반복문)
        String serverId = "sample";
        registerServerStream(serverId);
    }

    public void registerServerStream(String serverId) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        serverSinkMap.put(serverId, sink);
        var streamOffsetForVoice = StreamOffset.create(getServerStreamRedisKey(serverId), ReadOffset.latest());
        Disposable subscription = streamReceiver.receive(streamOffsetForVoice)
                .subscribe(record -> {
                    String seq = record.getId().getValue();
                    String message = record.getValue().get("message");
                    addSeqBeforeEmit(serverId, seq, message);
                });
        serverStream.put(serverId, subscription);
        log.info("Register Server Stream: {}", serverId);
    }

    public void unregisterServerStream(String serverId) {
        var subscription = serverStream.get(serverId);
        if (subscription != null) {
            subscription.dispose();
            log.info("Unregister Server Stream: {}", serverId);
        }
    }

    public Flux<String> getMessageFromServerSink(String serverId) {
        return serverSinkMap.get(serverId).asFlux();
    }

    private String getServerStreamRedisKey(String serverId) {
        return String.format("server:%s:events", serverId);
    }


    public Flux<String> registerVoice(String userId) {
        return initSinkMap(userId, serverSinkMap);
    }

    public void addMissedServerMessageToStream(String userId, String lastRecordId) {
        Range<String> range = Range.rightOpen(lastRecordId, "+");
        streamOperations.range(userId + ":voice", range)
                .skip(1)
                .subscribe(record -> {
                    String seq = record.getId().getValue();
                    String message = record.getValue().get("message").toString();
                    addSeqBeforeEmit(userId, seq, message);
                });
    }

    public void addVoiceMessageToStream(String userId, String message) {
        XAddArgs xAddArgs = new XAddArgs().maxlen(redisProperties.maxlen()).approximateTrimming(true);
        redisCommands
                .xadd(userId + ":voice", xAddArgs, Map.of("message", message))
                .subscribe(record -> log.info("Publish Voice to Redis: {}", record));
    }

    private void registerVoiceStream(String userId) {
        var streamOffsetForVoice = StreamOffset.create(userId + ":voice", ReadOffset.latest());
        Disposable subscription = streamReceiver.receive(streamOffsetForVoice)
                .subscribe(record -> {
                    String seq = record.getId().getValue();
                    log.info("Subscribe Voice from Redis: {}", seq);
                    String message = record.getValue().get("message");
//                    addSeqBeforeEmit(userId, seq, message, serverSinkMap);
                });
        serverStream.put(userId, subscription);
        log.info("Register Voice Stream: {}", userId);
    }

    private void addSeqBeforeEmit(String serverId, String seq, String message) {
        Response response = convertService.createResFromJson(message).setSeq(seq);
        Sinks.Many<String> serverSink = serverSinkMap.get(serverId);
        String jsonMessage = convertService.eventToJson(response);
        serverSink.tryEmitNext(jsonMessage);
        log.info("JsonMessage emitted: {}", jsonMessage);
    }

    private Flux<String> initSinkMap(String userId, Map<String, Sinks.Many<String>> sinkMap) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        sinkMap.put(userId, sink);
        registerVoiceStream(userId);
        sendHello(sink);
        return sink.asFlux();
    }

    private void sendHello(Sinks.Many<String> sink) {
        String jsonHelloEvent = convertService.eventToJson(Hello.of(serverProperties.getHeartbeatInterval()));
        sink.tryEmitNext(jsonHelloEvent);
    }
}
