package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.res.Hello;
import com.example.pitching.call.operation.res.Response;
import lombok.extern.slf4j.Slf4j;
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

    public SinkManager(ConvertService convertService,
                       ServerProperties serverProperties,
                       ReactiveStringRedisTemplate redisTemplate,
                       ReactiveRedisConnectionFactory redisConnectionFactory) {
        this.convertService = convertService;
        this.serverProperties = serverProperties;

        this.streamOperations = redisTemplate.opsForStream();

        var options = StreamReceiver.StreamReceiverOptions.builder()
                .pollTimeout(Duration.ofMillis(100L))
                .build();

        streamReceiver = StreamReceiver.create(redisConnectionFactory, options);
    }

    public Flux<String> registerVoice(Mono<String> userIdMono) {
        return userIdMono
                .flux()
                .flatMap(userId -> initSinkMap(userId, voiceSinkMap));
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
                    log.info("Subscribe Voice from Redis: {}", record.getId().getValue());
                    addSeqBeforeEmit(userId, record);
                });
        voiceStream.put(userId, subscription);
        log.info("Register Voice Stream: {}", userId);
    }

    private void addSeqBeforeEmit(String userId, MapRecord<String, String, String> record) {
        var values = record.getValue();
        var message = values.get("message");
        String seq = record.getId().getValue();
        Response response = convertService.createEventFromJson(message).setValue(seq);
        Sinks.Many<String> voiceSink = voiceSinkMap.get(userId);
        voiceSink.tryEmitNext(convertService.eventToJson(response));
    }

    public void addVoiceMessage(Mono<String> userIdMono, String message) {
        userIdMono
                .flatMap(userId -> streamOperations.add(userId + ":voice",
                        Map.of("message", message)))
                .subscribe(record -> log.info("Publish Voice to Redis: {}", record));
    }

    public Flux<String> registerVideo(Mono<String> getUserIdFromContext) {
        return getUserIdFromContext
                .flux()
                .flatMap(userId -> initSinkMap(userId, videoSinkMap));
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
