package com.example.pitching.user.controller;

import com.example.pitching.user.dto.InviteMemberRequest;
import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.dto.ServerResponse;
import com.example.pitching.user.service.ServerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
@Validated
public class ServerController {
    private final ServerService serverService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ServerResponse> createServer(
            @Valid @RequestBody ServerRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return serverService.createServer(request, user.getUsername());
    }

    @PutMapping("/{server_id}/name")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ServerResponse> updateServerName(
            @PathVariable(name = "server_id") @Positive Long serverId,
            @Valid @RequestBody ServerRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return serverService.updateServerName(serverId, request.server_name(), user.getUsername());
    }

    @PostMapping(value = "/{server_id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Map<String, String>> updateServerImage(
            @PathVariable(name = "server_id") @Positive Long serverId,
            @RequestPart("file") Mono<FilePart> file,
            @AuthenticationPrincipal UserDetails user) {
        return file
                .flatMap(filePart -> serverService.updateServerImage(serverId, filePart, user.getUsername()))
                .map(imageUrl -> Map.of("serverImageUrl", imageUrl));
    }

    @PostMapping("/{server_id}/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> inviteMember(
            @PathVariable(name = "server_id") @Positive Long serverId,
            @Valid @RequestBody InviteMemberRequest request) {
        return serverService.inviteMember(serverId, request.email());
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<ServerResponse> getServers(
            @AuthenticationPrincipal UserDetails user) {
        return serverService.getUserServers(user.getUsername());
    }

    @DeleteMapping("/{server_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteServer(
            @PathVariable(name = "server_id") @Positive Long serverId,
            @AuthenticationPrincipal UserDetails user) {
        return serverService.deleteServer(serverId, user.getUsername());
    }
}