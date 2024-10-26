package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.res.Hello;
import com.example.pitching.call.operation.res.Response;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import lombok.extern.slf4j.Slf4j;
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
public class SinkManager {
    public static final Map<String, Sinks.Many<String>> voiceSinkMap = new ConcurrentHashMap<>();
    public static final Map<String, Sinks.Many<String>> videoSinkMap = new ConcurrentHashMap<>();
    private final Map<String, Disposable> voiceStream = new ConcurrentHashMap<>();
    private final Map<String, Disposable> videoStream = new ConcurrentHashMap<>();
    private final ReactiveStreamOperations<String, Object, Object> streamOperations;
    private final StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;
    private final ConvertService convertService;
    private final ServerProperties serverProperties;
    private final RedisReactiveCommands<String, String> redisCommands;

    public SinkManager(ConvertService convertService,
                       ServerProperties serverProperties,
                       ReactiveStringRedisTemplate redisTemplate,
                       ReactiveRedisConnectionFactory redisConnectionFactory,
                       RedisReactiveCommands<String, String> redisCommands) {
        this.convertService = convertService;
        this.serverProperties = serverProperties;
        this.streamOperations = redisTemplate.opsForStream();
        this.redisCommands = redisCommands;

        var options = StreamReceiver.StreamReceiverOptions.builder()
                .pollTimeout(Duration.ofMillis(100L))
                .build();

        streamReceiver = StreamReceiver.create(redisConnectionFactory, options);
    }

    public Flux<String> registerVoice(String userId) {
        return initSinkMap(userId, voiceSinkMap);
    }

    public void unregisterVoiceStream(String userId) {
        var subscription = voiceStream.get(userId);
        if (subscription != null) {
            subscription.dispose();
            log.info("Unregister Voice Stream: {}", userId);
        }
    }

    private void registerVoiceStream(String userId) {
        var streamOffsetForVoice = StreamOffset.create(userId + ":voice", ReadOffset.latest());
        Disposable subscription = streamReceiver.receive(streamOffsetForVoice)
                .subscribe(record -> {
                    String seq = record.getId().getValue();
                    log.info("Subscribe Voice from Redis: {}", seq);
                    String message = record.getValue().get("message");
                    addSeqBeforeEmit(userId, seq, message, voiceSinkMap);
                });
        voiceStream.put(userId, subscription);
        log.info("Register Voice Stream: {}", userId);
    }

    private void addSeqBeforeEmit(String userId, String seq, String message,
                                  Map<String, Sinks.Many<String>> sinkMap) {
        Response response = convertService.createResFromJson(message).setSeq(seq);
        Sinks.Many<String> voiceSink = sinkMap.get(userId);
        voiceSink.tryEmitNext(convertService.eventToJson(response));
    }

    public void addMissedVoiceMessageToStream(String userId, String lastRecordId) {
        Range<String> range = Range.rightOpen(lastRecordId, "+");
        streamOperations.range(userId + ":voice", range)
                .skip(1)
                .subscribe(record -> {
                    String seq = record.getId().getValue();
                    String message = record.getValue().get("message").toString();
                    addSeqBeforeEmit(userId, seq, message, voiceSinkMap);
                });
    }

    public void addVoiceMessageToStream(String userId, String message) {
        XAddArgs xAddArgs = new XAddArgs().maxlen(500).approximateTrimming(true);
        redisCommands
                .xadd(userId + ":voice", xAddArgs, Map.of("message", message))
                .subscribe(record -> log.info("Publish Voice to Redis: {}", record));
    }

    public Flux<String> registerVideo(String userId) {
        return initSinkMap(userId, videoSinkMap);
    }

    public Mono<String> getUserIdFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (UserDetails) context.getAuthentication().getPrincipal())
                .doOnNext(userDetails -> log.info("UserDetails: {}", userDetails))
                .map(UserDetails::getUsername)
                .cache();
    }

    private Flux<String> initSinkMap(String userId, Map<String, Sinks.Many<String>> sinkMap) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
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
