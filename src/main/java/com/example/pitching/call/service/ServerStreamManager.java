package com.example.pitching.call.service;

import com.example.pitching.call.config.RedisConfig;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;
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
    private final Map<String, Sinks.Many<String>> serverSinkMap = new ConcurrentHashMap<>();
    private final Map<String, Disposable> serverStream = new ConcurrentHashMap<>();
    private final StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;
    private final ConvertService convertService;
    private final RedisReactiveCommands<String, String> redisCommands;
    private final RedisConfig.RedisProperties redisProperties;

    public ServerStreamManager(ConvertService convertService,
                               ReactiveRedisConnectionFactory redisConnectionFactory,
                               RedisReactiveCommands<String, String> redisCommands,
                               RedisConfig.RedisProperties redisProperties) {
        this.convertService = convertService;
        this.redisCommands = redisCommands;
        this.redisProperties = redisProperties;

        var options = StreamReceiver.StreamReceiverOptions.builder()
                .pollTimeout(Duration.ofMillis(100L))
                .build();

        streamReceiver = StreamReceiver.create(redisConnectionFactory, options);
    }

    public Mono<String> addVoiceMessageToStream(String serverId, String message) {
        XAddArgs xAddArgs = new XAddArgs().maxlen(redisProperties.maxlen()).approximateTrimming(true);
        return redisCommands
                .xadd(getServerStreamRedisKey(serverId), xAddArgs, Map.of("message", message));
    }

    public Flux<String> getMessageFromServerSink(String serverId) {
        return serverSinkMap.get(serverId).asFlux();
    }

    @EventListener(ApplicationReadyEvent.class)
    private void initialize() {
        // TODO: 모든 serverId를 조회하여 해당 serverId에 대한 Sink 생성 후 Stream 구독 (반복문)
        String serverId = "12345";
        registerServerStream(serverId);
    }

    @EventListener(ContextClosedEvent.class)
    private void onShutdown() {
        log.info("Clean up server stream resources...");
        serverStream.forEach((serverId, disposable) -> {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
                log.info("Unregister server stream: {}", serverId);
            }
        });
        log.info("Clean up server stream resources... Done");
    }

    private void registerServerStream(String serverId) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        serverSinkMap.put(serverId, sink);
        var streamOffsetForVoice = StreamOffset.create(getServerStreamRedisKey(serverId), ReadOffset.latest());
        Disposable subscription = streamReceiver.receive(streamOffsetForVoice)
                .subscribe(record -> {
                    String sequence = record.getId().getValue();
                    String message = record.getValue().get("message");
                    addSequenceBeforeEmit(serverId, sequence, message);
                });
        serverStream.put(serverId, subscription);
        log.info("Register server stream: {}", serverId);
    }

    private void addSequenceBeforeEmit(String serverId, String sequence, String jsonMessage) {
        convertService.convertJsonToEventWithSequence(sequence, jsonMessage)
                .doOnError(throwable -> log.error("Error occurred while converting json to event: ", throwable))
                .subscribe(serverEvent -> {
                    Sinks.Many<String> serverSink = serverSinkMap.get(serverId);
                    serverSink.tryEmitNext(convertService.convertObjectToJson(serverEvent));
                    log.info("ServerEvent emitted to {}: {}", serverId, jsonMessage);
                });
    }

    private String getServerStreamRedisKey(String serverId) {
        return String.format("server:%s:events", serverId);
    }
}
