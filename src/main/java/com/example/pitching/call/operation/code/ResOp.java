package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ResOp {
    HELLO(0, "Sent immediately after connecting, contains the heartbeat_interval to use."),
    HEARTBEAT(1, "Fired periodically by the client to keep the connection alive."),
    HEARTBEAT_ACK(2, "Sent in response to receiving a heartbeat to acknowledge that it has been received."),
    ERROR(4, "Sent error message with cause"),
    ;

    @JsonValue
    private final int code;
    private final String description;

    ResOp(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ResOp from(int code) {
        for (ResOp type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
