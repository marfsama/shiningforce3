package com.sf3.vdp1.model;

import com.sf3.util.Utils;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public class Chunk {
    private final int pos;
    private final String magic;
    private final int length;
    private final byte[] content;

    public Chunk(ImageInputStream stream) throws IOException {
        this.pos = (int) stream.getStreamPosition();
        this.magic = Utils.readString(stream, 8 * 4);
        this.length = stream.readInt();
        stream.skipBytes(length);
        this.content = null;
    }

    public Chunk(int pos, String magic, byte[] content) {
        this.pos = pos;
        this.magic = magic;
        this.length = content.length;
        this.content = content;
    }

    public int getPos() {
        return pos;
    }

    public String getMagic() {
        return magic;
    }

    public int getLength() {
        return length;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "magic='" + magic + '\'' +
                ", pos=0x" + Integer.toHexString(pos) +
                ", length=" + length +
                '}';
    }
}
