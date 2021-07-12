package com.sf3.vdp1.model;

/**
 * Color Mode.
 * @see "page 89"
 */
public enum ColorMode {
    COLOR_BANK_4_BPP_16_COLORS(0, 4),
    LOOKUP_TABLE_4_BPP_16_COLORS(1, 4),
    COLOR_BANK_8_BPP_64_COLORS(2, 2),
    COLOR_BANK_8_BPP_128_COLORS(3, 2),
    COLOR_BANK_8_BPP_256_COLORS(4, 2),
    RGB_16_BPP(5, 1),
    unknown_6(6, 1),
    ;

    private final int value;
    private final int mask;
    private final int bitsPerColor;
    /** number of pixels per word (16 bit). */
    private final int numColors;

    ColorMode(int value, int numColors) {
        this.value = value;
        this.numColors = numColors;
        this.bitsPerColor = 16 / numColors;
        this.mask = ((int) Math.pow(2, bitsPerColor)) - 1;
    }
    public int getMask() {
        return mask;
    }

    public int getBitsPerColor() {
        return bitsPerColor;
    }

    public int getNumColors() {
        return numColors;
    }

    public static ColorMode of(int value) {
        return values()[value];
    }
}
