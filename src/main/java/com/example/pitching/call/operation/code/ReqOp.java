package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ReqOp {

    HEARTBEAT(1, "Request/Response", "Fired periodically by the client to keep the connection alive."),
    RESUME(2, "Request", "Request events after the last sequence.");

    @JsonValue
    private final int code;
    private final String direction;
    private final String description;

    ReqOp(int code, String direction, String description) {
        this.code = code;
        this.direction = direction;
        this.description = description;
    }

    public static ReqOp from(int code) {
        for (ReqOp type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
