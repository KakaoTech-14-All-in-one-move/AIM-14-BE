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
    record NotFound(@Schema(description = "에러 메시지", example = "리소스를 찾을 수 없습니다") String message) implements ApiError {}

    @Schema(description = "잘못된 요청의 경우의 에러")
    record BadRequest(@Schema(description = "에러 메시지", example = "잘못된 요청 파라미터입니다") String message) implements ApiError {}

    @Schema(description = "서버 내부 오류")
    record ServerError(@Schema(description = "에러 메시지", example = "서버 내부 오류가 발생했습니다") String message) implements ApiError {}

    @Schema(description = "요청 페이로드가 너무 큰 경우의 에러")
    record PayloadTooLarge(@Schema(description = "에러 메시지", example = "파일 크기가 제한을 초과했습니다") String message) implements ApiError {}

    @Schema(description = "인증되지 않은 접근의 경우의 에러")
    record Unauthorized(@Schema(description = "에러 메시지", example = "인증되지 않은 접근입니다") String message) implements ApiError {}

    @Schema(description = "리소스 충돌이 발생한 경우의 에러")
    record Conflict(@Schema(description = "에러 메시지", example = "중복된 데이터가 존재합니다") String message) implements ApiError {}

    default HttpStatus getStatus() {
        return switch (this) {
            case NotFound ignored -> HttpStatus.NOT_FOUND;
            case BadRequest ignored -> HttpStatus.BAD_REQUEST;
            case PayloadTooLarge ignored -> HttpStatus.PAYLOAD_TOO_LARGE;
            case Unauthorized ignored -> HttpStatus.UNAUTHORIZED;
            case Conflict ignored -> HttpStatus.CONFLICT;
            case ServerError ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}