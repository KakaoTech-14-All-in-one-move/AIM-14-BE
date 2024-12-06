package com.example.pitching.user.controller;

import com.example.pitching.common.error.ApiError;
import com.example.pitching.user.dto.UpdateUsernameRequest;
import com.example.pitching.user.dto.UserResponse;
import com.example.pitching.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "프로필 이미지 업데이트",
            description = "사용자의 프로필 이미지를 업데이트합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프로필 이미지 업데이트 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = Map.class,
                                            example = """
                        {
                            "profileImageUrl": "/uploads/uuid_20240206123456.jpg"
                        }
                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (이미지 파일이 아닌 경우)",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "413",
                            description = "파일 크기 초과",
                            content = @Content(schema = @Schema(implementation = ApiError.PayloadTooLarge.class))
                    )
            }
    )
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Map<String, String>> updateProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") @Parameter(
                    description = "업로드할 프로필 이미지 파일",
                    required = true,
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)
            ) Mono<FilePart> file) {
        return file
                .flatMap(filePart ->
                        userService.updateProfileImage(userDetails.getUsername(), filePart))
                .map(imageUrl -> Map.of("profileImageUrl", imageUrl));
    }

    @Operation(
            summary = "사용자 이름 업데이트",
            description = "사용자의 이름을 업데이트합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "사용자 이름 업데이트 성공",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (유효하지 않은 사용자 이름)",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    )
            }
    )
    @PutMapping("/username")
    @ResponseStatus(HttpStatus.OK)
    public Mono<UserResponse> updateUsername(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "업데이트할 사용자 이름 정보",
                    content = @Content(
                            schema = @Schema(implementation = UpdateUsernameRequest.class)
                    )
            ) UpdateUsernameRequest request) {
        return userService.updateUsername(userDetails.getUsername(), request.username());
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "사용자 계정을 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "회원 탈퇴 성공"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    )
            }
    )
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> withdrawUser(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        return userService.withdrawUser(userDetails.getUsername());
    }
}