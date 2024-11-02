package com.example.pitching.call.exception;

public class DuplicateOperationException extends CommonException {
    public DuplicateOperationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
