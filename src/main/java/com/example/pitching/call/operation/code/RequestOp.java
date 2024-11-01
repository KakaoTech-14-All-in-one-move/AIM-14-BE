package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RequestOp {

    INIT(0, "Send serverId to activate"),
    HEARTBEAT(1, "Fired periodically by the client to keep the connection alive."),
    SERVER(2, "Send serverId when user changed server."),
    ENTER_VOICE(3, "Enter the voice channel."),
    ENTER_VIDEO(4, "Enter the video channel."),
    LEAVE_VOICE(5, "Leave the voice channel."),
    LEAVE_VIDEO(6, "Leave the video channel."),
    ;

    @JsonValue
    private final int code;
    private final String description;

    RequestOp(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RequestOp from(int code) {
        for (RequestOp type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
