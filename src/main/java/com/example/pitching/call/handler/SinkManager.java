package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.res.Hello;
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
    private final ConvertService convertService;
    private final ServerProperties serverProperties;
    private ReactiveStreamOperations<String, Object, Object> streamOperations;
    private StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;

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

    private void registerVoiceStream(String userId) {
        var streamOffsetForVoice = StreamOffset.create(userId + ":voice", ReadOffset.latest());
        streamReceiver.receive(streamOffsetForVoice)
                .subscribe(record -> {
                    log.info("Subscribe Voice from Redis: {}", record);
                    var values = record.getValue();
                    var message = values.get("message");
                    Sinks.Many<String> voiceSink = voiceSinkMap.get(userId);
                    voiceSink.tryEmitNext(message);
                });
    }

    public void addVoiceMessage(Mono<String> userIdMono, String message) {
        userIdMono
                .flatMap(userId -> streamOperations.add(userId + ":voice",
                        Map.of("message", message, "seq", "")))
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
