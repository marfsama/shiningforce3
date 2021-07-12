package com.sf3.gamedata.utils;

import java.util.Arrays;
import java.util.stream.IntStream;

/** Class to write Bits. */
public class BitWriterStream {
    private byte[] content;
    private int mask = 0x80;
    private int bytePosition;

    public BitWriterStream() {
        this(10);
    }
    public BitWriterStream(int initialCapacity) {
        this.content = new byte[initialCapacity];
        this.bytePosition = 0;
    }

    public void writeBit(boolean b) {
        // increase buffer size
        if (bytePosition >= content.length) {
            int newCapacity = bytePosition * 2;
            content = Arrays.copyOf(content, newCapacity);
        }
        if (b) {
            this.content[bytePosition] |= mask;
        }
        mask >>= 1;
        if (mask == 0) {
            mask = 0x80;
            bytePosition++;
        }
    }

    public int getBytePosition() {
        return bytePosition;
    }

    public int getSize() {
        return bytePosition + (mask == 0x80 ? 0 : 1);
    }

    public byte[] getBuffer() {
        return content;
    }

    public byte[] copyToByteArrayExact() {
        return Arrays.copyOf(content, getSize());
    }

    public void skip(int numBits) {
        IntStream.range(0, numBits)
                .forEach(i -> writeBit(false));

    }
}
