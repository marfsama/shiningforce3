package com.sf3.gamedata.chp;

import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.Utils;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/** Read battle sprite files. */
public class ChpRead {

    public static void main(String[] args) {
        String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/chp - battle sprite";
        String file = "cbf14.chp";
        Path path = Paths.get(basePath, file);

        Block chpFile = readFile(path);
        System.out.println(Utils.toPrettyFormat(chpFile.toString()));
    }

    private static Block readFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            Block file = new Block(path.getFileName().toString(), 0, (int) Files.size(path));
            ImageInputStream stream = new MemoryCacheImageInputStream(is);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            file.addProperty("size", new HexValue(file.getLength()));
            file.addBlock(readHeader(stream, 0, 0));
            file.addBlock(readHeader(stream, 1, 0x3000));
            file.addBlock(readHeader(stream, 2, 0x3800));
            file.addBlock(readHeader(stream, 3, 0x6800));

            return file;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Block readHeader(ImageInputStream stream, int num, int offset) throws IOException {
        stream.seek(offset);

        Block header = new Block("header"+num, 0, 0x800);
        header.addProperty("start", new HexValue(offset));

        for (int i = 0; i < (0x88 / 4); i++) {
            header.addProperty("["+i+"]", new HexValue(stream.readInt()));
        }

        stream.seek(offset+header.getInt("[4]"));
        for (int i = 0; i < 17; i++) {
            // note: add 4 to these offsets.
            header.addProperty("offset["+i+"]", new HexValue(stream.readInt()));
        }

        return header;
    }
}
