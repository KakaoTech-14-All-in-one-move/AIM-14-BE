package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ResponseOperation implements Operation {
    ERROR(-1, "Send message when error occurs."),
    INIT_ACK(10, "Sent immediately after connecting, contains the heartbeat_interval to use."),
    //    HEARTBEAT(11, "Fired periodically by the client to keep the connection alive."),
    HEARTBEAT_ACK(11, "Sent in response to receiving a heartbeat to acknowledge that it has been received."),
    SERVER_ACK(12, "Send current state of the server."),
    ENTER_CHANNEL_EVENT(13, "Success to enter voice/video channel."),
    LEAVE_CHANNEL_EVENT(14, "Success to leave voice/video channel."),
    UPDATE_STATE_EVENT(15, "Success to update state."),
    ICE_CANDIDATE(16, "Received ICE candidate from the remote peer for WebRTC connection"),
    RECEIVE_VIDEO_ANSWER(17, "Response to video offer containing session description for WebRTC"),
    CANCEL_VIDEO_ANSWER(18, ""),
    ;
    ;

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
