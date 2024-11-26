package com.example.pitching.user.controller;

import com.example.pitching.user.dto.UpdateUsernameRequest;
import com.example.pitching.user.dto.UserResponse;
import com.example.pitching.user.service.FileValidator;
import com.example.pitching.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final FileValidator fileValidator;

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Map<String, String>> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") Mono<FilePart> file) {

        return file
                .flatMap(filePart ->
                        userService.updateProfileImage(userDetails.getUsername(), filePart))
                .map(imageUrl -> Map.of("profileImageUrl", imageUrl));
    }

    @PutMapping("/username")
    @ResponseStatus(HttpStatus.OK)
    public Mono<UserResponse> updateUsername(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUsernameRequest request) {
        return userService.updateUsername(userDetails.getUsername(), request.username());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> withdrawUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return userService.withdrawUser(userDetails.getUsername());
    }
}