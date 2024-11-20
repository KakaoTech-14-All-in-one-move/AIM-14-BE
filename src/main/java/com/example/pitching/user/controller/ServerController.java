package com.example.pitching.user.controller;

import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.dto.ServerResponse;
import com.example.pitching.user.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {
    private final ServerService serverService;

    @PostMapping
    public Mono<ResponseEntity<ServerResponse>> createServer(
            @RequestBody ServerRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return serverService.createServer(request, user.getUsername())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @PutMapping("/{server_id}/name")
    public Mono<ResponseEntity<ServerResponse>> updateServerName(
            @PathVariable(name = "server_id") Long serverId,
            @RequestBody ServerRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return serverService.updateServerName(serverId, request.server_name(), user.getUsername())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{server_id}/image")
    public Mono<ResponseEntity<ServerResponse>> updateServerImage(
            @PathVariable(name = "server_id") Long serverId,
            @RequestBody ServerRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return serverService.updateServerImage(serverId, request.server_image(), user.getUsername())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<ServerResponse>>> getServers(
            @AuthenticationPrincipal UserDetails user) {
        return Mono.just(serverService.getUserServers(user.getUsername()))
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{server_id}")
    public Mono<ResponseEntity<Void>> deleteServer(
            @PathVariable(name = "server_id") Long serverId,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Deleting server: serverId={}, user={}", serverId, user.getUsername());
        return serverService.deleteServer(serverId, user.getUsername())
                .doOnSuccess(v -> log.info("Server deleted successfully: serverId={}", serverId))
                .doOnError(error -> log.error("Failed to delete server: serverId={}, error={}", serverId, error.getMessage()))
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(e -> {
                    log.error("Error processing delete request: serverId={}, error={}", serverId, e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().<Void>build());
                });
    }
}