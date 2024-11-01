package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ResponseOp {
    HELLO(0, "Sent immediately after connecting, contains the heartbeat_interval to use."),
    HEARTBEAT(1, "Fired periodically by the client to keep the connection alive."),
    HEARTBEAT_ACK(2, "Sent in response to receiving a heartbeat to acknowledge that it has been received."),
    SERVER_ACK(3, "Send current state of the server."),
    ENTER_SUCCESS(4, "Enter voice/video channel successful"),
    LEAVE_SUCCESS(5, "Leave voice/video channel successful"),
    ;

    @JsonValue
    private final int code;
    private final String description;

    ResponseOp(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ResponseOp from(int code) {
        for (ResponseOp type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
