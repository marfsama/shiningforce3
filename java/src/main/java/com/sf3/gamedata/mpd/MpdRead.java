package com.sf3.gamedata.mpd;

import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.sgl.Point;
import com.sf3.gamedata.sgl.Polygon;
import com.sf3.gamedata.sgl.*;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.util.Sf3Util;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Known stuff for map data. */
public abstract class MpdRead {

    public static final Map<String, Integer> OFFSETS = new HashMap<>()
    {{
        put("bochi.mpd", 0x60a0000);
        put("btl019.mpd", 0x60a0000);
        put("bochim.mpd", 0x60a0000);
        put("tori00.mpd", 0x60a0000);
        put("nasu00.mpd", 0x60a0000);
        put("yakata.mpd", 0x60a0000);
        put("tesmap.mpd", 0x60a0000);
        put("sara02.mpd", 0x0292100);

        put("btl02.mpd", 0x60a0000);
        put("btl03.mpd", 0x60a0000);
        put("btl04a.mpd", 0x60a0000);
        put("btl06.mpd", 0x60a0000);
        put("btl12.mpd", 0x60a0000);
        put("btl17.mpd", 0x60a0000);
        put("btl21.mpd", 0x60a0000);
        put("btl24.mpd", 0x60a0000);

        put("chou00.mpd", 0x60a0000);
        put("fed06.mpd", 0x60a0000);
        put("field.mpd", 0x60a0000);
        put("furain.mpd", 0x60a0000);
        put("gdi5.mpd", 0x60a0000);
        put("gdi.mpd", 0x60a0000);
        put("zlv1.mpd", 0x60a0000);
        put("zlv2.mpd", 0x60a0000);
        put("zlv3.mpd", 0x60a0000);
        put("zlv4.mpd", 0x60a0000);
        put("yaka2.mpd", 0x60a0000);
        put("yaka3.mpd", 0x60a0000);
        put("yaka4.mpd", 0x60a0000);
        put("point.mpd", 0x60a0000);
        put("hrnaka.mpd", 0x60a0000);
        put("hrrail.mpd", 0x60a0000);
        put("inka00.mpd", 0x60a0000);
//        put("jousai.mpd", 0x60a0000);
        put("mgma00.mpd", 0x60a0000);
        put("mgma01.mpd", 0x60a0000);
        put("muhasi.mpd", 0x60a0000);
        put("sara05.mpd", 0x60a0000);
        put("shief1.mpd", 0x60a0000);
        put("shief2.mpd", 0x60a0000);
        put("shief3.mpd", 0x60a0000);
        put("shief4.mpd", 0x60a0000);
        put("shief5.mpd", 0x60a0000);
        put("shio00.mpd", 0x60a0000);
        put("tnka00.mpd", 0x60a0000);
        put("tomt00.mpd", 0x60a0000);
        put("toue00.mpd", 0x60a0000);
        put("tree00.mpd", 0x60a0000);
        put("turi00.mpd", 0x60a0000);
        put("turi01.mpd", 0x60a0000);
        put("zoku00.mpd", 0x60a0000);

        // broken beyond repair
        put("ship2.mpd" , 0x250000+0x2100);
    }};
    public static final int DEFAULT_OFFSET = 0x292100;
    public static final int MAP_MEMORY_OFFSET = 0x290000;
    private static final Integer EMPTY_SURFACE_TILE = 0xff;

