package com.sf3.gamedata.battleterrain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sf3.gamedata.utils.HexValue;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

@Getter
public class ChunkItem {
    private final HexValue offset;
    private final HexValue dataSize;
    private final HexValue chunkSize;

    public  ChunkItem(ImageInputStream stream) throws IOException {
        this.offset = new HexValue(stream.readInt());
        this.dataSize = new HexValue(stream.readInt());
        this.chunkSize = new HexValue(stream.readInt());
        // pdding
        stream.readInt();
    }

    @SneakyThrows
    @Override
    public String toString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
