package com.sf3.gamedata.mpd;

import com.sf3.util.ByteArrayImageInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * RLE encoded Stream of words.
 * The encoded/compressed data is split in 0x11 (17) words, one control word and 0x10 data words.
 * Each bit in the control word determines if the corresponding data word is data or a copy command.
 * The order of the bits in the control word is msb to lsb.
 * The control word 0x00 marks the end of the stream.
 */
public class DecompressedStream {
    /** decompressed data. */
    private final byte[] result;
    private final ByteOrder byteOrder;

    public DecompressedStream(ImageInputStream stream) throws IOException {
        this.result = decompressStream(stream);
        this.byteOrder = stream.getByteOrder();
    }

    public byte[] getResult() {
        return result;
    }

    public ImageInputStream toStream() {
        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(result);
        stream.setByteOrder(byteOrder);
        return stream;
    }

    private byte[] decompressStream(ImageInputStream stream) throws IOException {
        byte[] data = new byte[4096];
        int currentPos = 0;
        while (true) {
            int bitfield = stream.readUnsignedShort();

            for (int i = 0; i < 16; i++) {
                boolean commandFlag = (bitfield & 0x8000) != 0;
                if (commandFlag) {
                    int value = stream.readUnsignedShort();
                    if (value == 0) {
                        // end of Stream command
                        byte[] finalData = new byte[currentPos];
                        System.arraycopy(data, 0, finalData, 0, currentPos);
                        return finalData;
                    }

                    int negativeOffset = (value >> 5) * 2;
                    int count = ((value & 0x1f) + 2) * 2;

                    // stream copy
                    data = assertSize(data, currentPos+count);
                    for (int o = 0; o < count; o++) {
                        data[currentPos] = data[currentPos-negativeOffset];
                        currentPos++;
                    }
                }
                else {
                    // no command, copy word to output buffer
                    data = assertSize(data, currentPos+2);
                    data[currentPos++] = (byte) stream.readUnsignedByte();
                    data[currentPos++] = (byte) stream.readUnsignedByte();
                }
                bitfield <<= 1;
            }
        }

    }

    /** makes sure that the buffer is at least size elements big. */
    private static byte[] assertSize(byte[] buffer, int size) {
        if (size <= buffer.length) {
            return buffer;
        }
        byte[] newBuffer = new byte[size + size / 2];
        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
        return newBuffer;
    }


}
