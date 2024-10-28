package com.example.pitching.call.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.netty.udp.UdpServer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceSfuServer {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<String, InetSocketAddress> clients = new ConcurrentHashMap<>();

    @EventListener(ApplicationStartedEvent.class)
    public void start() {
        executorService.execute(() -> {
            UdpServer.create()
                    .wiretap(true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .handle((in, out) ->
                            out.sendObject(
                                    in.receiveObject()
                                            .cast(DatagramPacket.class)
                                            .map(packet -> {
                                                ByteBuf receivedBuf = packet.content().retain();
                                                log.info("MESSAGE : {}", receivedBuf.toString(CharsetUtil.UTF_8));
                                                ByteBuf buf = Unpooled.copiedBuffer("hello", CharsetUtil.UTF_8);
                                                return new DatagramPacket(buf, packet.sender());
                                            })
                            )
                    )
                    .bindAddress(() -> new InetSocketAddress(9090))
                    .bindNow(Duration.ofSeconds(30))
                    .onDispose()
                    .doOnSubscribe(s -> log.info("UDP Server successfully bound to port 9090."))
                    .doOnError(e -> log.error("Failed to bind UDP server.", e))
                    .block();
        });
        executorService.shutdown();
    }
}
