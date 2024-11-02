package com.example.pitching.call.exception;

public class InvalidValueException extends CommonException {

    public InvalidValueException(ErrorCode errorCode, String value) {
        super(errorCode, value);
    }
}
