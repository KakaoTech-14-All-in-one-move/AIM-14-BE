package com.example.pitching.user.service;

import com.example.pitching.user.domain.Server;
import com.example.pitching.user.domain.UserServerMembership;
import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.dto.ServerResponse;
import com.example.pitching.user.repository.ServerRepository;
import com.example.pitching.user.repository.UserServerMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {
    private final ServerRepository serverRepository;
    private final UserServerMembershipRepository userServerMembershipRepository;

    public Mono<ServerResponse> createServer(ServerRequest request, String email) {
        Server newServer = Server.createNewServer(
                request.server_name(),
                request.server_image()
        );

        return serverRepository.save(newServer)
                .flatMap(server -> {
                    UserServerMembership membership = new UserServerMembership(
                            email,
                            server.getServerId(),
                            LocalDateTime.now()
                    );
                    return userServerMembershipRepository.save(membership)
                            .thenReturn(server);
                })
                .map(this::mapToResponse);
    }

    public Flux<ServerResponse> getUserServers(String email) {
        return serverRepository.findServersByUserEmail(email)
                .map(this::mapToResponse);
    }

    public Mono<ServerResponse> updateServerName(Long serverId, String newName, String email) {
        log.info("Updating server name: serverId={}, newName={}, email={}", serverId, newName, email);

        return userServerMembershipRepository.findByServerIdAndEmail(serverId, email)
                .switchIfEmpty(Mono.error(new RuntimeException("Server membership not found")))
                .doOnNext(membership -> log.info("Found membership: {}", membership))
                .flatMap(membership -> serverRepository.findByServerId(serverId)
                        .doOnNext(server -> server.setServerName(newName))
                        .flatMap(serverRepository::save)
                        .doOnSuccess(updatedServer -> log.info("Updated server name: {}", updatedServer))
                )
                .map(this::mapToResponse)
                .doOnError(e -> log.error("Failed to update server name: serverId={}, error={}", serverId, e.getMessage()));
    }

    public Mono<ServerResponse> updateServerImage(Long serverId, String newImage, String email) {
        log.info("Updating server image: serverId={}, newImage={}, email={}", serverId, newImage, email);

        return userServerMembershipRepository.findByServerIdAndEmail(serverId, email)
                .switchIfEmpty(Mono.error(new RuntimeException("Server membership not found")))
                .doOnNext(membership -> log.info("Found membership: {}", membership))
                .flatMap(membership -> serverRepository.findByServerId(serverId)
                        .doOnNext(server -> server.setServerImage(newImage))
                        .flatMap(serverRepository::save)
                        .doOnSuccess(updatedServer -> log.info("Updated server image: {}", updatedServer))
                )
                .map(this::mapToResponse)
                .doOnError(e -> log.error("Failed to update server image: serverId={}, error={}", serverId, e.getMessage()));
    }

    public Mono<Void> deleteServer(Long serverId, String email) {
        return Mono.from(
                userServerMembershipRepository.deleteByServerIdAndEmail(serverId, email)
                        .thenMany(userServerMembershipRepository.countByServerId(serverId))
                        .filter(memberCount -> memberCount == 0)
                        .flatMap(memberCount -> serverRepository.deleteById(serverId))
        );
    }

    private ServerResponse mapToResponse(Server server) {
        return new ServerResponse(
                server.getServerId(),
                server.getServerName(),
                server.getServerImage(),
                server.getCreatedAt().toString()
        );
    }
}