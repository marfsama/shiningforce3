package com.sf3.vdp1.model;

import com.sf3.util.Utils;
import com.sf3.util.ByteArrayImageInputStream;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Base Class for Chunks with subchunks. */
public class AbstractChunk {
    protected final Map<String, Chunk> chunks;

    public AbstractChunk(ImageInputStream stream, Chunk chunk) throws IOException {
        stream.seek(chunk.getPos() + 9*4);
        byte[] buffer = new byte[chunk.getLength()];
        stream.read(buffer);

        this.chunks = new LinkedHashMap<>();
        ImageInputStreamImpl chunkStream = new ByteArrayImageInputStream(buffer);
        chunkStream.setByteOrder(stream.getByteOrder());
        while (chunkStream.getStreamPosition() < buffer.length ) {
            int magicLen = chunkStream.read();
            String magic = Utils.readString(chunkStream, magicLen);
            int chunkLength = chunkStream.readInt();
            int pos = (int) (chunkStream.getStreamPosition() + chunk.getPos() + 9*4);
            byte[] chunkContent = new byte[chunkLength];
            chunkStream.readFully(chunkContent);
            chunks.put(magic, new Chunk(pos, magic, chunkContent));
        }
        chunkStream.close();
    }

    public Map<String, Chunk> getChunks() {
        return chunks;
    }
}
