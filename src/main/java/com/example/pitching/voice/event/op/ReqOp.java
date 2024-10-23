package com.example.pitching.voice.event.op;

import lombok.Getter;

@Getter
public enum ReqOp {

    HEARTBEAT(1, "Request/Response", "Fired periodically by the client to keep the connection alive."),
    IDENTIFY(2, "Request", "Starts a new session during the initial handshake."),
    PRESENCE_UPDATE(3, "Request", "Update the client's presence."),
    VOICE_STATE_UPDATE(4, "Request", "Used to join/leave or move between voice channels."),
    RESUME(6, "Request", "Resume a previous session that was disconnected."),
    REQUEST_GUILD_MEMBERS(8, "Request", "Request information about offline guild members in a large guild."),
    REQUEST_SOUNDBOARD_SOUNDS(31, "Request", "Request information about soundboard sounds in a set of guilds.");

    private final int code;
    private final String direction;
    private final String description;

    ReqOp(int code, String direction, String description) {
        this.code = code;
        this.direction = direction;
        this.description = description;
    }

    public static ReqOp from(int code) {
        for (ReqOp type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
