package com.sf3.vdp1.model;

/**
 * Zoom point select.
 * @see "page 73"
 */
public enum ZoomPoint {
    SPECIFY_COORDS(0),
    UPPER_LEFT(0x5),
    UPPER_CENTER(0x6),
    UPPER_RIGHT(0x7),
    CENTER_LEFT(0x9),
    CENTER_CENTER(0xA),
    CENTER_RIGHT(0xB),
    LOWER_LEFT(0xD),
    LOWER_CENTER(0xE),
    LOWER_RIGHT(0xF),
    ;

    private final int value;

    ZoomPoint(int value) {
        this.value = value;
    }

    public static ZoomPoint of(int value) {
        for (ZoomPoint point : values()) {
            if (point.value == value)
                return point;
        }
        throw new IllegalArgumentException("prohibited zoom point: 0x"+Integer.toHexString(value));
    }
}
