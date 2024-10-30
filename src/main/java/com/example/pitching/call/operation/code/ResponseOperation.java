package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ResponseOperation implements Operation {
    ERROR(-1, "Send message when error occurs."),
    HELLO(0, "Sent immediately after connecting, contains the heartbeat_interval to use."),
    HEARTBEAT(1, "Fired periodically by the client to keep the connection alive."),
    HEARTBEAT_ACK(2, "Sent in response to receiving a heartbeat to acknowledge that it has been received."),
    SERVER_ACK(3, "Send current state of the server."),
    ENTER_CHANNEL_ACK(4, "Success to enter voice/video channel."),
    LEAVE_CHANNEL_ACK(5, "Success to leave voice/video channel.");

    @JsonValue
    private final int code;
    private final String description;

    ResponseOperation(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ResponseOperation from(int code) {
        for (ResponseOperation type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new RuntimeException("Invalid ResponseOperation code: " + code);
    }
}
