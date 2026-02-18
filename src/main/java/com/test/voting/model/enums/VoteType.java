package com.test.voting.model.enums;

import lombok.Getter;

public enum VoteType {
    YES(0),
    NO(1);

    @Getter
    private int code;

    VoteType(int code) {
        this.code = code;
    }
}
