package com.example.pitching.gateway.event.op;

import lombok.Getter;

@Getter
public enum ResOp {
    DISPATCH(0, "Response", "An event was dispatched."),
    HEARTBEAT(1, "Request/Response", "Fired periodically by the client to keep the connection alive."),
    RECONNECT(7, "Response", "You should attempt to reconnect and resume immediately."),
    INVALID_SESSION(9, "Response", "The session has been invalidated. You should reconnect and identify/resume accordingly."),
    HELLO(10, "Response", "Sent immediately after connecting, contains the heartbeat_interval to use."),
    HEARTBEAT_ACK(11, "Response", "Sent in response to receiving a heartbeat to acknowledge that it has been received.");

    private final int code;
    private final String direction;
    private final String description;

    ResOp(int code, String direction, String description) {
        this.code = code;
        this.direction = direction;
        this.description = description;
    }
}
