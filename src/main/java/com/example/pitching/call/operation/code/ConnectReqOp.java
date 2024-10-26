package com.example.pitching.call.operation.code;

import lombok.Getter;

@Getter
public enum ConnectReqOp {

    HEARTBEAT(1, "Request/Response", "Fired periodically by the client to keep the connection alive.");

    private final int code;
    private final String direction;
    private final String description;

    ConnectReqOp(int code, String direction, String description) {
        this.code = code;
        this.direction = direction;
        this.description = description;
    }

    public static ConnectReqOp from(int code) {
        for (ConnectReqOp type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
