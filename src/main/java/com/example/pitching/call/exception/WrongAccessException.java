package com.example.pitching.call.exception;

public class WrongAccessException extends CommonException {
    public WrongAccessException(ErrorCode errorCode, String value) {
        super(errorCode, value);
    }
}
