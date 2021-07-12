package com.sf3.vdp1.model;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public class VDP2 extends AbstractChunk {
    public VDP2(ImageInputStream stream, Chunk chunk) throws IOException {
        super(stream, chunk);
    }

    public ColorRam getColorRam() {
        Chunk colorRamChunk = getChunks().get("CRAM");
        Chunk cramType = getChunks().get("CRAM_Mode");
        return new ColorRam(cramType.getContent()[0], colorRamChunk.getContent());
    }
}
