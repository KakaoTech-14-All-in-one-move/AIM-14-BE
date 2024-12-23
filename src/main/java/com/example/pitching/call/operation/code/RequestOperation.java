package com.example.pitching.call.operation.code;

import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.InvalidValueException;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RequestOperation implements Operation {

    INIT(0, "Send serverId to activate"),
    HEARTBEAT(1, "Fired periodically by the client to keep the connection alive."),
    SERVER(2, "Send serverId when user changed server."),
    ENTER_CHANNEL(3, "Enter the voice/video channel."),
    LEAVE_CHANNEL(4, "Leave the voice/video channel."),
    UPDATE_STATE(5, "Change the voice/video channel state"),
    ON_ICE_CANDIDATE(6, "Send ICE candidate for WebRTC peer connection setup"),
    RECEIVE_VIDEO(7, "Receive video stream from remote peer"),
    CANCEL_VIDEO(8, ""),
    ;

    @JsonValue
    private final int code;
    private final String description;

    RequestOperation(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RequestOperation from(int code) {
        for (RequestOperation type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new InvalidValueException(ErrorCode.INVALID_REQUEST_OPERATION, String.valueOf(code));
    }
}
