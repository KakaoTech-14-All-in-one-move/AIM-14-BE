package com.example.pitching.common.error;

import org.springframework.http.HttpStatus;

public sealed interface ApiError permits
        ApiError.NotFound,
        ApiError.BadRequest,
        ApiError.ServerError,
        ApiError.PayloadTooLarge {

    record NotFound(String message) implements ApiError {}
    record BadRequest(String message) implements ApiError {}
    record ServerError(String message) implements ApiError {}
    record PayloadTooLarge(String message) implements ApiError {}

    default HttpStatus getStatus() {
        return switch (this) {
            case NotFound ignored -> HttpStatus.NOT_FOUND;
            case BadRequest ignored -> HttpStatus.BAD_REQUEST;
            case PayloadTooLarge ignored -> HttpStatus.PAYLOAD_TOO_LARGE;
            case ServerError ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}