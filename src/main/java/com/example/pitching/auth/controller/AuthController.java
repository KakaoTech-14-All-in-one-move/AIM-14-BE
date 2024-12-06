package com.example.pitching.auth.controller;

import com.example.pitching.auth.dto.*;
import com.example.pitching.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "인증 관련 API")
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(example = "{\"status\":401,\"message\":\"이메일 또는 비밀번호가 올바르지 않습니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(example = "{\"status\":500,\"message\":\"로그인 처리 중 오류가 발생했습니다.\"}"))
            )
    })
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<LoginResponse> login(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "로그인 정보", required = true)
            LoginRequest request) {
        return authService.authenticate(request.email(), request.password());
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = TokenInfo.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰",
                    content = @Content(schema = @Schema(example = "{\"status\":401,\"message\":\"리프레시 토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해주세요.\"}"))
            )
    })
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public Mono<TokenInfo> refresh(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "리프레시 토큰", required = true)
            RefreshRequest request) {
        return authService.refreshToken(request.refreshToken());
    }

    @Operation(summary = "이메일 중복 확인", description = "회원가입 시 이메일 중복 여부를 확인합니다")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이메일 중복 확인 성공",
                    content = @Content(schema = @Schema(implementation = ExistsEmailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 이메일 형식",
                    content = @Content(schema = @Schema(example = "{\"status\":400,\"message\":\"올바른 이메일 형식이 아닙니다\"}"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(example = "{\"status\":500,\"message\":\"이메일 중복 확인 중 오류가 발생했습니다.\"}"))
            )
    })
    @GetMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ExistsEmailResponse> existsEmail(
            @Parameter(description = "확인할 이메일 주소", required = true, example = "user@example.com")
            @RequestParam("email") @Email String email) {
        return authService.existsEmail(email);
    }

    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 생성합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 요청 데이터",
                    content = @Content(schema = @Schema(example = "{\"status\":400,\"message\":\"비밀번호는 최소 8자 이상이어야 합니다\"}"))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 이메일",
                    content = @Content(schema = @Schema(example = "{\"status\":409,\"message\":\"이미 존재하는 이메일입니다\"}"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(example = "{\"status\":500,\"message\":\"회원가입 처리 중 오류가 발생했습니다\"}"))
            )
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> signup(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "회원가입 정보", required = true)
            SignupRequest request) {
        return authService.signup(request);
    }
}