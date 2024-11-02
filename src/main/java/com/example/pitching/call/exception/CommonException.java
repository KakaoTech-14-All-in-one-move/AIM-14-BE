package com.example.pitching.call.exception;

import lombok.Getter;

@Getter
public class CommonException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String value;

    public CommonException(ErrorCode errorCode, String value) {
        super(value);
        this.errorCode = errorCode;
        this.value = value;
    }

    public CommonException(Throwable e) {
        super(e);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.value = e.getMessage();
    }
}
