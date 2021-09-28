package com.sf3.gamedata.background;

import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.Utils;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CharacterImage {
    private static final String BASE_PATH = System.getProperty("user.home")+"/project/games/shiningforce3/build/target/disk/chr";

    public static void main(String[] args) {
        Block file = new CharacterImage().readFile(Paths.get(BASE_PATH, "XMINIMUM.CHR"));
        System.out.println(Utils.toPrettyFormat(file.toString()));
    }

    private Block readFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            Block file = new Block(path.getFileName().toString(), 0, (int) Files.size(path));
            ImageInputStream stream = new MemoryCacheImageInputStream(is);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            file.addProperty("filename", path.getFileName().toString());
            file.addProperty("size", new HexValue(file.getLength()));
            readHeader(file, stream);


            return file;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private void readHeader(Block file, ImageInputStream stream) throws IOException {
        Block header = file.createBlock("header", (int) stream.getStreamPosition(), 0);

        int i = 0;
        while (true) {
            int id = stream.readUnsignedShort();
            if (id == 0xffff)
                break;
            Block block = header.createBlock(String.format("item[%d]", i), (int) stream.getStreamPosition(), 0x18);
            block.addProperty("const0_id", new HexValue(id));
            block.addProperty("width", new HexValue(stream.readUnsignedShort()));
            block.addProperty("height", new HexValue(stream.readUnsignedShort()));
            block.addProperty("const1", new HexValue(stream.readUnsignedShort()));
            block.addProperty("const2", new HexValue(stream.readUnsignedShort()));
            block.addProperty("const3", new HexValue(stream.readUnsignedShort()));
            block.addProperty("offset1", new HexValue(stream.readInt()));
            block.addProperty("offset2", new HexValue(stream.readInt()));
            block.addProperty("offset3", new HexValue(stream.readInt()));
            i++;
        }
        header.addProperty("end", new HexValue((int) stream.getStreamPosition()));
        readStuffOffset2(stream, header);
        readStuffOffset3(stream, header);
    }

    private void readStuffOffset2(ImageInputStream stream, Block header) throws IOException {
        // read stuff @offset3
        for (Block item : header.valuesStream(Block.class).collect(Collectors.toList())) {
            int offset = item.getInt("offset2");
            stream.seek(offset);
            Block blockAtOffset2 = item.createBlock("offsets_at_offset2", (int) stream.getStreamPosition(), 0);
            List<Block> blocks = new ArrayList<>();
            while (true) {
                int value = stream.readInt();
                if (value == 0) {
                    break;
                }
                Block block = blockAtOffset2.createBlock("[" + new HexValue(value) + "]", value, 0);
                block.addProperty("offset", new HexValue(value));
                blocks.add(block);
            }
            for (Block block : blocks) {
                stream.seek(block.getStart());
                int size = stream.readInt();
                int otherOffset = size + block.getStart();
                block.addProperty("data_size", new HexValue(size));
                block.addProperty("other_offset", new HexValue(otherOffset));
                block.addProperty("foo1", new HexValue(stream.readInt()));
                block.addProperty("foo2", new HexValue(stream.readInt()));
                stream.seek(otherOffset);
                List<HexValue> values = new ArrayList<>();
                for (int i = 0; i < 24; i++) {
                    values.add(new HexValue(stream.readInt()));
                }
                block.addProperty("values", values);
                block.addProperty("end", new HexValue((int) stream.getStreamPosition()));
            }
            for (Block testBlock : blocks) {
                stream.seek(testBlock.getStart());
                int size = stream.readInt();
                System.out.println(stream.getStreamPosition() + " - " + size);
                byte[] compressed = new byte[size];
                stream.readFully(compressed);
                byte[] uncompressed = decompress(compressed);
                Files.write(Paths.get(BASE_PATH, "foo_" + new HexValue(testBlock.getStart()) + "_c.dat"), compressed);
                Files.write(Paths.get(BASE_PATH, "foo_" + new HexValue(testBlock.getStart()) + "_d.dat"), uncompressed);
            }
        }
    }

    private static final int BufferSize = 1 << 10;
    private static final int DictionarySize = 34;

    public static byte[] decompress(byte[] input) throws IOException {
        int THRESHOLD = 2;
        var text_buf = new byte[BufferSize];
        int inputIdx = 0;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int bufferIdx = BufferSize - DictionarySize; //r
        byte c = 0;
        int flags;

        for (int i = 0; i < BufferSize - DictionarySize; i++) {
            text_buf[i] = 0x0;
        }

        flags = 0;
        while (true)
        {
            flags >>= 1;
            if ((flags & 0x100) == 0)
            {
                if (inputIdx < input.length) {
                    c = input[inputIdx++];
                } else {
                    break;
                }
                flags = (c | 0xFF00);  /* uses higher byte cleverly */
            }   /* to count eight */
            if ((flags & 1) > 0)
            {
                if (inputIdx < input.length) {
                    c = input[inputIdx++];
                } else {
                    break;
                }
                output.write(((int) c) & 0xff);
                text_buf[bufferIdx++] = c;
                bufferIdx &= (BufferSize - 1);
            }
            else
            {
                int i = 0;
                int j = 0;
                if (inputIdx < input.length) {
                    i = input[inputIdx++];
                } else {
                    break;
                }
                if (inputIdx < input.length) {
                    j = input[inputIdx++];
                } else {
                    break;
                }
                i |= ((j & 0xE0) << 3);
                j = (j & 0x1F) + THRESHOLD;
                for (int k = 0; k <= j; k++)
                {
                    c = text_buf[(i + k) & (BufferSize - 1)];
                    output.write(((int) c) & 0xff);
                    text_buf[bufferIdx++] = c;
                    bufferIdx &= (BufferSize - 1);
                }
            }
        }

        return output.toByteArray();
    }



    private void readStuffOffset3(ImageInputStream stream, Block header) throws IOException {
        // read stuff @offset3
        List<Block> stuffAtOffset3Blocks = new ArrayList<>();
        for (Block item : header.valuesStream(Block.class).collect(Collectors.toList())) {
            int offset = item.getInt("offset3");
            stream.seek(offset);
            Block blockAtOffset3 = item.createBlock("_offsets_at_offset3", 0, 0);
            while (true) {
                int value = stream.readInt();
                if (value == 0) {
                    break;
                }
                Block block = blockAtOffset3.createBlock("[" + new HexValue(value) + "]", 0, 0);
                block.addProperty("offset", new HexValue(value));
                stuffAtOffset3Blocks.add(block);
            }
            // note: the values at offsets_at_offset3 are shorts which are terminated by 0x00f.?
            // seen: 0x00f2, 0x00fe, 0x00ff (followed by 0x0001)
        }
        // read the values at stuff_at_offset3_blocks
        for (Block block : stuffAtOffset3Blocks) {
            int offset = block.getInt("offset");
            stream.seek(offset);
            int j = 0;
            while (true) {
                int a = stream.readUnsignedShort();
                int b = stream.readUnsignedShort();
                Block foo = block.createBlock("item[" + j + "]", 0, 0);
                foo.addProperty("a", new HexValue(a)); // note: this is always a multiple of 4, might be 0
                foo.addProperty("b", new HexValue(b));
                if ((a & 0x80) > 0) {
                    break;
                }
                j++;
            }
        }
    }
}
