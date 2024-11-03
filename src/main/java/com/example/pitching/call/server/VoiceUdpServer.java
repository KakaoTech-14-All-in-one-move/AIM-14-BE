package com.example.pitching.call.server;

import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.netty.NettyOutbound;
import reactor.netty.udp.UdpInbound;
import reactor.netty.udp.UdpOutbound;
import reactor.netty.udp.UdpServer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceUdpServer {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final CallUdpClient callUdpClient;

    @EventListener(ApplicationStartedEvent.class)
    public void start() {
        executorService.execute(() -> {
            UdpServer.create()
                    .wiretap(true)
                    .metrics(true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handle(this::handleVoiceData)
                    .bindAddress(() -> new InetSocketAddress(9090))
                    .bindNow(Duration.ofSeconds(30))
                    .onDispose()
                    .doOnSubscribe(_ -> log.info("Voice UDP Server successfully bound to port 9090."))
                    .doOnError(e -> log.error("Failed to bind Voice UDP server.", e))
                    .doFinally(signalType -> {
                        log.info("Voice UdpServer is shutting down: {}", signalType);
                        executorService.shutdown();
                    })
                    .block();
        });
    }

    private NettyOutbound handleVoiceData(UdpInbound in, UdpOutbound out) {
        return out.sendObject(
                // TODO: 자신이 송출한 데이터는 되돌려 받지 않기 때문에 Void를 반환하는데, UDP 연결이 안끊기고 있음
                in.receiveObject()
                        .cast(DatagramPacket.class)
                        .flatMap(callUdpClient::handleDatagramPacket)
                        .onErrorResume(e -> {
                            log.error("Failed to handle voice data.", e);
                            return Flux.empty();
                        })
        );
    }
}
