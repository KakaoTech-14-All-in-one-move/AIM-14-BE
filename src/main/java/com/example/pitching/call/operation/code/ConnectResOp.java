package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ConnectResOp {
    HELLO(0, "Response", "Sent immediately after connecting, contains the heartbeat_interval to use."),
    HEARTBEAT(1, "Request/Response", "Fired periodically by the client to keep the connection alive."),
    HEARTBEAT_ACK(2, "Response", "Sent in response to receiving a heartbeat to acknowledge that it has been received."),
    RECONNECT(4, "Response", "You should attempt to reconnect and resume immediately."),
    ;

    @JsonValue
    private final int code;
    private final String direction;
    private final String description;

    ConnectResOp(int code, String direction, String description) {
        this.code = code;
        this.direction = direction;
        this.description = description;
    }

    public static ConnectResOp from(int code) {
        for (ConnectResOp type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
