package com.example.pitching.common.error;

import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 에러 응답")
public sealed interface ApiError permits
        ApiError.NotFound,
        ApiError.BadRequest,
        ApiError.ServerError,
        ApiError.PayloadTooLarge,
        ApiError.Unauthorized,
        ApiError.Conflict {

    @Schema(description = "리소스를 찾을 수 없는 경우의 에러")
    record NotFound(
            @Schema(description = "에러 메시지", example = "리소스를 찾을 수 없습니다")
            String message,
            @Schema(description = "HTTP 상태 코드", example = "404")
            HttpStatus status
    ) implements ApiError {
        public NotFound(String message) {
            this(message, HttpStatus.NOT_FOUND);
        }
    }

    @Schema(description = "잘못된 요청의 경우의 에러")
    record BadRequest(
            @Schema(description = "에러 메시지", example = "잘못된 요청 파라미터입니다")
            String message,
            @Schema(description = "HTTP 상태 코드", example = "400")
            HttpStatus status
    ) implements ApiError {
        public BadRequest(String message) {
            this(message, HttpStatus.BAD_REQUEST);
        }
    }

    @Schema(description = "서버 내부 오류")
    record ServerError(
            @Schema(description = "에러 메시지", example = "서버 내부 오류가 발생했습니다")
            String message,
            @Schema(description = "HTTP 상태 코드", example = "500")
            HttpStatus status
    ) implements ApiError {
        public ServerError(String message) {
            this(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Schema(description = "요청 페이로드가 너무 큰 경우의 에러")
    record PayloadTooLarge(
            @Schema(description = "에러 메시지", example = "파일 크기가 제한을 초과했습니다")
            String message,
            @Schema(description = "HTTP 상태 코드", example = "413")
            HttpStatus status
    ) implements ApiError {
        public PayloadTooLarge(String message) {
            this(message, HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    @Schema(description = "인증되지 않은 접근의 경우의 에러")
    record Unauthorized(
            @Schema(description = "에러 메시지", example = "인증되지 않은 접근입니다")
            String message,
            @Schema(description = "HTTP 상태 코드", example = "401")
            HttpStatus status
    ) implements ApiError {
        public Unauthorized(String message) {
            this(message, HttpStatus.UNAUTHORIZED);
        }
    }

    @Schema(description = "리소스 충돌이 발생한 경우의 에러")
    record Conflict(
            @Schema(description = "에러 메시지", example = "중복된 데이터가 존재합니다")
            String message,
            @Schema(description = "HTTP 상태 코드", example = "409")
            HttpStatus status
    ) implements ApiError {
        public Conflict(String message) {
            this(message, HttpStatus.CONFLICT);
        }
    }

    default HttpStatus getStatus() {
        return switch (this) {
            case NotFound n -> n.status;
            case BadRequest b -> b.status;
            case PayloadTooLarge p -> p.status;
            case Unauthorized u -> u.status;
            case Conflict c -> c.status;
            case ServerError s -> s.status;
        };
    }
}