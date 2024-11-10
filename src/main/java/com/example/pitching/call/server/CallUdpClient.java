package com.example.pitching.call.server;

import com.example.pitching.call.operation.response.ChannelEnterResponse;
import com.example.pitching.call.service.UdpAddressManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableChannel;
import reactor.netty.udp.UdpClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CallUdpClient {
    private final ConcurrentHashMap<String, Sinks.Many<DatagramPacket>> channelSinkMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Sinks.Many<DatagramPacket>> clientSinkMap = new ConcurrentHashMap<>();
    private final Map<String, Disposable> clientDisposable = new ConcurrentHashMap<>();
    private final UdpAddressManager udpAddressManager;
    private final UdpClient client;

    public CallUdpClient(UdpAddressManager udpAddressManager) {
        this.udpAddressManager = udpAddressManager;
        this.client = UdpClient.create()
                .metrics(true)
                .wiretap(true)
                .doOnChannelInit((observer, channel, remote) ->
                        channel.pipeline().addFirst(new LoggingHandler("reactor.netty.examples")))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
    }

    public void initializeCallSink(ChannelEnterResponse channelEnterResponse) {
        // Create clientSink
        if (!clientSinkMap.containsKey(channelEnterResponse.userId())) {
            Sinks.Many<DatagramPacket> clientSink = clientSinkMap
                    .computeIfAbsent(channelEnterResponse.userId(), ignored -> Sinks.many().unicast().onBackpressureBuffer());
            Disposable disposable = clientSink
                    .asFlux()
                    .flatMap(datagramPacket -> sendPacket(datagramPacket, channelEnterResponse.userId()))
                    .subscribe();
            clientDisposable.put(channelEnterResponse.userId(), disposable);
            // Subscribe channelSink
            channelSinkMap.computeIfAbsent(channelEnterResponse.channelId(), ignored -> Sinks.many().multicast().directBestEffort())
                    .asFlux()
                    .flatMap(packet -> tryEmitIfNotSameSender(channelEnterResponse, packet, clientSink))
                    .subscribe();
            log.info("[{}] initialize call sink for channel: {}", channelEnterResponse.userId(), channelEnterResponse.channelId());
        }
    }

    public Mono<Void> handleDatagramPacket(DatagramPacket datagramPacket) {
        return extractChannelId(datagramPacket)
                .flatMap(this::getChannelSink)
                .flatMap(channelSink -> addClientUdpAddress(datagramPacket, channelSink))
                .doOnSuccess(channelSink -> channelSink.tryEmitNext(datagramPacket))
                .then();
    }

    public void cleanupResources(String userId) {
        disposeSubscription(userId);
        removeClientSink(userId);
        removeUdpAddressFromRedis(userId).subscribe();
    }

    public Mono<Boolean> removeUdpAddressFromRedis(String userId) {
        return udpAddressManager.removeUdpAddress(userId);
    }

    private Mono<Boolean> tryEmitIfNotSameSender(ChannelEnterResponse channelEnterResponse, DatagramPacket packet, Sinks.Many<DatagramPacket> clientSink) {
        return udpAddressManager.isSameSender(channelEnterResponse.userId(), packet.sender())
                .filter(Boolean.FALSE::equals)
                .doOnNext(ignored -> clientSink.tryEmitNext(packet));
    }

    private Mono<Sinks.Many<DatagramPacket>> addClientUdpAddress(DatagramPacket datagramPacket, Sinks.Many<DatagramPacket> channelSink) {
        return extractUserId(datagramPacket)
                .flatMap(userId -> udpAddressManager.addUserAddress(userId, datagramPacket.sender()))
                .thenReturn(channelSink);
    }

    private Mono<Sinks.Many<DatagramPacket>> getChannelSink(String channelId) {
        // TODO: get 에서 에러가 발생하면 서버가 끊김
        return Mono.justOrEmpty(channelSinkMap.get(channelId))
                .switchIfEmpty(Mono.error(new RuntimeException("Cannot find channel sink: " + channelId)));
    }

    // 채널 삭제 시 싱크 삭제
    private Mono<Void> sendPacket(DatagramPacket datagramPacket, String userId) {
        return getClientAddress(userId)
                .flatMap(clientAddress ->
                        this.client
                                .remoteAddress(() -> clientAddress)
                                .handle((in, out) -> out.sendObject(Mono.just(datagramPacket)))
                                .connect()
                                .doOnSuccess(ignored -> log.info("Sent packet to client at: {}", clientAddress))
                                .doOnError(error -> log.error("Failed to send packet to client at: {}", clientAddress, error))
                                .flatMap(DisposableChannel::onDispose)
                );
    }

    private Mono<InetSocketAddress> getClientAddress(String userId) {
        return udpAddressManager.getUserAddress(userId)
                .map(udpAddress -> new InetSocketAddress(udpAddress.ip(), udpAddress.port()));
    }

    private Mono<String> extractChannelId(DatagramPacket packet) {
        return Mono.defer(() -> {
                    int uuidLength = 36;
                    byte[] channelIdBytes = new byte[uuidLength];
                    packet.content().readBytes(channelIdBytes, 0, uuidLength);
                    return Mono.just(new String(channelIdBytes, StandardCharsets.UTF_8));
                })
                .doOnSuccess(channelId -> log.info("Extracted channelId: {}", channelId))
                .onErrorMap(throwable -> new RuntimeException("Cannot extract channelId from packet:", throwable));
    }

    private Mono<String> extractUserId(DatagramPacket packet) {
        return Mono.defer(() -> {
                    ByteBuf content = packet.content();
                    content.readerIndex(36);

                    if (content.readByte() != '|') {
                        return Mono.error(new RuntimeException("Expected '|' delimiter after channelId"));
                    }

                    int userIdLength = content.bytesBefore((byte) '|');
                    if (userIdLength < 0) {
                        return Mono.error(new RuntimeException("Delimiter '|' not found after userId"));
                    }

                    byte[] userIdBytes = new byte[userIdLength];
                    content.readBytes(userIdBytes);
                    content.readByte();

                    return Mono.just(new String(userIdBytes, StandardCharsets.UTF_8));
                })
                .doOnSuccess(userId -> log.info("Extracted userId: {}", userId));
    }

    private void removeClientSink(String userId) {
        clientSinkMap.remove(userId);
    }

    private void disposeSubscription(String userId) {
        if (clientDisposable.containsKey(userId)) clientDisposable.get(userId).dispose();
    }
}
