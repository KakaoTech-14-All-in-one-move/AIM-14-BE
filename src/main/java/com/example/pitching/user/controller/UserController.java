package com.example.pitching.user.controller;

import com.example.pitching.user.dto.UpdateUsernameRequest;
import com.example.pitching.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    private final UserService userService;

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Map<String, String>>> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") Mono<FilePart> file) {

        return file
                .flatMap(filePart -> userService.updateProfileImage(userDetails.getUsername(), filePart))
                .map(imageUrl -> ResponseEntity.ok(Map.of("profileImageUrl", imageUrl)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", e.getMessage()))));
    }

    @PutMapping("/username")
    public Mono<ResponseEntity<Map<String, String>>> updateUsername(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUsernameRequest request) {

        return userService.updateUsername(userDetails.getUsername(), request.getUsername())
                .map(username -> ResponseEntity.ok(Map.of("username", username)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", e.getMessage()))));
    }
}
