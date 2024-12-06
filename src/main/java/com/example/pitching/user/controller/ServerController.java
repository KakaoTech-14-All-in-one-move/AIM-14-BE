package com.example.pitching.user.controller;

import com.example.pitching.common.error.ApiError;
import com.example.pitching.user.dto.InviteMemberRequest;
import com.example.pitching.user.dto.ServerRequest;
import com.example.pitching.user.dto.ServerResponse;
import com.example.pitching.user.service.ServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Server", description = "서버 관리 API")
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
@Validated
public class ServerController {
    private final ServerService serverService;

    @Operation(
            summary = "서버 생성",
            description = "새로운 서버를 생성합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "서버 생성 성공",
                            content = @Content(schema = @Schema(implementation = ServerResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 파라미터",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 접근",
                            content = @Content(schema = @Schema(implementation = ApiError.Unauthorized.class))
                    )
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ServerResponse> createServer(
            @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "생성할 서버 정보",
                    content = @Content(schema = @Schema(implementation = ServerRequest.class))
            ) ServerRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user) {
        return serverService.createServer(request, user.getUsername());
    }

    @Operation(
            summary = "서버 이름 수정",
            description = "서버의 이름을 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "서버 이름 수정 성공",
                            content = @Content(schema = @Schema(implementation = ServerResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 서버 이름",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "접근 권한 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.Unauthorized.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "서버를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    )
            }
    )
    @PutMapping("/{server_id}/name")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ServerResponse> updateServerName(
            @Parameter(description = "서버 ID") @PathVariable(name = "server_id") @Positive Long serverId,
            @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "수정할 서버 이름",
                    content = @Content(schema = @Schema(implementation = ServerRequest.class))
            ) ServerRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user) {
        return serverService.updateServerName(serverId, request.server_name(), user.getUsername());
    }

    @Operation(
            summary = "서버 이미지 수정",
            description = """
                    서버의 이미지를 수정합니다.
                    이미지 파일 형식만 업로드 가능하며, 파일 크기에 제한이 있습니다.
                    기존 이미지가 있는 경우 자동으로 삭제되며, 새로운 이미지로 대체됩니다.
                    업로드된 파일은 UUID와 타임스탬프가 포함된 고유한 파일명으로 저장됩니다.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "서버 이미지 수정 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = Map.class,
                                            example = """
                                                    {
                                                        "serverImageUrl": "/uploads/550e8400-e29b-41d4-a716-446655440000_20240206123456.jpg"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (이미지 파일이 아닌 경우, 파일 확장자가 없는 경우)",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "접근 권한 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.Unauthorized.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "서버를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    ),
                    @ApiResponse(
                            responseCode = "413",
                            description = "파일 크기가 제한을 초과함(2MB 제한)",
                            content = @Content(schema = @Schema(implementation = ApiError.PayloadTooLarge.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류 (파일 저장/삭제 실패, 업로드 디렉토리 생성 실패 등)",
                            content = @Content(schema = @Schema(implementation = ApiError.ServerError.class))
                    )
            }
    )
    @PostMapping(value = "/{server_id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<Map<String, String>> updateServerImage(
            @Parameter(description = "서버 ID") @PathVariable(name = "server_id") @Positive Long serverId,
            @RequestPart("file") @Parameter(
                    description = "업로드할 서버 이미지 파일 (이미지 파일만 허용)",
                    required = true,
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)
            ) Mono<FilePart> file,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user) {
        return file.flatMap(filePart -> serverService.updateServerImage(serverId, filePart, user.getUsername()))
                .map(imageUrl -> Map.of("serverImageUrl", imageUrl));
    }

    @Operation(
            summary = "서버 멤버 초대",
            description = "새로운 멤버를 서버에 초대합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "멤버 초대 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 이메일",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "서버 또는 사용자를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "이미 초대된 사용자",
                            content = @Content(schema = @Schema(implementation = ApiError.Conflict.class))
                    )
            }
    )
    @PostMapping("/{server_id}/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> inviteMember(
            @Parameter(description = "서버 ID") @PathVariable(name = "server_id") @Positive Long serverId,
            @Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "초대할 사용자 정보",
                    content = @Content(schema = @Schema(implementation = InviteMemberRequest.class))
            ) InviteMemberRequest request) {
        return serverService.inviteMember(serverId, request.email());
    }

    @Operation(
            summary = "서버 목록 조회",
            description = "사용자가 속한 모든 서버 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "서버 목록 조회 성공",
                            content = @Content(schema = @Schema(implementation = ServerResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 접근",
                            content = @Content(schema = @Schema(implementation = ApiError.Unauthorized.class))
                    )
            }
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<ServerResponse> getServers(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user) {
        return serverService.getUserServers(user.getUsername());
    }

    @Operation(
            summary = "서버 삭제",
            description = """
                    서버를 삭제합니다.
                    서버와 관련된 모든 데이터(멤버십, 이미지 등)가 함께 삭제됩니다.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "서버 삭제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "접근 권한 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.Unauthorized.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "서버를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류 (이미지 파일 삭제 실패 등)",
                            content = @Content(schema = @Schema(implementation = ApiError.ServerError.class))
                    )
            }
    )
    @DeleteMapping("/{server_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteServer(
            @Parameter(description = "서버 ID") @PathVariable(name = "server_id") @Positive Long serverId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails user) {
        return serverService.deleteServer(serverId, user.getUsername());
    }
}