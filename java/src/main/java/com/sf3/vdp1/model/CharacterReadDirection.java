package com.sf3.vdp1.model;

/**
 * Character Read Direction. The read direction of the character can be specified.
 * This specification makes it possible to invert the character vertically and horizontally.
 * @see "page 77"
 */
public enum CharacterReadDirection {
    NOT_INVERTED(0),
    INVERTED_HORIZONTALLY(1),
    INVERTED_VERTICALLY(2),
    INVERTED_BOTH(3),
    ;

    private final int value;

    CharacterReadDirection(int value) {
        this.value = value;
    }

    public static CharacterReadDirection of(int value) {
        return values()[value];
    }
}
