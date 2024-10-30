package com.example.pitching.call.exception;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(1000),
    INVALID_SERVER_ID(1001),
    INVALID_REQUEST_OPERATION(1002),
    DUPLICATED_SERVER_DESTINATION(1011),
    DUPLICATED_CHANNEL_ENTRY(1012),
    DUPLICATED_CHANNEL_EXIT(1013),
    ;

    @JsonValue
    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }
}
