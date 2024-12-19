package com.example.pitching.call.exception;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(1000),

    UNAUTHORIZED_ACCESS_TOKEN(1001),
    UNAUTHORIZED_USER(1002),

    INVALID_SERVER_ID(1011),
    INVALID_CHANNEL_ID(1012),
    INVALID_REQUEST_OPERATION(1013),

    DUPLICATE_SERVER_DESTINATION(1021),
    DUPLICATE_CHANNEL_ENTRY(1022),
    DUPLICATE_CHANNEL_EXIT(1023),

    WRONG_ACCESS_INACTIVE_USER(1031),
    WRONG_ACCESS_INACTIVE_SERVER(1032),
    WRONG_ACCESS_INACTIVE_CHANNEL(1033),
    ;

    @JsonValue
    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }
}
