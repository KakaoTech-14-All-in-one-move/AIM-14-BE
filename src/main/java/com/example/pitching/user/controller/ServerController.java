package com.example.pitching.user.controller;

import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.dto.ServerResponse;
import com.example.pitching.user.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

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
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body(null)));
    }

    @PutMapping("/{server_id}/name")
    public Mono<ResponseEntity<ServerResponse>> updateServerName(
            @PathVariable(name = "server_id") Long serverId,
            @RequestBody ServerRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return serverService.updateServerName(serverId, request.server_name(), user.getUsername())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body(null)));
    }

    @PostMapping(value = "/{server_id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Map<String, String>>> updateServerImage(
            @PathVariable(name = "server_id") Long serverId,
            @RequestPart("file") Mono<FilePart> file) {
        return file
                .flatMap(filePart -> serverService.updateServerImage(serverId, filePart))
                .map(imageUrl -> ResponseEntity.ok(Map.of("serverImageUrl", imageUrl)))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()))));
    }

    @PostMapping("/{server_id}/invite")
    public Mono<ResponseEntity<Map<String, String>>> inviteMember(
            @PathVariable(name = "server_id") Long serverId,
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        return serverService.inviteMember(serverId, email)
                .map(result -> ResponseEntity.ok(Map.of("message", "Successfully invited member")))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()))));
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<ServerResponse>>> getServers(
            @AuthenticationPrincipal UserDetails user) {
        return Mono.just(serverService.getUserServers(user.getUsername()))
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body(Flux.empty())));
    }

    @DeleteMapping("/{server_id}")
    public Mono<ResponseEntity<Void>> deleteServer(
            @PathVariable(name = "server_id") Long serverId,
            @AuthenticationPrincipal UserDetails user) {
        return serverService.deleteServer(serverId, user.getUsername())
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .build()));
    }
}