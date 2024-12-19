package com.example.pitching.call.exception;

public class UnAuthorizedException extends CommonException {
    public UnAuthorizedException(ErrorCode errorCode, String value) {
        super(errorCode, value);
    }
}
