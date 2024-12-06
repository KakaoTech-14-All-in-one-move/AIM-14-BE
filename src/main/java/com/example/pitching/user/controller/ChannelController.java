package com.example.pitching.user.controller;

import com.example.pitching.common.error.ApiError;
import com.example.pitching.user.dto.ChannelResponse;
import com.example.pitching.user.dto.CreateChannelRequest;
import com.example.pitching.user.dto.UpdateChannelNameRequest;
import com.example.pitching.user.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "Channel", description = "채널 관리 API")
@RestController
@RequestMapping("/api/v1/servers/{server_id}/channels")
@RequiredArgsConstructor
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class ChannelController {
    private final ChannelService channelService;

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "채널 생성",
            description = "새로운 채널을 생성합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "채널 생성 성공",
                            content = @Content(schema = @Schema(implementation = ChannelResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 파라미터",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "서버를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "중복된 채널 이름",
                            content = @Content(schema = @Schema(implementation = ApiError.Conflict.class))
                    )
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChannelResponse> createChannel(
            @Parameter(description = "서버 ID") @PathVariable(name = "server_id") Long serverId,
            @Valid @RequestBody CreateChannelRequest request
    ) {
        return channelService.createChannel(serverId, request)
                .map(ChannelResponse::from);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "채널 이름 수정",
            description = "채널의 이름을 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "채널 이름 수정 성공",
                            content = @Content(schema = @Schema(implementation = ChannelResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 채널 이름",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "채널을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "중복된 채널 이름",
                            content = @Content(schema = @Schema(implementation = ApiError.Conflict.class))
                    )
            }
    )
    @PutMapping("/{channel_id}/name")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ChannelResponse> updateChannelName(
            @Parameter(description = "서버 ID") @PathVariable(name = "server_id") Long serverId,
            @Parameter(description = "채널 ID") @PathVariable(name = "channel_id") Long channelId,
            @Valid @RequestBody UpdateChannelNameRequest request
    ) {
        return channelService.updateChannelName(channelId, request.channelName())
                .map(ChannelResponse::from);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "채널 삭제",
            description = "채널을 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "채널 삭제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "채널을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    )
            }
    )
    @DeleteMapping("/{channel_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteChannel(
            @Parameter(description = "서버 ID") @PathVariable(name = "server_id") Long serverId,
            @Parameter(description = "채널 ID") @PathVariable(name = "channel_id") Long channelId
    ) {
        return channelService.deleteChannel(channelId);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "채널 목록 조회",
            description = "서버에 속한 모든 채널 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "채널 목록 조회 성공",
                            content = @Content(schema = @Schema(implementation = ChannelResponse.class))
                    )
            }
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<ChannelResponse> getServerChannels(
            @Parameter(description = "서버 ID") @PathVariable(name = "server_id") Long serverId
    ) {
        return channelService.getChannelsByServerId(serverId)
                .map(ChannelResponse::from);
    }
}