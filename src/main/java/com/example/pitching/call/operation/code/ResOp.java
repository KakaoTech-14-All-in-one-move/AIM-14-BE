package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ResOp {
    HELLO(0, "Response", "Sent immediately after connecting, contains the heartbeat_interval to use."),
    HEARTBEAT(1, "Request/Response", "Fired periodically by the client to keep the connection alive."),
    HEARTBEAT_ACK(2, "Response", "Sent in response to receiving a heartbeat to acknowledge that it has been received."),
    RESUMED(3, "Response", "Send all missed events and then send the last one"),
    ERROR(4, "Response", "Sent error message with cause"),
    RECONNECT(5, "Response", "You should attempt to reconnect and resume immediately."),
    ;

    @JsonValue
    private final int code;
    private final String direction;
    private final String description;

    ResOp(int code, String direction, String description) {
        this.code = code;
        this.direction = direction;
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
