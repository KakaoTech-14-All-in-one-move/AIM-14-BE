package com.example.pitching.chat.controller;

import com.example.pitching.chat.dto.ChatMessageDTO;
import com.example.pitching.chat.service.ChatService;
import com.example.pitching.common.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "Chat", description = "채팅 메시지 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/ws/v1/channels")
@Slf4j
public class ChatController {
    private final ChatService chatService;

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "채널 메시지 조회",
            description = "특정 채널의 모든 메시지를 조회합니다. 선택적으로 타임스탬프 이후의 메시지만 조회할 수 있습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "메시지 조회 성공",
                            content = @Content(schema = @Schema(implementation = ChatMessageDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 파라미터",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "채널을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.ServerError.class))
                    )
            }
    )
    @GetMapping("/{channel_id}/messages")
    public Flux<ChatMessageDTO> getChannelMessages(
            @Parameter(description = "채널 ID", required = true)
            @PathVariable(name = "channel_id") Long channelId,
            @Parameter(description = "조회할 메시지의 시작 타임스탬프", required = false)
            @RequestParam(name = "timestamp", required = false) Long timestamp
    ) {
        if (channelId == null || channelId <= 0) {
            return Flux.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid channel ID"
            ));
        }

        log.info("Getting messages for channel: {}, timestamp: {}", channelId, timestamp);

        try {
            return chatService.getChannelMessages(channelId)
                    .doOnSubscribe(subscription ->
                            log.info("Starting to fetch messages for channel: {}", channelId))
                    .doOnComplete(() ->
                            log.info("Completed fetching messages for channel: {}", channelId))
                    .doOnError(e ->
                            log.error("Error fetching messages for channel {}: {}", channelId, e.getMessage()))
                    .onErrorResume(e -> Flux.error(new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Error fetching messages: " + e.getMessage()
                    )));
        } catch (Exception e) {
            log.error("Unexpected error in getChannelMessages", e);
            return Flux.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error"
            ));
        }
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "채널 메시지 삭제",
            description = "특정 채널의 모든 메시지를 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "메시지 삭제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 파라미터",
                            content = @Content(schema = @Schema(implementation = ApiError.BadRequest.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "채널을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.NotFound.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.ServerError.class))
                    )
            }
    )
    @DeleteMapping("/{channel_id}/messages")
    public Mono<ResponseEntity<Void>> deleteChannelMessages(
            @Parameter(description = "채널 ID", required = true)
            @PathVariable(name = "channel_id") Long channelId
    ) {
        if (channelId == null || channelId <= 0) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid channel ID"
            ));
        }

        return chatService.deleteChannelMessages(channelId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(v -> log.info("Successfully deleted messages for channel: {}", channelId))
                .doOnError(e -> log.error("Error deleting messages for channel {}: {}", channelId, e.getMessage()));
    }
}
