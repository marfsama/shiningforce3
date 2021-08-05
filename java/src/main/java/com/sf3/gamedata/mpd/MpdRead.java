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

        // broken beyond repair
        put("ship2.mpd" , 0x02477E8 + 0x2100);
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
            highlightGroups.addGroup("textureAnimations", 0xcc66ff);
            highlightGroups.addGroup("pink", Color.PINK.getRGB());
            file.addProperty("highlights", highlightGroups);


            readHeader(highlightGroups, file, stream);
            readChunkDirectory(file, stream);
            readMapObjects(file, stream, filename);
            readSurfaceTiles(stream, outPath, file, filename +".surface_tiles");
            // read texture animations, updating the animation list in the header.
            readTextureAnimationImages(stream, file, filename +".textureaminations");

            readSurfaceBlock2(stream, file, outPath,filename +".surface2");

            readTextureChunks(mpdFile, outPath, file, stream);

            // I think these are scroll screens. Some are background images (skybox). some seems
            // to be ground textures like dirt or water.
            // Scroll screen can be a big image (bit map format) or a character indexed (cell format) pattern
            // page 65
            // 3rd image (other[2]) seems to be some kind of scroll screen configuration (cell configuration)
            dumpOtherChunks(mpdFile, outPath, file, stream);


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

        Block textureChunks = file.getBlock("texture_chunks");
        for (Object value : textureChunks.getProperties().values()) {
            if (value instanceof Block) {
                Block textureChunk = (Block) value;
                if (textureChunk.hasProperty("textures")) {
                    List<Texture> textures = textureChunk.getObject("textures");
                    textures.forEach(texture -> {
                        texture.setTextureImageIndex(textureImages.size());
                        textureImages.add(texture.getImage());
                    });
                }
            }
        }

        List<TextureAnimation> textureAnimations = file.getBlock("header").getObject("texture_animations");
        textureAnimations.stream().map(TextureAnimation::getFrames)
                .flatMap(List::stream)
                .forEach(frame -> {
                    frame.textureImageIndex = textureImages.size();
                    textureImages.add(frame.image);
                });

        List<TextureUv> uvMap = Sf3Util.writeTextureImage(textureImages, new HashSet<Integer>(), outPath.resolve(filename + ".png").toAbsolutePath().toString(), 0, false);
        Block uvBlock = file.createBlock("uvs", 0, 0);
        uvBlock.addProperty("filename", filename+".png");
        uvBlock.addProperty("uv_map",uvMap);
    }

    private Map<Integer, Texture> readTextureChunks(Path path, Path destinationPath, Block file, ImageInputStream stream) throws IOException {
        List<Texture> textures = new ArrayList<>();
        Block chunkDirectory = file.getBlock("chunk_directory");
        Block textureChunks = file.createBlock("texture_chunks", 0, 0);
        int i = -1;
        for (Block blockDescription : chunkDirectory.valuesStream(Block.class).filter(b -> b.getName().startsWith("textures")).collect(Collectors.toList())) {
            i++;
            int offset = blockDescription.getInt("relativeOffset");
            int size = blockDescription.getInt("size");
            if (size == 0) {
                textureChunks.addProperty(blockDescription.getName(), "chunk size is 0, skipped");
                // note: normally this means there are no more textures, so the continue could also be a break
                continue;
            }

            // filename w/o extension. ".raw" and ".decompressed" will be appended
            Path baseFilename = destinationPath.resolve(path.getFileName().toString() + ".texture" + i);

            decompressChunk(stream, offset, size, baseFilename);

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
            if (size == 0) {
                break;
            }

            // filename w/o extension. ".raw" and ".decompressed" will be appended
            Path baseFilename = destinationPath.resolve(path.getFileName().toString() + ".objecttexture" + i);

            decompressChunk(stream, offset, size, baseFilename);

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
        Path rawFilename = baseFilename.getParent().resolve(baseFilename.getFileName().toString()+".raw");
        Files.write(rawFilename, buffer);
        // try to decompress the chunk
        try {
            ImageInputStream byteStream = new ByteArrayImageInputStream(buffer);
            byteStream.setByteOrder(stream.getByteOrder());
            DecompressedStream decompressedStream = new DecompressedStream(byteStream);
            byte[] decompressesBytes = decompressedStream.getResult();
            // byteStream should be exhausted when the entire chunk was compressed.
            // otherwise the compression worked "by accident" and the chunk was not really compressed.
            // note: leave some slack for padding bytes.
            if (byteStream.getStreamPosition() >= (buffer.length - 16)) {
                Path decompressedFilename = baseFilename.getParent().resolve(baseFilename.getFileName().toString() + ".decompressed");
                Files.write(decompressedFilename, decompressesBytes);
            }
            return decompressesBytes;
        } catch (Exception e) {

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
                if (textureId != EMPTY_SURFACE_TILE && textureMap.containsKey(textureId)) {
                    g2d.drawImage(texture,x*16, y*16, 16, 16, null);
                }
            }
        }
        g2d.dispose();
        ImageIO.write(image, "png", destinationPath.resolve(filename + ".block0.maxi0.textured_surface.png").toFile());
    }

    private static void readSurfaceBlock2(ImageInputStream stream, Block file, Path destinationPath, String filename) throws IOException {
        Block blockDescription = file.getBlock("chunk_directory").getBlock("surface2");
        int offset = blockDescription.getInt("relativeOffset");
        int size = blockDescription.getInt("size");

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
        writeMaxiTile(tile, value -> (value << 11) , 5, destinationPath,filename + ".surface0.maxi0", false);

        Tile<Tile<Integer>> tile2 = new MaxiTile<>(64, 64,
                (s) -> new MaxiTile<>(1, 1, Sf3Util::readUnsignedShort, s), tileStream);
        writeMaxiTile(tile2, value -> Sf3Util.rgb16ToRgb24(value), 10, destinationPath,filename + ".surface0.maxi1a", false);
        writeMaxiTile(tile2, value -> (value << 4), 10, destinationPath, filename + ".surface0.maxi1b", false);

        Tile<Tile<Integer>> tile3 = new MaxiTile<>(64, 64,
                (s) -> new MaxiTile<>(1, 1, Sf3Util::readUnsignedByte, s), tileStream);
        writeMaxiTile(tile3, value -> (value << 11), 10, destinationPath, filename + ".surface0.maxi2", false);
    }

    private static void readTextureAnimationImages(ImageInputStream stream, Block file, String filename) throws IOException {
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

    private static void readSurfaceTiles(ImageInputStream stream, Path outPath, Block file, String filename) throws IOException {
        Block blockDescription = file.getBlock("chunk_directory").getBlock("surfaceData");
        int offset = blockDescription.getInt("relativeOffset");
        int size = blockDescription.getInt("size");

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

        writeMaxiTile(thirdTiles, Sf3Util::rgb16ToRgb24, 8, outPath, filename+".unknown", true);
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
    private static <T> void writeMaxiTile(Tile<Tile<T>> tile, Function<T, Integer> toColorFunction, int scale, Path destinationPath, String filename, boolean writeTileIndices) {
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

        g2d.drawString("0x"+Integer.toHexString(tile.getStreamOffset()),20, 20);

        g2d.dispose();
        try {
            ImageIO.write(image, "png", destinationPath.resolve(filename+".png").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readChunkDirectory(Block file, ImageInputStream stream) throws IOException {
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
    }

    private static Block readOffsetBlock(ImageInputStream stream, String name) throws IOException {
        Block offsetBlock = new Block(name, (int) stream.getStreamPosition(), 16);
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
            int mesh_offset = readPointer(stream, highlightGroups, "stuff_at_offset_8");
            if (mesh_offset == 0) {
                break;
            }
            Block item = stuff.createBlock("["+meshNo+"]", offset, 0);
            item.addProperty("offset", new HexValue(mesh_offset));
            item.addProperty("position", new Point(
                    new Fixed(stream.readShort() << 16),
                    new Fixed(stream.readShort() << 16),
                    new Fixed(stream.readShort() << 16)
            ));

            item.addProperty("rotation", Arrays.asList(
                    new Angle(stream.readUnsignedShort()).getRadians(),
                    new Angle(stream.readUnsignedShort()).getRadians(),
                    new Angle(stream.readUnsignedShort()).getRadians()
            ));

            item.addProperty("scale", new Point(stream));
            meshNo++;
        }
        // note: maybe the "stuff at offset 8" is terminated when the offset is 0x00000000
        highlightGroups.addRange("stuff_at_offset_8", offset, (int) stream.getStreamPosition()-offset);

        // read mesh data
        for (Block block : stuff.valuesStream(Block.class).collect(Collectors.toList())) {
            int mesh_offset = block.getInt("offset");
            stream.seek(mesh_offset);
            PolygonData polygonData = new PolygonData(stream, "mesh");
            polygonData.readDetails2(stream, -MAP_MEMORY_OFFSET);
            block.addBlock(polygonData);
            highlightGroups.addRange("polygonData", mesh_offset, PolygonData.SIZE);
            highlightGroups.addRange("points", polygonData.getPointsOffset()-MAP_MEMORY_OFFSET, polygonData.getNumPoints()*Point.SIZE);
            highlightGroups.addRange("polygons", polygonData.getPolygonOffset()-MAP_MEMORY_OFFSET, polygonData.getNumPolygons()* Polygon.SIZE);
            highlightGroups.addRange("polygonAttributes", polygonData.getPolygonAttributesOffset()-MAP_MEMORY_OFFSET, polygonData.getNumPolygons()*PolygonAttribute.SIZE);
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
                highlightGroups.addRange("textureAnimations", offsetTextureAnimations, pos-offsetTextureAnimations);
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

    private void readMapObjects(Block fileBlock, ImageInputStream stream, String file) throws IOException {
        Block mapObjects = fileBlock.getBlock("chunk_directory").getBlock("mapObjects");
        int offset = mapObjects.getInt("relativeOffset");
        int size = mapObjects.getInt("size");
        int relativeOffset = OFFSETS.getOrDefault(file, DEFAULT_OFFSET) - offset;

        Block block = fileBlock.createBlock("map_objects", offset, size);
        try {
            stream.seek(offset);

            block.addProperty("header_pointer1", new Pointer(stream, relativeOffset));
            block.addProperty("header_pointer2", new Pointer(stream, relativeOffset));
            block.addProperty("numObjects", new HexValue(stream.readShort()));
            block.addProperty("header_zero", new HexValue(stream.readShort()));

            Block models = block.createBlock("models", 0, 0);

            List<ModelHead> heads = new ArrayList<>();
            for (int i = 0; i < block.getInt("numObjects"); i++) {
                ModelHead modelHead = new ModelHead(stream, "modelhead[" + i + "]", relativeOffset);
                heads.add(modelHead);
                models.addBlock(modelHead);
            }
            block.addProperty("end_offset", new HexValue((int) stream.getStreamPosition()));
            // read polygon data structure for each model head
            for (ModelHead head : heads) {
                head.readData(stream);
            }
        } catch (Exception e) {
            block.addProperty("cannot_read_models", Sf3Util.getExceptionLines(e).stream().map(line -> "\""+line+"\"").collect(Collectors.toList()));
        }
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

    protected void dumpOtherChunks(Path path, Path destinationPath, Block file, ImageInputStream stream) throws IOException {
        Block chunkDirectory = file.getBlock("chunk_directory");
        List<List<Integer>> palettes = file.getBlock("header").getObject("palettes");
        int i = 0;
        for (Block blockDescription : chunkDirectory.valuesStream(Block.class).filter(b -> b.getName().startsWith("other")).collect(Collectors.toList())) {
            int offset = blockDescription.getInt("relativeOffset");
            int size = blockDescription.getInt("size");
            if (size > 0) {
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
