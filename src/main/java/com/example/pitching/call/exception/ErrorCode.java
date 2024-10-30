package com.example.pitching.call.exception;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(1000),

    INVALID_SERVER_ID(1001),
    INVALID_CHANNEL_ID(1002),
    INVALID_REQUEST_OPERATION(1003),

    DUPLICATE_SERVER_DESTINATION(1011),
    DUPLICATE_CHANNEL_ENTRY(1012),
    DUPLICATE_CHANNEL_EXIT(1013),

    FORBIDDEN_ACCESS_NOT_ACTIVE_SERVER(1021),
    ;

    @JsonValue
    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }
}
