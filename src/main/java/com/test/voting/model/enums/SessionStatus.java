package com.test.voting.model.enums;

import lombok.Getter;

public enum SessionStatus {
    OPEN(1),
    CLOSE(2);

    @Getter
    private int code;

    SessionStatus(int code) {
        this.code = code;
    }
}
