package com.example.pitching.call.operation.response;

import com.example.pitching.call.exception.CommonException;
import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.operation.Data;

public record ErrorResponse(
        ErrorCode code,
        String message
) implements Data {
    public static ErrorResponse from(CommonException e) {
        return new ErrorResponse(e.getErrorCode(), e.getValue());
    }
}