    public Block readFile(Path mpdFile, Path outPath) {
        try (ImageInputStream stream = new FileImageInputStream(mpdFile.toFile())) {
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            Files.createDirectories(outPath);
            String filename = mpdFile.getFileName().toString();
            Block file = new Block(filename, 0, (int) Files.size(mpdFile));
            file.addProperty("size", new HexValue(file.getLength()));
            file.addProperty("filename", mpdFile.toAbsolutePath().toString());
            HighlightGroups highlightGroups = new HighlightGroups();
            file.addProperty("highlights", highlightGroups);


            readHeader(highlightGroups, file, stream);
            readChunkDirectory(highlightGroups, file, stream);
            readMapObjects(highlightGroups, file, stream, filename);
            readSurfaceTiles(highlightGroups, stream, outPath, file, filename +".surface_tiles");
            // read texture animations, updating the animation list in the header.
            readTextureAnimationImages(highlightGroups, stream, file, filename +".textureaminations");

            readSurfaceBlock2(highlightGroups, stream, file, outPath,filename +".surface2");

            readTextureChunks(highlightGroups, mpdFile, outPath, file, stream);

            // I think these are scroll screens. Some are battle background images (skybox). some seems
            // to be ground textures like dirt or water.
            // Scroll screen can be a big image (bit map format) or a character indexed (cell format) pattern
            // page 65
            dumpScrollPaneChunks(highlightGroups, mpdFile, outPath, file, stream);


            // write Surface Image with correct textures
            writeSurfaceImage(file.getBlock("surface_tiles"), file.getObject("textures"), outPath,filename+".surface_tiles");
            writeTextureImage(file, outPath, filename+".textures");

            return file;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeTextureImage(Block file, Path outPath, String filename) {
        List<BufferedImage> textureImages = new ArrayList<>();

        List<Texture> textures = file.getBlock("texture_chunks")
                .valuesStream(Block.class)
                .filter(textureChunk -> textureChunk.hasProperty("textures"))
                .map(textureChunk -> textureChunk.<List<Texture>>getObject("textures"))
                .flatMap(List::stream)
//                .sorted(Comparator.comparingInt(Texture::getWidth))
                .collect(Collectors.toList());


        textures.forEach(texture -> {
            texture.setTextureImageIndex(textureImages.size());
            textureImages.add(texture.getImage());
        });

        List<TextureAnimation> textureAnimations = file.getBlock("header").getObject("texture_animations");
        textureAnimations.stream().map(TextureAnimation::getFrames)
                .flatMap(List::stream)
                .forEach(frame -> {
                    frame.textureImageIndex = textureImages.size();
                    textureImages.add(frame.image);
                });

        List<TextureUv> uvMap = Sf3Util.writeTextureImage(textureImages, new HashSet<>(), outPath.resolve(filename + ".png").toAbsolutePath().toString(), 0, false);
        Block uvBlock = file.createBlock("uvs", 0, 0);
        uvBlock.addProperty("filename", filename+".png");
        uvBlock.addProperty("uv_map",uvMap);
    }

    private Map<Integer, Texture> readTextureChunks(HighlightGroups highlightGroups, Path path, Path destinationPath, Block file, ImageInputStream stream) throws IOException {
        List<Texture> textures = new ArrayList<>();
        Block chunkDirectory = file.getBlock("chunk_directory");
        Block textureChunks = file.createBlock("texture_chunks", 0, 0);
        int i = -1;
        for (Block blockDescription : chunkDirectory.valuesStream(Block.class).filter(b -> b.getName().startsWith("textures")).collect(Collectors.toList())) {
            i++;
            int offset = blockDescription.getInt("relativeOffset");
            int size = blockDescription.getInt("size");
            highlightGroups.addRange("textures", offset, size);
            if (size == 0) {
                textureChunks.addProperty(blockDescription.getName(), "chunk size is 0, skipped");
                // note: normally this means there are no more textures, so the continue could also be a break
                continue;
            }

            decompressChunk(stream, offset, size, null);

            try {
                Block block = readTextures(stream, chunkDirectory, "textures[" + i + "]");
                textures.addAll(block.<List<Texture>>getObject("textures"));
                textureChunks.addBlock(block);
            } catch (Exception e) {
                textureChunks.addProperty("textures[" + i + "]", "reading failed: "+e.getMessage());
            }
        }
        // read objects textures
        i = 0;
        // object texture ids start at 0x102
        int startObjectTextureId = 0x102;
        for (Block blockDescription : chunkDirectory.valuesStream(Block.class).filter(b -> b.getName().startsWith("object_textures")).collect(Collectors.toList())) {
            int offset = blockDescription.getInt("relativeOffset");
            int size = blockDescription.getInt("size");
            highlightGroups.addRange("object_textures", offset, size);
            if (size == 0) {
                break;
            }

            decompressChunk(stream, offset, size, null);

            try {
                Block block = readTextures(stream, chunkDirectory, "object_textures[" + i + "]");
                List<Texture> chunkTextures = block.<List<Texture>>getObject("textures");
                // increase textureId by startObjectTextureId so these textures won't override existing textures
                int c = startObjectTextureId;
                chunkTextures.forEach(t -> t.setTextureId(t.getTextureId()+c));
                startObjectTextureId += chunkTextures.size();
                textures.addAll(chunkTextures);
                textureChunks.addBlock(block);
            } catch (Exception e) {
                textureChunks.addProperty("objecttextures[" + i + "]", "reading failed: "+e.getMessage());
            }
            i++;
        }
        // map by textureId
        Map<Integer, Texture> textureMap = textures.stream().collect(
                Collectors.toMap(Texture::getTextureId, texture -> texture, (u,v) -> v, TreeMap::new));
        file.addProperty("textures", textureMap);
        return textureMap;
    }

    protected byte[] decompressChunk(ImageInputStream stream, int offset, int size, Path baseFilename) throws IOException {
        byte[] buffer = new byte[size];
        stream.seek(offset);
        stream.readFully(buffer);
        if (baseFilename != null) {
            Path rawFilename = baseFilename.getParent().resolve(baseFilename.getFileName().toString() + ".raw");
            Files.write(rawFilename, buffer);
        }
        // try to decompress the chunk
        try {
            ImageInputStream byteStream = new ByteArrayImageInputStream(buffer);
            byteStream.setByteOrder(stream.getByteOrder());
            DecompressedStream decompressedStream = new DecompressedStream(byteStream);
            byte[] decompressesBytes = decompressedStream.getResult();
            if (baseFilename != null) {
                // byteStream should be exhausted when the entire chunk was compressed.
                // otherwise the compression worked "by accident" and the chunk was not really compressed.
                // note: leave some slack for padding bytes.
                if (byteStream.getStreamPosition() >= (buffer.length - 16)) {
                    Path decompressedFilename = baseFilename.getParent().resolve(baseFilename.getFileName().toString() + ".decompressed");
                    Files.write(decompressedFilename, decompressesBytes);
                }
            }
            return decompressesBytes;
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return null;
    }

    private void writeSurfaceImage(Block surface_tiles, Map<Integer, Texture> textureMap, Path destinationPath, String filename) throws IOException {
        if (!surface_tiles.hasProperty("surface")) {
            // no surface tiles
            return;
        }

        BufferedImage image = new BufferedImage(16*4*16, 16*4*16, BufferedImage.TYPE_INT_RGB);
        Surface surface = surface_tiles.getObject("surface");
        Map<Integer, BufferedImage> textureMapFlipped = textureMap.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey(),
                e -> flipVertically(e.getValue().getImage())
        ));
        Graphics2D g2d = image.createGraphics();
        for (int x = 0; x < 16*4; x++) {
            for (int y = 0; y < 16*4; y++) {
                Integer textureId = surface.getCharacter(x,y) & 0xff;
                BufferedImage texture = textureMapFlipped.get(textureId & 0xff);
                if (!textureId.equals(EMPTY_SURFACE_TILE) && textureMap.containsKey(textureId)) {
                    g2d.drawImage(texture,x*16, y*16, 16, 16, null);
                }
            }
        }
//        g2d.setColor(Color.CYAN);
//        for (int x = 0; x < 16; x++) {
//            for (int y = 0; y < 16; y++) {
//                for (int w = 0; w < 5; w++) {
//                    for (int h = 0; h < 5; h++) {
//                        Integer unknown = surface.getUnknown(x * 5 + w, y * 5 + h);
//                        if (unknown > 0) {
//                            g2d.drawArc(x * 16 * 4 - 4 + w * 16, y * 16 * 4 + h * 16, 8, 8, 0, 360);
//                        }
//                    }
//                }
//            }
//        }
        g2d.dispose();
        ImageIO.write(image, "png", destinationPath.resolve(filename + ".block0.maxi0.textured_surface.png").toFile());
    }

    private void readSurfaceBlock2(HighlightGroups highlightGroups, ImageInputStream stream, Block file, Path destinationPath, String filename) throws IOException {
        Block blockDescription = file.getBlock("chunk_directory").getBlock("surface2");
        int offset = blockDescription.getInt("relativeOffset");
        int size = blockDescription.getInt("size");
        highlightGroups.addRange("height_maps", offset, size);

        Block block = file.createBlock("surface2", offset, size);
        if (blockDescription.getInt("size") == 0) {
            block.addProperty("skipped", "size is 0");
        }

        stream.seek(offset);
        DecompressedStream decompressedStream = new DecompressedStream(stream);
        ImageInputStream tileStream = decompressedStream.toStream();
        int decompressedSize = decompressedStream.getResult().length;
        Files.write(destinationPath.resolve( filename + ".surface0.raw"), decompressedStream.getResult());
        block.addProperty("decompressed_size", new HexValue(decompressedSize));
        block.addProperty("surface2", new Surface2(tileStream));

        tileStream.seek(0);
        Tile<Tile<Integer>> tile = new MaxiTile<>(64, 64, PolygonTile::new, tileStream);
        writeMaxiTile(tile, value -> {
            int a = value & 0xff;
            int b = (value >> 8) & 0xff;
            int c = (value >> 16) & 0xff;
            int d = (value >> 24) & 0xff;
            int height = (a + b + c + d) / 2;
            return new Color(height, height, height).getRGB();
        } , 16, destinationPath,filename + ".surface0.heightmap", false);

        Tile<Tile<Integer>> tile2 = new MaxiTile<>(64, 64,
                (s) -> new MaxiTile<>(1, 1, Sf3Util::readUnsignedShort, s), tileStream);
        writeMaxiTile(tile2, value -> {
            int upper = (value >> 8) & 0xff;
            return new Color(upper, upper, upper).getRGB();
        }, 16, destinationPath,filename + ".surface0.maxi1a", false);
        writeMaxiTile(tile2, value -> {
            int v = ((value & 0xff) << 5);
            v = Math.min(v, 0xff);
            return new Color(v, v, v).getRGB();
        }, 16, destinationPath, filename + ".surface0.movementcosts", false);

        Tile<Tile<Integer>> tile3 = new MaxiTile<>(64, 64,
                (s) -> new MaxiTile<>(1, 1, Sf3Util::readUnsignedByte, s), tileStream);
        writeMaxiTile(tile3, value -> {
            int color = value * 3;
            if (color == 0) {
                return 0;
            }

            switch (color / 256) {
                case 0:
                    return new Color(color, 0, 0xff).getRGB();
                case 1:
                    return new Color(0xff, color - 256, 0).getRGB();
                case 2:
                    return new Color(0xff, 0xff, color - 256 * 2).getRGB();
                default:
                    return 0;
            }
        }, 16, destinationPath, filename + ".surface0.trigger", false);
    }

    private static void readTextureAnimationImages(HighlightGroups highlightGroups, ImageInputStream stream, Block file, String filename) throws IOException {
        Block chunkOffset = file.getBlock("chunk_directory").getBlock("texture_animation_data");
        List<TextureAnimation> textureAnimations = file.getBlock("header").getObject("texture_animations");

        int offset = chunkOffset.getInt("relativeOffset");

        for (TextureAnimation textureAnimation : textureAnimations) {
            for (TextureFrame frame : textureAnimation.frames) {
                stream.seek(offset+frame.offset);
                DecompressedStream decompressedStream = new DecompressedStream(stream);
                ImageInputStream imageStream = decompressedStream.toStream();
                frame.image = Sf3Util.readBufferedImage(imageStream, textureAnimation.width, textureAnimation.height);
            }
        }
        highlightGroups.addRange("texture_animations", offset, chunkOffset.getInt("size"));
    }

    private static Block readTextures(ImageInputStream stream, Block chunkDictionary, String blockName) throws IOException {
        Block blockDescription = chunkDictionary.getBlock(blockName);
        int offset = blockDescription.getInt("relativeOffset");
        int size = blockDescription.getInt("size");

        Block block = new Block(blockName, offset, size);
        if (blockDescription.getInt("size") == 0) {
            block.addProperty("skipped","size is 0");
            block.addProperty("textures", new ArrayList<Texture>());
            return block;
        }

        stream.seek(offset);
        DecompressedStream decompressedStream = new DecompressedStream(stream);
        ImageInputStream imageStream = decompressedStream.toStream();

        int numTextures = imageStream.readUnsignedShort();
        block.addProperty("num_textures", new HexValue(numTextures));
        int textureIdStart = imageStream.readUnsignedShort();
        block.addProperty("texture_id_start", new HexValue(textureIdStart));

        List<Texture> textures = new ArrayList<>();
        for (int i = 0; i < numTextures; i++) {
            Texture texture = new Texture(textureIdStart + i, imageStream);
            textures.add(texture);
        }

        // read textures
        for (Texture texture : textures) {
            imageStream.seek(texture.getOffset());
            texture.setImage(Sf3Util.readBufferedImage(imageStream, texture.getWidth(), texture.getHeight()));
        }
        block.addProperty("textures", textures);
        return block;
    }

    private void readSurfaceTiles(HighlightGroups highlightGroups, ImageInputStream stream, Path outPath, Block file, String filename) throws IOException {
        Block blockDescription = file.getBlock("chunk_directory").getBlock("surfaceData");
        int offset = blockDescription.getInt("relativeOffset");
        int size = blockDescription.getInt("size");
        highlightGroups.addRange("surface_tiles", offset, size);

        Block block = file.createBlock("surface_tiles", offset, size);
        if (blockDescription.getInt("size") == 0) {
            block.addProperty("skipped","size is 0");
            return;
        }

        block.addProperty("startOffset", new HexValue(offset));
        byte[] chunkContent = new byte[size];
        stream.seek(offset);
        stream.readFully(chunkContent);
        Files.write(outPath.resolve(filename+".raw"), chunkContent);
        block.addProperty("_content", chunkContent);
        stream.seek(offset);
        block.addProperty("surface", new Surface(stream));

        stream.seek(offset);
        Tile<Tile<Integer>> tile = new MaxiTile<>(16, 16,
                (s) -> new MaxiTile<>(4,4, Sf3Util::readUnsignedShort, s), stream);
        block.addProperty("tiles", tile);

        block.addProperty("end_offset_block_0", new HexValue((int) stream.getStreamPosition()));
        // read next block with 5x5 tiles
        Tile<Tile<TilePart2>> secondTileStructure =
                new MaxiTile<>(16, 16,
                        (s) -> new MaxiTile<>(5, 5, TilePart2::new, s), stream);
        block.addProperty("end_offset_block_1", new HexValue((int) stream.getStreamPosition()));
        writeMaxiTile(secondTileStructure, value -> {
            float x = ((short) value.a) / 32768.0f;
            float y = ((short) value.b) / 32768.0f;
            float z = ((short) value.c) / 32768.0f;
            int r = (int) (x * 255.0) & 0xff;
            int g = (int) (y * 255.0) & 0xff;
            int b = (int) (z * 255.0) & 0xff;
            return r + (g << 8) + (b << 16);
        }, 8, outPath, filename+".normals", true);
        block.addProperty("tiles2", secondTileStructure);

        Tile<Tile<Integer>> thirdTiles = new MaxiTile<>(16, 16,
                s -> new MaxiTile<>(5, 5, Sf3Util::readUnsignedByte, s), stream);
        block.addProperty("end_offset_block_2", new HexValue((int) stream.getStreamPosition()));

        writeMaxiTile(thirdTiles, value -> new Color(value,value,value).getRGB(), 8, outPath, filename+".walkmesh", false);
        block.addProperty("tiles2", thirdTiles);

        block.addProperty("size", new HexValue((int) stream.getStreamPosition()-offset));
    }

    /** returns a vertically flipped version of the image. */
    protected BufferedImage flipVertically(BufferedImage image) {
        AffineTransform at = new AffineTransform();
        at.concatenate(AffineTransform.getScaleInstance(1, -1));
        at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
        return createTransformed(image, at);
    }

    private static BufferedImage createTransformed(
            BufferedImage image, AffineTransform at)
    {
        BufferedImage newImage = new BufferedImage(
                image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.transform(at);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

    /** One tile for the 2nd surface tile structure. */
    private static final class TilePart2 {
        final int a;
        final int b;
        final int c;

        public TilePart2(ImageInputStream stream) {
            try {
                a = stream.readUnsignedShort();
                b = stream.readUnsignedShort();
                c = stream.readUnsignedShort();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }

        public int getC() {
            return c;
        }
    }
    private <T> void writeMaxiTile(Tile<Tile<T>> tile, Function<T, Integer> toColorFunction, int scale, Path destinationPath, String filename, boolean writeTileIndices) {
        int subWidth = tile.get(0).getWidth();
        int subHeight = tile.get(0).getHeight();

        int width = tile.getWidth() * subWidth;
        int height = tile.getHeight() * subHeight;

        BufferedImage image = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                T value = tile.get(x / subWidth, y / subHeight).get(x % subWidth, y % subHeight);
                Integer rgb = toColorFunction.apply(value);
                g2d.setColor(new Color(rgb));
                g2d.fillRect(x*scale, y*scale, scale,scale);
            }
        }
        g2d.setColor(Color.WHITE);
        if (writeTileIndices) {
            for (int x = 0; x < tile.getWidth(); x++) {
                for (int y = 0; y < tile.getHeight(); y++) {
                    int tileIndex = tile.xyToIndex(x,y);
                    g2d.drawRect(x*subWidth*scale, y*subHeight*scale, scale*subWidth,scale*subHeight);
                    g2d.drawString(Integer.toHexString(tileIndex),x*subWidth*scale, (y+1)*subHeight*scale);
                }
            }
        }
        g2d.dispose();
        image = this.flipVertically(image);

        try {
            ImageIO.write(image, "png", destinationPath.resolve(filename+".png").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readChunkDirectory(HighlightGroups highlightGroups, Block file, ImageInputStream stream) throws IOException {
        // note: chunk directory always start at 0x2000
        stream.seek(0x2000);
        Block chunk = file.createBlock("chunk_directory", (int) stream.getStreamPosition(), 0);

        // map object models
        chunk.addBlock(readOffsetBlock(stream, "unknown_empty[0]"));
        chunk.addBlock(readOffsetBlock(stream, "mapObjects"));

        chunk.addBlock(readOffsetBlock(stream, "surfaceData"));

        chunk.addBlock(readOffsetBlock(stream, "texture_animation_data"));
        chunk.addBlock(readOffsetBlock(stream, "unknown_empty[1]"));
        chunk.addBlock(readOffsetBlock(stream, "surface2"));

        // read 5 texture chunks
        chunk.addBlock(readOffsetBlock(stream, "textures[0]"));
        chunk.addBlock(readOffsetBlock(stream, "textures[1]"));
        chunk.addBlock(readOffsetBlock(stream, "textures[2]"));
        chunk.addBlock(readOffsetBlock(stream, "textures[3]"));
        chunk.addBlock(readOffsetBlock(stream, "textures[4]"));
        // read 3 textures for objects (chest, barrel)
        chunk.addBlock(readOffsetBlock(stream, "object_textures[0]"));
        chunk.addBlock(readOffsetBlock(stream, "object_textures[1]"));
        chunk.addBlock(readOffsetBlock(stream, "object_textures[2]"));
        // additional chunks
        for (int i = 0; ; i++) {
            Block offsetBlock = readOffsetBlock(stream, "other[" + i + "]");
            if (offsetBlock.getInt("offset") == 0) {
                break;
            }
            chunk.addBlock(offsetBlock);
        }
        highlightGroups.addRange("chunk_directory", chunk.getStart(), (int) (stream.getStreamPosition()-chunk.getStart()));
    }

    private static Block readOffsetBlock(ImageInputStream stream, String name) throws IOException {
        Block offsetBlock = new Block(name, (int) stream.getStreamPosition(), 8);
        // offset in system memory
        int offset = stream.readInt();
        offsetBlock.addProperty("offset", new HexValue(offset));
        int relativeOffset = offset > 0 ? offset - MAP_MEMORY_OFFSET : 0;
        offsetBlock.addProperty("relativeOffset", new HexValue(relativeOffset));
        offsetBlock.addProperty("size", new HexValue(stream.readInt()));
        return offsetBlock;
    }

    protected abstract Block readHeader(HighlightGroups highlightGroups, Block file, ImageInputStream stream) throws IOException;

    protected Block readHeaderObjects(HighlightGroups highlightGroups, ImageInputStream stream, Block header, int offset, int num) throws IOException {
        if (offset == 0) {
            return null;
        }
        stream.seek(offset);
        Block stuff = new Block("object_"+num, offset, 0);
        int meshNo = 0;
        while (true) {
            int mesh_offset = readPointer(stream, highlightGroups, "header_objects");
            if (mesh_offset == 0) {
                break;
            }
            Block item = stuff.createBlock("["+meshNo+"]", offset, 0);
            item.addProperty("offset", new HexValue(mesh_offset));
            item.addProperty("position", new Point(
                    new Fixed(stream.readShort() << 16),
                    new Fixed(stream.readShort() << 16),
                    new Fixed(stream.readShort() << 16)
            ).toJson());

            item.addProperty("rotation", Arrays.asList(
                    new Angle(stream.readUnsignedShort()).getRadians(),
                    new Angle(stream.readUnsignedShort()).getRadians(),
                    new Angle(stream.readUnsignedShort()).getRadians()
            ));

            item.addProperty("scale", new Point(stream).toJson());
            meshNo++;
        }
        highlightGroups.addRange("header_objects", offset, (int) stream.getStreamPosition()-offset);

        // read mesh data
        for (Block block : stuff.valuesStream(Block.class).collect(Collectors.toList())) {
            int mesh_offset = block.getInt("offset");
            stream.seek(mesh_offset);
            PolygonData polygonData = new PolygonData(stream);
            polygonData.readDetails2(stream, -MAP_MEMORY_OFFSET);
            block.addProperty(("mesh_"+new HexValue(mesh_offset)), polygonData);
            highlightGroups.addRange("polygon_data", mesh_offset, PolygonData.SIZE);
            highlightGroups.addRange("points", polygonData.getPointsOffset()-MAP_MEMORY_OFFSET, polygonData.getNumPoints()*Point.SIZE);
            highlightGroups.addRange("polygons", polygonData.getPolygonOffset()-MAP_MEMORY_OFFSET, polygonData.getNumPolygons()* Polygon.SIZE);
            highlightGroups.addRange("polygon_attributes", polygonData.getPolygonAttributesOffset()-MAP_MEMORY_OFFSET, polygonData.getNumPolygons()*PolygonAttribute.SIZE);
        }
        return stuff;
    }

    protected void readTextureAnimationDescription(HighlightGroups highlightGroups, ImageInputStream stream, Block header) throws IOException {
        int offsetTextureAnimations = header.getInt("offset_texture_animations");
        stream.seek(offsetTextureAnimations);
        List<TextureAnimation> textureAnimations = new ArrayList<>();
        while (true) {
            int textureIndex = stream.readUnsignedShort();
            if (textureIndex == 0xffff) {
                // no more animations
                int pos = (int) stream.getStreamPosition();
                highlightGroups.addRange("texture_animations", offsetTextureAnimations, pos-offsetTextureAnimations);
                break;
            }
            TextureAnimation animation = new TextureAnimation(textureIndex,
                    stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort());
            while (true) {
                int offset = stream.readUnsignedShort();
                if (offset == 0xfffe) {
                    // no more frames in this animation
                    break;
                }

                animation.frames.add(new TextureFrame(offset, stream.readUnsignedShort()));
            }
            textureAnimations.add(animation);
        }
        header.addProperty("texture_animations", textureAnimations);
    }

    protected int readPointer(ImageInputStream stream, HighlightGroups highlightGroups, String highlightGroup) throws IOException {
        int filePos = (int) stream.getStreamPosition();
        int pointer = stream.readInt();
        if (pointer == 0) {
            return 0;
        }
        int relativePointer = pointer - MAP_MEMORY_OFFSET;
        highlightGroups.addPointer(highlightGroup, filePos, relativePointer);
        return relativePointer;
    }

    private static class TextureAnimation {
        private final int group;
        private final int width;
        private final int height;
        private final int speed;
        private final List<TextureFrame> frames = new ArrayList<>();

        public TextureAnimation(int group, int width, int height, int speed) {
            this.group = group;
            this.width = width;
            this.height = height;
            this.speed = speed;
        }

        public List<TextureFrame> getFrames() {
            return frames;
        }

        @Override
        public String toString() {
            return "{" +
                    "\"group\":" + new HexValue(group) +
                    ", \"width\":" + new HexValue(width) +
                    ", \"height\":" + new HexValue(height) +
                    ", \"speed\":" + new HexValue(speed) +
                    ", \"indices\":" + frames +
                    '}';
        }
    }

    /** One frame in a texture animation. */
    private static class TextureFrame {
        private final int offset;
        private final int unknown;
        public BufferedImage image;
        public int textureImageIndex;

        public TextureFrame(int offset, final int unknown) {
            this.offset = offset;
            this.unknown = unknown;
        }

        @Override
        public String toString() {
            return "{" +
                    "offset:" + new HexValue(offset) +
                    ", unknown: " + new HexValue(unknown) +
                    ", textureImageIndex: " + textureImageIndex +
                    ", width: " + image.getWidth() +
                    ", height: " + image.getHeight() +
                    '}';
        }
    }

    private void readMapObjects(HighlightGroups highlightGroups, Block fileBlock, ImageInputStream stream, String file) throws IOException {
        Block mapObjects = fileBlock.getBlock("chunk_directory").getBlock("mapObjects");
        int offset = mapObjects.getInt("relativeOffset");
        int relativeOffset = OFFSETS.getOrDefault(file, DEFAULT_OFFSET) - offset;

        try {
            stream.seek(offset);
            fileBlock.addProperty("map_objects", new MapObjects(stream, relativeOffset, highlightGroups));
        } catch (Exception e) {
            fileBlock.addProperty("cannot_read_models", Sf3Util.getExceptionLines(e).stream().map(line -> "\""+line+"\"").collect(Collectors.toList()));
        }
    }

    private void readStuffAtObjectOffset2(HighlightGroups highlightGroups, ImageInputStream stream, int relativeOffset, Block block, Pointer pointer2) throws IOException {
        if (pointer2.getValue().getValue() == 0) {
            return;
        }
        // read stuff at offset 2
        stream.seek(pointer2.getRelativeOffset().getValue());
        Block stuffAtOffset2 = block.createBlock("stuff_at_offset_2", pointer2.getRelativeOffset().getValue(), 0);
        List<Integer> offsets = new ArrayList<>();
        while (true) {
            int offsetValue = stream.readInt();
            if (offsetValue == -1) {
                break;
            }
            offsets.add(offsetValue - relativeOffset);
        }
        stuffAtOffset2.addProperty("size", offsets.size());
        Block offsetsBlock = stuffAtOffset2.createBlock("offsets", 0, 0);
        int min = 0x7fff;
        int max = 0;
        // these values index into the list at stuff_at_offset_1.values_2
        int offsetIndex = 0;
        for (int singleOffset : offsets) {
            stream.seek(singleOffset);
            List<HexValue> values = new ArrayList<>();
            while (true) {
                int value = stream.readUnsignedShort();
                if (value == 0xffff) {
                    break;
                }
                min = Math.min(min, value);
                max = Math.max(max, value);
                values.add(new HexValue(value));
            }
            offsetsBlock.addProperty(new HexValue(offsetIndex).toString(), values.toString().replace('"',' '));
            offsetIndex++;
        }
        highlightGroups.addRange("map_objects_stuff2", stuffAtOffset2.getStart(), (int) (stream.getStreamPosition() - stuffAtOffset2.getStart()));
        stuffAtOffset2.addProperty("min", new HexValue(min));
        stuffAtOffset2.addProperty("max", new HexValue(max));
    }

    private void readStuffAtObjectOffset1(HighlightGroups highlightGroups, ImageInputStream stream, int relativeOffset, Block block, Pointer pointer1, Pointer pointer2) throws IOException {
        // read stuff at offset 1
        // maybe this is some kind of binary tree for visibility culling of the objects
        // see bsp in doom (https://en.wikipedia.org/wiki/Binary_space_partitioning)

        if (pointer1.getValue().getValue() == 0) {
            return;
        }
        stream.seek(pointer1.getRelativeOffset().getValue());
        int length = pointer2.getValue().getValue() - pointer1.getValue().getValue();
        Block stuffAtOffset1 = block.createBlock("stuff_at_offset_1", pointer1.getRelativeOffset().getValue(), length);
        Pointer offset1 = new Pointer(stream, relativeOffset);
        Pointer offset2 = new Pointer(stream, relativeOffset);
        stuffAtOffset1.addProperty("offset_1", offset1);
        stuffAtOffset1.addProperty("offset_2", offset2);

        highlightGroups.addPointer("map_objects_header", pointer1.getRelativeOffset().getValue(), offset1.getRelativeOffset().getValue());
        highlightGroups.addPointer("map_objects_header", pointer1.getRelativeOffset().getValue()+4, offset2.getRelativeOffset().getValue());
        stream.seek(offset1.getRelativeOffset().getValue());
        int size1 = (offset2.getRelativeOffset().getValue() - offset1.getRelativeOffset().getValue()) / 4;
        List<Object> values1 = new ArrayList<>();
        // note: list seems to be terminated by 0xff
        for (int i = 0; i < size1; i++) {
            values1.add(new HexValue(stream.readInt()));
        }
        stuffAtOffset1.addProperty("count_1", values1.size());
        stuffAtOffset1.addProperty("count_1_hex", new HexValue(values1.size()));
        stuffAtOffset1.addProperty("values_1", values1);

        stream.seek(offset2.getRelativeOffset().getValue());
        int size2 = (pointer2.getRelativeOffset().getValue() - offset2.getRelativeOffset().getValue()) / 8;
        List<Object> values2 = new ArrayList<>();

        for (int i = 0; i < size2; i++) {
            HexValue value1 = new HexValue(stream.readInt());
            HexValue value2 = new HexValue(stream.readInt());
            values2.add("\""+value1+" "+value2+"\"");
        }
        stuffAtOffset1.addProperty("count_2", values2.size());
        stuffAtOffset1.addProperty("count_2_hex", new HexValue(values2.size()));
        stuffAtOffset1.addProperty("values_2", values2);
        highlightGroups.addRange("map_objects_stuff1", stuffAtOffset1.getStart(), stuffAtOffset1.getLength());
    }

    protected List<Integer> readPalette(HighlightGroups highlightGroups, ImageInputStream stream, int offset, int toOffset) throws IOException {
        if (offset < 1) {
            // note: sometimes the palette offset is not in the usual memory range. ignore the palette in this case.
            return Collections.emptyList();
        }
        stream.seek(offset);
        List<Integer> values = new ArrayList<>();
        int count = (toOffset - offset) / 2;

        for (int i = 0; i < count; i++) {
            int rgb16 = Sf3Util.readUnsignedShort(stream);
            int rgb24 = Sf3Util.rgb16ToRgb24(rgb16);
            values.add(rgb24);
        }
        highlightGroups.addRange("palette", offset, values.size() * 2);
        return values;
    }

    protected void dumpScrollPaneChunks(HighlightGroups highlightGroups, Path path, Path destinationPath, Block file, ImageInputStream stream) throws IOException {
        Block chunkDirectory = file.getBlock("chunk_directory");
        List<List<Integer>> palettes = file.getBlock("header").getObject("palettes");
        int i = 0;
        for (Block blockDescription : chunkDirectory.valuesStream(Block.class).filter(b -> b.getName().startsWith("other")).collect(Collectors.toList())) {
            int offset = blockDescription.getInt("relativeOffset");
            int size = blockDescription.getInt("size");
            if (size > 0) {
                highlightGroups.addRange("scroll_screens", offset, size);
                // filename w/o extension. ".raw" and ".decompressed" will be appended
                Path baseFilename = destinationPath.resolve(path.getFileName().toString() + ".other" + i);
                byte[] bytes = decompressChunk(stream, offset, size, baseFilename);
                if (bytes != null) {
                    for (int paletteNum = 0; paletteNum < palettes.size(); paletteNum++) {
                        List<Integer> palette = palettes.get(paletteNum);
                        if (palette.size() == 0) {
                            continue;
                        }
                        BufferedImage image = new BufferedImage(512, 128, BufferedImage.TYPE_INT_RGB);
                        ByteArrayOutputStream raw = new ByteArrayOutputStream();
                        ImageOutputStream rawStream = new MemoryCacheImageOutputStream(raw);
                        rawStream.setByteOrder(ByteOrder.BIG_ENDIAN);
                        ImageInputStream imageStream = new ByteArrayImageInputStream(bytes);
                        out:
                        for (int y = 0; y < image.getHeight(); y++) {
                            for (int x = 0; x < image.getWidth(); x++) {
                                int index = imageStream.read();
                                if (index == -1) {
                                    break out;
                                }
                                if (palette.size() > index) {
                                    image.setRGB(x, y, palette.get(index));
                                    rawStream.writeShort(Sf3Util.rgb24ToRgb16(palette.get(index)));
                                } else {
                                    rawStream.writeShort(0);
                                }
                            }
                        }
                        rawStream.close();
                        ImageIO.write(image, "PNG", destinationPath.resolve(path.getFileName().toString() + ".other" + i+""+paletteNum + ".png").toFile());
                        Files.write(destinationPath.resolve(path.getFileName().toString() + ".other" + i+""+paletteNum + ".raw"), raw.toByteArray());
                    }
                }
            }

            i++;
        }
    }
}
