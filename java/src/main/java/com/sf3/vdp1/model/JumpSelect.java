package com.sf3.vdp1.model;

/**
 * Jump Mode.
 * @see "page 72"
 * */
public enum JumpSelect {
    JUMP_NEXT(0),
    JUMP_ASSIGN(1),
    JUMP_CALL(2),
    JUMP_RETURN(3),
    SKIP_NEXT(4),
    SKIP_ASSIGN(5),
    SKIP_CALL(6),
    SKIP_RETURN(7),
    ;

    private final int value;

    JumpSelect(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static JumpSelect of(int value) {
        return values()[value];
    }
}
