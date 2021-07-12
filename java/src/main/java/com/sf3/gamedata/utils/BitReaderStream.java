package com.sf3.gamedata.utils;

import java.util.Iterator;

/** Class to read the bits from a byte array. */
public class BitReaderStream implements Iterable<Boolean> {
    private final byte[] data;
    private final int length;
    private final int offset;

    private int bytePosition;
    private int mask;

    public BitReaderStream(BitReaderStream other) {
        this.data = other.data;
        this.length = other.length;
        this.offset = other.offset;

        this.bytePosition = other.bytePosition;
        this.mask = other.mask;

    }

    public BitReaderStream(byte[] data) {
        this(data,0, data.length);
    }

    public BitReaderStream(byte[] data, int offset) {
        this(data,offset, data.length - offset);
    }

    public BitReaderStream(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;

        this.bytePosition = 0;
        this.mask = 0x80;
    }

    public boolean nextBit() {
        if (!hasNext()) {
            throw new IndexOutOfBoundsException(bytePosition+" >= "+ data.length);
        }
        boolean bit = (data[offset + bytePosition] & mask) > 0;
        mask >>= 1;
        // processed each bit? of the byte?
        if (mask == 0) {
            // start with msb of next byte
            bytePosition++;
            mask = 0x80;
        }

        return bit;
    }

    private boolean hasNext() {
        return bytePosition < length;
    }

    @Override
    public Iterator<Boolean> iterator() {
        return new Iterator<Boolean>() {
            @Override
            public boolean hasNext() {
                return BitReaderStream.this.hasNext();
            }

            @Override
            public Boolean next() {
                return BitReaderStream.this.nextBit();
            }
        };
    }
}
