package com.example.pitching.call.exception;

public class ForbiddenAccessException extends CommonException {
    public ForbiddenAccessException(ErrorCode errorCode, String value) {
        super(errorCode, value);
    }
}
