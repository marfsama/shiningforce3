package com.sf3.vdp1.model;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/** saved state for VDP1. */
public class VDP1 extends AbstractChunk {

    public VDP1(ImageInputStream stream, Chunk vpd1Chunk) throws IOException {
        super(stream, vpd1Chunk);
    }

    public Chunk getVram() {
        return getChunks().get("VRAM");
    }

    @Override
    public String toString() {
        return "VDP1{" +
                "chunks=" + getChunks() +
                '}';
    }
}
