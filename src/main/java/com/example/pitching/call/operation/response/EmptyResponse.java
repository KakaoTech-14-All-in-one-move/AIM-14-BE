package com.example.pitching.call.operation.response;

import com.example.pitching.call.operation.Data;

public record EmptyResponse() implements Data {
    public static EmptyResponse of() {
        return new EmptyResponse();
    }
}
