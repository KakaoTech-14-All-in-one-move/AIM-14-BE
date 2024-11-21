package com.example.pitching.user.service;

import com.example.pitching.auth.domain.User;
import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.user.domain.Server;
import com.example.pitching.user.domain.UserServerMembership;
import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.dto.ServerResponse;
import com.example.pitching.user.repository.ServerRepository;
import com.example.pitching.user.repository.UserServerMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final UserServerMembershipRepository userServerMembershipRepository;
    private final FileStorageService fileStorageService;

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

    public Mono<Void> inviteMember(Long serverId, String email) {
        return serverRepository.findById(serverId)
                .switchIfEmpty(Mono.error(new RuntimeException("Server not found")))
                .zipWith(userRepository.findByEmail(email)
                        .switchIfEmpty(Mono.error(new RuntimeException("User not found"))))
                .flatMap(tuple -> {
                    Server server = tuple.getT1();
                    User user = tuple.getT2();

                    return userServerMembershipRepository.findByServerIdAndEmail(serverId, email)
                            .hasElement()
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new RuntimeException("User is already a member"));
                                }

                                UserServerMembership membership = UserServerMembership.createMembership(email, serverId);
                                return userServerMembershipRepository.save(membership);
                            });
                })
                .then();
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

    public Mono<String> updateServerImage(Long serverId, FilePart file) {
        return findServer(serverId)
                .flatMap(server -> storeNewImage(file)
                        .flatMap(newImageUrl -> updateServerAndHandleOldImage(server, newImageUrl)));
    }

    private Mono<Server> findServer(Long serverId) {
        return serverRepository.findById(serverId)
                .switchIfEmpty(Mono.error(
                        new Error("Server not found with id: " + serverId)
                ));
    }

    private Mono<String> storeNewImage(FilePart file) {
        return fileStorageService.store(file)
                .onErrorMap(error ->
                        new Error("Failed to store new server image: " + error.getMessage())
                );
    }

    private Mono<String> updateServerAndHandleOldImage(Server server, String newImageUrl) {
        String oldImageUrl = server.getServerImage();

        return Mono.just(server)
                .doOnNext(existingServer -> existingServer.setServerImage(newImageUrl))
                .flatMap(serverRepository::save)
                .map(Server::getServerImage)
                .doOnSuccess(__ -> {
                    if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                        deleteOldImage(oldImageUrl);
                    }
                })
                .doOnSuccess(__ -> log.info("Updated server image. Server ID: {}", server.getServerId()));
    }

    private void deleteOldImage(String imageUrl) {
        fileStorageService.delete(imageUrl)
                .doOnSuccess(__ -> log.info("Successfully deleted old server image: {}", imageUrl))
                .doOnError(error -> log.error("Failed to delete old server image: {}", imageUrl, error))
                .subscribe();
    }

    public Mono<Void> deleteServer(Long serverId, String email) {
        return serverRepository.findById(serverId)
                .flatMap(server ->
                        userServerMembershipRepository.deleteByServerIdAndEmail(serverId, email)
                                .then(userServerMembershipRepository.countByServerId(serverId))
                                .filter(memberCount -> memberCount == 0)
                                .flatMap(__ ->
                                        Mono.justOrEmpty(server.getServerImage())
                                                .flatMap(fileStorageService::delete)
                                                .then(serverRepository.deleteById(serverId))
                                                .switchIfEmpty(serverRepository.deleteById(serverId))
                                )
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