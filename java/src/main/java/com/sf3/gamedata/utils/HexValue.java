package com.sf3.gamedata.utils;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sf3.gamedata.mpd.serializer.HexValueSerializer;

import java.util.Objects;

/** Wrapper around an integer to print these as hex. */
@JsonSerialize(using = HexValueSerializer.class)
public class HexValue implements Comparable<HexValue> {
    private final int value;

    public HexValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "0x"+Integer.toHexString(value);
    }

    @Override
    public int compareTo(HexValue o) {
        return value - o.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HexValue)) return false;
        HexValue hexValue = (HexValue) o;
        return value == hexValue.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
