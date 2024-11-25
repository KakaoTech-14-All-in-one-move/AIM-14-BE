package com.example.pitching.user.service;

import com.example.pitching.auth.repository.UserRepository;
import com.example.pitching.user.domain.Server;
import com.example.pitching.user.domain.UserServerMembership;
import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.dto.ServerResponse;
import com.example.pitching.user.repository.ServerRepository;
import com.example.pitching.user.repository.UserServerMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final UserServerMembershipRepository userServerMembershipRepository;
    private final FileStorageService fileStorageService;

    public Mono<ServerResponse> createServer(ServerRequest request, String email) {
        return serverRepository.save(Server.createNewServer(
                        request.server_name(),
                        request.server_image()))
                .flatMap(server -> {
                    UserServerMembership membership = UserServerMembership.createMembership(email, server.getServerId());
                    return userServerMembershipRepository.save(membership)
                            .thenReturn(server);
                })
                .map(this::mapToResponse)
                .onErrorMap(DataIntegrityViolationException.class,
                        e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버 생성에 실패했습니다."))
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 생성 중 오류가 발생했습니다."));
    }

    public Mono<Void> inviteMember(Long serverId, String email) {
        return Mono.zip(
                        serverRepository.findById(serverId)
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다."))),
                        userRepository.findByEmail(email)
                                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")))
                )
                .flatMap(tuple -> userServerMembershipRepository.findByServerIdAndEmail(serverId, email)
                        .hasElement()
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 초대된 사용자입니다."));
                            }
                            UserServerMembership membership = UserServerMembership.createMembership(email, serverId);
                            return userServerMembershipRepository.save(membership);
                        }))
                .then()
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "초대 처리 중 오류가 발생했습니다."));
    }

    public Flux<ServerResponse> getUserServers(String email) {
        return serverRepository.findServersByUserEmail(email)
                .map(this::mapToResponse)
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 목록 조회 중 오류가 발생했습니다."));
    }

    public Mono<ServerResponse> updateServerName(Long serverId, String newName, String email) {
        return checkMemberAccess(serverId, email)
                .filter(hasAccess -> hasAccess)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "서버에 대한 접근 권한이 없습니다.")))
                .flatMap(__ -> serverRepository.findByServerId(serverId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다.")))
                        .doOnNext(server -> server.setServerName(newName))
                        .flatMap(serverRepository::save))
                .map(this::mapToResponse)
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 이름 수정 중 오류가 발생했습니다."));
    }

    public Mono<String> updateServerImage(Long serverId, FilePart file, String email) {
        return checkMemberAccess(serverId, email)
                .filter(hasAccess -> hasAccess)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "서버에 대한 접근 권한이 없습니다.")))
                .then(findServer(serverId))
                .flatMap(server -> storeNewImage(file)
                        .flatMap(newImageUrl -> updateServerAndHandleOldImage(server, newImageUrl)))
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 이미지 업데이트 중 오류가 발생했습니다."));
    }

    private Mono<Server> findServer(Long serverId) {
        return serverRepository.findById(serverId)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다.")
                ));
    }

    private Mono<String> storeNewImage(FilePart file) {
        return fileStorageService.store(file)
                .onErrorMap(error ->
                        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장 중 오류가 발생했습니다: " + error.getMessage())
                );
    }

    private Mono<String> updateServerAndHandleOldImage(Server server, String newImageUrl) {
        String oldImageUrl = server.getServerImage();

        return Mono.just(server)
                .doOnNext(existingServer -> existingServer.setServerImage(newImageUrl))
                .flatMap(serverRepository::save)
                .map(Server::getServerImage)
                .doOnNext(__ -> {
                    if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                        deleteOldImage(oldImageUrl);
                    }
                });
    }

    private void deleteOldImage(String imageUrl) {
        fileStorageService.delete(imageUrl)
                .doOnError(error -> log.error("Failed to delete old server image: {}", imageUrl, error))
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이전 이미지 삭제 중 오류가 발생했습니다."))
                .subscribe();
    }

    public Mono<Void> deleteServer(Long serverId, String email) {
        return checkMemberAccess(serverId, email)
                .filter(hasAccess -> hasAccess)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "서버에 대한 접근 권한이 없습니다.")))
                .then(serverRepository.findById(serverId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다.")))
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
                )
                .onErrorMap(e -> !(e instanceof ResponseStatusException),
                        e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 삭제 중 오류가 발생했습니다."));
    }

    private Mono<Boolean> checkMemberAccess(Long serverId, String email) {
        return userServerMembershipRepository.findByServerIdAndEmail(serverId, email)
                .hasElement();
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