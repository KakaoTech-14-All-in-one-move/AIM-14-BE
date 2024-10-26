package com.example.pitching.call.handler;

import com.example.pitching.call.dto.properties.ServerProperties;
import com.example.pitching.call.operation.res.Hello;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SinkManager {
    private static final Map<String, Sinks.Many<String>> voiceSinkMap = new ConcurrentHashMap<>();
    private static final Map<String, Sinks.Many<String>> videoSinkMap = new ConcurrentHashMap<>();
    private final ConvertService convertService;
    private final ServerProperties serverProperties;

    public Flux<String> registerVoice(Mono<String> userIdMono) {
        return userIdMono
                .flux()
                .flatMap(userId -> initSinkMap(userId, voiceSinkMap));
    }

    public void addVoiceMessage(Mono<String> userIdMono, String message) {
        userIdMono
                .doOnSuccess(userId -> {
                    Sinks.Many<String> voiceSink = voiceSinkMap.get(userId);
                    voiceSink.tryEmitNext(message);
                }).subscribe();
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
        sendHello(sink);
        return sink.asFlux();
    }


    private void sendHello(Sinks.Many<String> voiceSink) {
        String jsonHelloEvent = convertService.eventToJson(Hello.of(serverProperties.getHeartbeatInterval()));
        voiceSink.tryEmitNext(jsonHelloEvent);
    }
}
