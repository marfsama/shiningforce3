package com.sf3.vdp1.model;

/**
 * Command Select.
 * @see "page 71"
 */
public enum CommandSelect {
    DRAW_SPRITE_NORMAL(0, true),
    DRAW_SPRITE_SCALED(1, true),
    DRAW_SPRITE_DISTORTED(2, true ),

    DRAW_POLYGON(4, false),
    DRAW_POLYLINE(5, false),
    DRAW_LINE(6, false),

    SET_USER_CLIPPING(8, false),
    SET_SYSTEM_CLIPPING(9, false),
    SET_LOCAL_COORDINATE(10, false),
;

    private final int value;
    private final boolean sprite;

    CommandSelect(int value, boolean sprite) {
        this.value = value;
        this.sprite = sprite;
    }

    public boolean isSprite() {
        return sprite;
    }

    public static CommandSelect of(int value) {
        for (CommandSelect commandSelect : values()) {
            if (commandSelect.value == value) {
                return commandSelect;
            }
        }
        throw  new IllegalArgumentException("prohibited command select: 0x"+ Integer.toHexString(value));
    }
}
