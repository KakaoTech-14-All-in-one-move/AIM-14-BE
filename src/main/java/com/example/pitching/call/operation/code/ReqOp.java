package com.example.pitching.call.operation.code;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ReqOp {

    INIT(0, "Send serverId to activate"),
    HEARTBEAT(1, "Fired periodically by the client to keep the connection alive."),
    ;

    @JsonValue
    private final int code;
    private final String description;

    ReqOp(int code, String description) {
        this.code = code;
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
