package com.example.pitching.voice.operation;

import lombok.Getter;

@Getter
public enum DispatchEvent {
    READY(1),
    RESUMED(2);

    private final int seq;

    DispatchEvent(int seq) {
        this.seq = seq;
    }
}
