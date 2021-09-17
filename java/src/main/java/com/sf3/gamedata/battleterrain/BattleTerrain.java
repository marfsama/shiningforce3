package com.sf3.gamedata.battleterrain;

import com.sf3.binaryview.HighlightGroups;
import com.sf3.gamedata.mpd.DecompressedStream;
import com.sf3.gamedata.sgl.*;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.util.Sf3Util;
import com.sf3.util.Utils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BattleTerrain {
    private static final String BASE_PATH = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/bin/x2-battleterrain";
    private static final String OUT_BASE_PATH = System.getProperty("user.home")+"/project/games/shiningforce3/data/battleterrain";

    public static void main(String[] args) throws IOException {
        BattleTerrain battleTerrain = new BattleTerrain();
        Files.list(Paths.get(BASE_PATH))
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map((Path::toString))
                .map(name -> name.replace(".bin", ""))
//                .filter(name -> name.equals("x2st009"))
                .sorted()
                .forEach(battleTerrain::analyzeFile);


    }

    private void analyzeFile(String fileName) {
        try {
            Path path = Paths.get(BASE_PATH, fileName + ".bin");
            Path outPath = Paths.get(OUT_BASE_PATH, fileName + ".json");
            if (!Files.exists(path) || Files.size(path) < 10) {
                return;
            }

            Block file = readFile(path);
            System.out.println("write: " + outPath.toAbsolutePath().toString());
            Files.writeString(outPath, Utils.toPrettyFormat(file.toString()));
        }
        catch (IOException ioe) {
            throw new IllegalStateException(fileName, ioe);
        }
    }

    public Block readFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            Block file = new Block(path.getFileName().toString(), 0, (int) Files.size(path));
            HighlightGroups highlightGroups = file.addProperty("highlights", new HighlightGroups());

            ImageInputStream stream = new MemoryCacheImageInputStream(is);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            String filename = path.getFileName().toString();
            file.addProperty("filename", path.toAbsolutePath().toString());
            file.addProperty("size", new HexValue(file.getLength()));
            List<ChunkItem> header = readHeader(file, stream);
            stream.seek(header.get(0).getOffset().getValue());


            readGroundImage(file, stream, filename, header);
            readTextures(file, stream, filename, header);
            readMeshes(file, highlightGroups, stream, header);


            return file;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readMeshes(Block file, HighlightGroups highlightGroups, ImageInputStream stream, List<ChunkItem> header) throws IOException {
        Block meshChunk = file.createBlock("mesh_chunk");
        int chunkOffset = header.get(1).getOffset().getValue();
        stream.seek(chunkOffset);

        int instancesOffset = stream.readInt();
        meshChunk.addProperty("instance_offset", new HexValue(instancesOffset));
        int meshOffset = stream.readInt();
        meshChunk.addProperty("mesh_offset", new HexValue(meshOffset));

        stream.seek(chunkOffset + (long) instancesOffset);
        List<ObjectInstance> instances = meshChunk.addProperty("instances", new ArrayList<>());
        while (true) {
            int meshId = stream.readUnsignedShort();
            if (meshId == 0xFFFF) {
                break;
            }

            EulerAngles rotation = new EulerAngles(stream);
            Point position = new Point(stream);
            Point scale = new Point(stream);
            instances.add(new ObjectInstance(meshId, rotation, position, scale));
        }
        highlightGroups.addRange("mesh_instances", chunkOffset+8, (int) (stream.getStreamPosition()-chunkOffset+8));
        // read pdata
        stream.seek(chunkOffset + (long) meshOffset);

        List<PolygonData> meshes = meshChunk.addProperty("meshes", new ArrayList<>());
        while (true) {
            PolygonData e = new PolygonData(stream);
            if (e.getPointsOffset() == -1) {
                break;
            }
            meshes.add(e);
        }
        highlightGroups.addRange("mesh_PDATA", chunkOffset+meshOffset, (int) (stream.getStreamPosition()-(chunkOffset+meshOffset)));

        for (PolygonData polygonData : meshes) {
            polygonData.readDetails2(stream, chunkOffset);

            highlightGroups.addRange("points", chunkOffset+polygonData.getPointsOffset(), polygonData.getNumPoints()*Point.SIZE);
            highlightGroups.addRange("polygons", chunkOffset+polygonData.getPolygonOffset(), polygonData.getNumPolygons()* Polygon.SIZE);
            highlightGroups.addRange("polygon_attributes", chunkOffset+polygonData.getPolygonAttributesOffset(), polygonData.getNumPolygons()*PolygonAttribute.SIZE);
        }


    }

    private void readTextures(Block file, ImageInputStream stream, String filename, List<ChunkItem> header) throws IOException {
        Block textureChunk = file.createBlock("textures");
        int chunkOffset = header.get(2).getOffset().getValue();
        stream.seek(chunkOffset);

        int offsetTextures = stream.readInt();
        textureChunk.addProperty("offset_textures", new HexValue(offsetTextures));
        textureChunk.addProperty("header_size", new HexValue(stream.readInt()));
        int numTextures = stream.readInt();
        textureChunk.addProperty("texture_count", new HexValue(numTextures));
        textureChunk.addProperty("decompressed_size", stream.readInt());
        textureChunk.addProperty("padding1", stream.readInt());
        textureChunk.addProperty("padding2", stream.readInt());

        // read texture definitions
        List<Texture> textures = textureChunk.addProperty("texture_list", new ArrayList<>());
        for (int i = 0; i < numTextures; i++) {
            textures.add(new Texture(stream, "name"));
        }
        textureChunk.addProperty("current_offset", new HexValue((int) stream.getStreamPosition()));

        List<BufferedImage> images = new ArrayList<>();
        stream.seek( chunkOffset + offsetTextures + 8L);
        DecompressedStream decompressedStream = new DecompressedStream(stream);
        ImageInputStream imageStream = decompressedStream.toStream();
        for (Texture texture : textures) {
            imageStream.seek(texture.getVramAddress());
            images.add(Sf3Util.readBufferedImage(imageStream, texture.getWidth(), texture.getHeight()));
        }
        String textureFileName = Paths.get(OUT_BASE_PATH, filename+".textures.png").toAbsolutePath().toString();
        List<TextureUv> uvs = Sf3Util.writeTextureImage(images, Collections.emptySet(), textureFileName, 0, false);
        textureChunk.addProperty("uvs", uvs);
        textureChunk.addProperty("texture_file", filename+".textures.png");
    }

    private void readGroundImage(Block file, ImageInputStream stream, String filename, List<ChunkItem> header) throws IOException {
        file.addProperty("ground_value", new HexValue(stream.readInt()));
        int[] palette = readPalette(stream);
        // read image
        int height = (header.get(0).getDataSize().getValue() - 0x200) / 512;
        BufferedImage image = createBufferedImage(stream, 512, height, palette);
        ImageIO.write(image, "png", Paths.get(OUT_BASE_PATH, filename +".ground.png").toFile());
        file.addProperty("ground_file", filename +".ground.png");
    }

    private BufferedImage createBufferedImage(ImageInputStream stream, int width, int height, int[] palette) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int value = stream.readUnsignedByte();
                    image.setRGB(x, y, palette[value]);
                }
            }
            return image;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    private static int[] readPalette(ImageInputStream stream) throws IOException {
        int[] palette = new int[256];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = Sf3Util.rgb16ToRgb24(stream.readUnsignedShort());
        }
        return palette;
    }


    private List<ChunkItem> readHeader(Block file, ImageInputStream stream) throws IOException {
        List<ChunkItem> header = file.addProperty("header", new ArrayList<>());
        for (int i = 0; i < 4; i++) {
            header.add(new ChunkItem(stream));
        }
        return header;
    }

}
