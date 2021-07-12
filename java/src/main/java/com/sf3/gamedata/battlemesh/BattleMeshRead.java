package com.sf3.gamedata.battlemesh;

import com.google.common.collect.Streams;
import com.sf3.gamedata.sgl.Point;
import com.sf3.gamedata.sgl.Polygon;
import com.sf3.gamedata.utils.Block;
import com.sf3.gamedata.utils.HexValue;
import com.sf3.gamedata.mpd.DecompressedStream;
import com.sf3.gamedata.sgl.*;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.util.Sf3Util;
import com.sf3.util.Utils;
import com.sf3.util.WavefrontObjWriter;

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
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BattleMeshRead {
    private static final String basePath = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/bin";

    public static void main(String[] args) throws IOException {
        analyzeFiles();
//        dumpAllMeshes();
//        dumpMesh("x8pc03a.bin");
    }

    private static void dumpMesh(String filename) {
        Path path = Paths.get(basePath, filename);

        Block block = readFile(path);
        Block meshes = block.getBlock("meshes");
        Block textures = block.getBlock("textures");
        dumpMeshes(meshes, textures, Paths.get(filename+".obj"));
    }

    private static void dumpAllMeshes() {
        try (Stream<Path> files = Files.walk(Paths.get(basePath))) {
            files.filter(f -> f.getFileName().toString().startsWith("x8pc"))
                    .map(f -> {
                        try {
                            return readFile(f);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(block -> {
                        Block meshes = block.getBlock("meshes");
                        Block textures = block.getBlock("textures");
                        String filename = block.getObject("filename");
                        dumpMeshes(meshes, textures, Paths.get(filename+".obj"));
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void analyzeFiles() {
        int[] nums = new int[] {0,1,2,3};
        String[] letters = new String[] {"a","b","c","e","f","g"};

        List<String> files = new ArrayList<>();
        for (int num : nums) {
            for (String letter : letters) {
                files.add(String.format("x8pc%02d%s", num, letter));
            }
        }

        files.forEach(BattleMeshRead::analyzeFile);
    }

    private static void analyzeFile(String fileName) {
        try {
            Path path = Paths.get(basePath, fileName + ".bin");
            Path outPath = Paths.get(fileName + "_full.json");

            Block file = readFile(path);
            addStatistics(file);
            System.out.println("wite: " + outPath.toAbsolutePath().toString());
            Files.writeString(outPath, Utils.toPrettyFormat(file.toString()));
        }
        catch (IOException ioe) {
            throw new IllegalStateException(fileName, ioe);
        }
    }

    private static void addStatistics(Block file) {
        // adds some statistics to the parsed file to ease the interpretation
        Block statistics = file.createBlock("statistics", 0, 0);
        statistics.addProperty("numMeshes", file.getBlock("meshes").getInt("numMeshes"));
        statistics.addProperty("hasWeaponMesh", file.getBlock("meshes").getInt("weaponMeshOffset") > 0);

        // find bounding box for each mesh and for the total model
        List<PolygonDataExtended> meshes = file.getBlock("meshes").getProperties().values()
                .stream()
                .filter(PolygonDataExtended.class::isInstance)
                .map(PolygonDataExtended.class::cast)
                .collect(Collectors.toList());

        List<BoundingBox> boundingBoxes = meshes.stream().map(BattleMeshRead::getBoundingBox).collect(Collectors.toList());

        BoundingBox modelBoundingBox = new BoundingBox();
        boundingBoxes.stream().map(BoundingBox::getMin).forEach(modelBoundingBox::addPoint);
        boundingBoxes.stream().map(BoundingBox::getMax).forEach(modelBoundingBox::addPoint);
        statistics.addProperty("boundingBox", modelBoundingBox);


        statistics.addProperty("numAnimations", file.getBlock("animations").getBlock("animations").getProperties().size());
        List<String> animationsTable = getAnimationsTable(file.getBlock("animations").getBlock("animations"));
        statistics.addProperty("animationsTable", animationsTable);

        statistics.addProperty("numKeyFrameStuff", file.getBlock("animations").getBlock("dictionary").getProperties().size());
        List<String> dictionaryTable = getDictionaryTable(file.getBlock("animations").getBlock("dictionary"));
        statistics.addProperty("dictionaryTable", dictionaryTable);

        statistics.addProperty("keyframes_transposed", getTransposeKeyframes(file.getBlock("animations").getBlock("dictionary")));

        // keyframe clusters in bonekeyframe animation block
        // ie 1..30, 100..140, 200..233
    }

    private static Map<String, Object> getTransposeKeyframes(Block keyFrames) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("translation", getTransposedChannel(keyFrames,"translation"));
        result.put("rotation", getTransposedChannel(keyFrames,"rotation"));
        result.put("scale", getTransposedChannel(keyFrames,"scale"));

        return result;
    }

    private static Map<Integer, Object> getTransposedChannel(Block keyFrames, String channelName) {
        int[] times = keyFrames.getProperties().values().stream()
                .map(Block.class::cast)
                .map(animation -> animation.<Map<Integer, Object>>getObject(channelName))
                .flatMap(channel -> channel.keySet().stream())
                .mapToInt(Integer::valueOf)
                .distinct()
                .sorted()
                .toArray();
        Map<Integer, Object> result = new TreeMap<>();
        for (int time : times) {
            List<Object> list = keyFrames.getProperties().values().stream()
                    .map(Block.class::cast)
                    .map(animation -> animation.<Map<Integer, Object>>getObject(channelName))
                    .map(channel -> channel.get(time))
                    .collect(Collectors.toList());
            result.put(time, list);
        }
        return result;
    }

    private static List<String> getDictionaryTable(Block keyFrames) {
        String line = "\"| %2d | %3d / %3d / %3d | %3d | %3d |\"";
        List<String> table = new ArrayList<>();
        table.add("\"| No  | num Keyframes  | min | max |\"");
        table.add("\"+-----+----------------+-----+-----+\"");
        AtomicInteger i = new AtomicInteger();
        List<String> lines = keyFrames.getProperties().values().stream()
                .map(Block.class::cast)
                .map(animation ->
                             String.format(line,
                                    i.getAndIncrement(),
                                    animation.getInt("translation_key_frames"),
                                    animation.getInt("rotation_key_frames"),
                                    animation.getInt("scale_key_frames"),
                                    animation.<Map<Integer, ?>>getObject("translation").keySet()
                                            .stream().mapToInt(Integer::valueOf)
                                            .min().orElse(-1),
                                    animation.<Map<Integer, ?>>getObject("translation").keySet()
                                            .stream().mapToInt(Integer::valueOf)
                                            .max().orElse(-1)
                            )

                ).collect(Collectors.toList());

        table.addAll(lines);
        return table;
    }

    private static List<String> getAnimationsTable(Block animations) {
        String line = "\"| %d | %13s | %4s (%4d) | %8s | %8s | %9d | %s |\"";
        List<String> table = new ArrayList<>();
        table.add("\"| No | Animation Id | Frames      | unknown2 | distance | No events | events |\"");
        table.add("\"+----+--------------+-------------+----------+----------+-----------+--------+\"");
        AtomicInteger i = new AtomicInteger();
        List<String> lines = animations.getProperties().values().stream()
                .map(Block.class::cast)
                .map(animation -> {
                            return String.format(line,
                                    i.getAndIncrement(),
                                    animation.getObject("animationID"),
                                    animation.getObject("numberOfFrames"),
                                    animation.getInt("numberOfFrames"),
                                    animation.getObject("unknown2"),
                                    animation.getObject("distanceFromEnemy"),
                                    animation.getBlock("events").getProperties().size(),
                                    animation.getBlock("events").getProperties().values().stream()
                                        .map(Block.class::cast)
                                        .map(block -> block.getObject("frame")+" / "+block.getObject("eventCode"))
                                        .collect(Collectors.joining(", "))
                                    );
                        }

                ).collect(Collectors.toList());

        table.addAll(lines);
        return table;
    }

    private static BoundingBox getBoundingBox(PolygonDataExtended polygonDataExtended) {
        BoundingBox boundingBox = new BoundingBox();
        polygonDataExtended.getPoints().forEach(boundingBox::addPoint);
        return boundingBox;
    }

    private static void dumpAttributes(Block meshes) {
        for (int i = 0; i < meshes.getInt("numMeshes"); i++){
            String name = "mesh[" + i + "]";
            dumpAttribute(name, meshes.getBlock(name));
        }
        dumpAttribute("weaponMesh", meshes.getBlock("weaponMesh"));
    }

    private static void dumpAttribute(String name, PolygonDataExtended mesh) {
        System.out.println(name);
        mesh.getPolygonAttributes().forEach(System.out::println);
    }


    private static void dumpMeshes(Block meshes, Block textures, Path destination) {
        WavefrontObjWriter obj = new WavefrontObjWriter();
        String textureFilename = textures.getObject("textureFileName");
        obj.setMaterialLibrary(textureFilename);
        obj.setMaterial("textured");
        for (int i = 0; i < meshes.getInt("numMeshes"); i++){
            String name = "mesh[" + i + "]";
            dumpMesh(obj, name, meshes.getBlock(name));
        }
        dumpMesh(obj, "weaponMesh", meshes.getBlock("weaponMesh"));

        try {
            Files.write(destination, obj.toString().getBytes());
            System.out.println("written to "+destination);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void dumpMesh(WavefrontObjWriter obj, String name, PolygonDataExtended mesh) {
        obj.nextGroup(name);
        mesh.getPoints().stream().forEach(p -> obj.addVertex(p.getX().toFloat(), p.getY().toFloat(), p.getZ().toFloat()));
        for (int i = 0; i < mesh.getPolygons().size(); i++) {
            Polygon polygon = mesh.getPolygons().get(i);
            PolygonAttribute attributes = mesh.getPolygonAttributes().get(i);

            List<Integer> vertices = Arrays.stream(polygon.getVertexIndices()).mapToObj(Integer::valueOf).collect(Collectors.toList());
            List<Integer> textureVertices = null;
            obj.addFace(vertices, textureVertices);

        }
    }

    public static Block readFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            Block file = new Block(path.getFileName().toString(), 0, (int) Files.size(path));
            ImageInputStream stream = new MemoryCacheImageInputStream(is);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);
            file.addProperty("filename", path.getFileName().toString());
            file.addProperty("size", new HexValue(file.getLength()));
            file.addBlock(readHeader(stream));

            file.addBlock(readTexturesDefinition(stream, file.getBlock("header").getBlock("texturesDefinitions")));
            file.addBlock(readTextures(stream, file.getBlock("header").getBlock("textures"), file.getBlock("textureDefinitions"),  path.getFileName().toString()));
            file.addBlock(readMeshChunk(stream, file.getBlock("header").getBlock("meshChunk")));
            file.addBlock(readAnimations(stream, file.getBlock("header").getBlock("offset[1]")));
            saveTextures(file.getBlock("meshes"), file.getBlock("textures"));

            return file;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void saveTextures(Block meshes, Block textures) {
        // find solid face colors
        Set<Integer> colors = new HashSet<>();
        Block bodyMeshes = meshes.getBlock("body_meshes");
        for (String name : bodyMeshes.getProperties().keySet()) {
            PolygonDataExtended pdata = bodyMeshes.getObject(name);
            for (PolygonAttribute attribute : pdata.getPolygonAttributes()) {
                int drawMode = attribute.getDir() & 0x0f;
                if (drawMode == PolygonAttribute.DRAW_MODE_POLY) {
                    colors.add(attribute.getColno());
                }
            }
        }
        if (meshes.hasProperty("weaponMesh")) {
            PolygonDataExtended weaponMesh = meshes.getObject("weaponMesh");
            for (PolygonAttribute attribute : weaponMesh.getPolygonAttributes()) {
                int drawMode = attribute.getDir() & 0x0f;
                if (drawMode == PolygonAttribute.DRAW_MODE_POLY) {
                    colors.add(attribute.getColno());
                }
            }
        }

        textures.addProperty("solid_colors", colors);
        List<BufferedImage> images = textures.getObject("images");
        String filename = textures.getObject("textureFileName");

        List<TextureUv> uvs = Sf3Util.writeTextureImage(images, colors, filename, 0, false);
        textures.addProperty("uvs", uvs);


    }


    /** Read Chunk 1 */
    private static Block readAnimations(ImageInputStream stream, Block chunkOffsets) throws IOException {
        stream.seek(chunkOffsets.getInt("offset"));
        Block chunk = new Block("animations", chunkOffsets.getInt("offset"), chunkOffsets.getInt("data_size"));
        chunk.addProperty("chunk_offset", new HexValue(chunk.getStart()));
        chunk.addProperty("chunk_length", new HexValue(chunk.getLength()));

        Block header = chunk.createBlock("header", chunk.getStart(), 4*7);
        // bounding box?
        header.addProperty("bounding_box_min", new Point(stream));
        header.addProperty("bounding_box_max", new Point(stream));

        int dictionaryOffset = stream.readInt();
        header.addProperty("dictionary_offset", new HexValue(dictionaryOffset));
        header.addProperty("dictionary_offset_absolute", new HexValue(dictionaryOffset + chunk.getStart()));
        header.addProperty("header_end", new HexValue((int) stream.getStreamPosition()));

        Block animations = chunk.createBlock("animations", chunk.getStart(), 0);
        int j = 0;
        while (true) {
            // note: names copied from https://github.com/knight0fdragon/PlayerModelTest/blob/master/3DModelTest/Classes/Animations.cs
            int animationId = stream.readUnsignedShort();
            if (animationId == 0xffff) {
                break;
            }
            Block entry = animations.createBlock("animation["+j+"]", (int) stream.getStreamPosition(), 3*4);
            entry.addProperty("animationID", new HexValue(animationId));
            entry.addProperty("numberOfFrames", new HexValue(stream.readUnsignedShort()));
            entry.addProperty("unknown2", new HexValue(stream.readUnsignedShort()));
            entry.addProperty("distanceFromEnemy", new HexValue(stream.readUnsignedShort()));
            int offset = stream.readInt();
            entry.addProperty("_offset", new HexValue(offset));
            entry.addProperty("_absolute_offset", new HexValue(offset + chunk.getStart()));
            j++;
        }
        header.addProperty("animations_end", new HexValue((int) stream.getStreamPosition()));
        // read animation events
        for (Object property : animations.getProperties().values()) {
            Block block = (Block) property;
            int offset = block.getInt("offset");
            stream.seek((long) chunk.getStart() + offset);
            j = 0;
            Block events = block.createBlock("events",chunk.getStart() + offset, 0);
            while (true) {
                int frame = stream.readUnsignedShort();
                if (frame == 0xffff) {
                    break;
                }
                Block event = events.createBlock("event["+j+"]",chunk.getStart() + offset, 0);
                event.addProperty("frame", new HexValue(frame));
                event.addProperty("eventCode", new HexValue(stream.readUnsignedShort()));
                j++;
            }
        }
        header.addProperty("animation_events_end", new HexValue((int) stream.getStreamPosition()));


        stream.seek((long) chunk.getBlock("header").getInt("dictionary_offset") + chunk.getStart());
        Block dictionary = chunk.createBlock("dictionary", chunk.getStart(), 0);
        int i = 0;
        while (true) {
            int translationKeyFrames = stream.readInt();
            if (translationKeyFrames == 0xffffffff)  {
                break;
            }
            Block entry = dictionary.createBlock("dictionary["+i+"]", (int) stream.getStreamPosition(), 16*4);
            int rotationKeyFrames = stream.readInt();
            int scaleKeyFrames = stream.readInt();

            entry.addProperty("translation_key_frames", new HexValue(translationKeyFrames));
            entry.addProperty("rotation_key_frames", new HexValue(rotationKeyFrames));
            entry.addProperty("scale_key_frames", new HexValue(scaleKeyFrames));
            List<HexValue> offsets = new ArrayList<>();
            for (int o = 0; o < 13; o++) {
                offsets.add(new HexValue(stream.readInt()+chunk.getStart()));
            }
            entry.addProperty("offsets", offsets);
            long tempOffset = stream.getStreamPosition();
            List<Integer> translationTimes = readList(stream, translationKeyFrames, offsets.get(0).getValue(), BattleMeshRead::readShort);
            List<Integer> rotationTimes = readList(stream, rotationKeyFrames, offsets.get(1).getValue(), BattleMeshRead::readShort);
            List<Integer> scaleTimes = readList(stream, scaleKeyFrames, offsets.get(2).getValue(), BattleMeshRead::readShort);

            List<Fixed> translationX = readList(stream, translationKeyFrames, offsets.get(3).getValue(), BattleMeshRead::readSglFixed);
            List<Fixed> translationY = readList(stream, translationKeyFrames, offsets.get(4).getValue(), BattleMeshRead::readSglFixed);
            List<Fixed> translationZ = readList(stream, translationKeyFrames, offsets.get(5).getValue(), BattleMeshRead::readSglFixed);

            List<Fixed> rotationX = readList(stream, rotationKeyFrames, offsets.get(6).getValue(), BattleMeshRead::readHalfSglFixed);
            List<Fixed> rotationY = readList(stream, rotationKeyFrames, offsets.get(7).getValue(), BattleMeshRead::readHalfSglFixed);
            List<Fixed> rotationZ = readList(stream, rotationKeyFrames, offsets.get(8).getValue(), BattleMeshRead::readHalfSglFixed);
            List<Fixed> rotationTheta = readList(stream, rotationKeyFrames, offsets.get(9).getValue(), BattleMeshRead::readHalfSglFixed);

            List<Fixed> scaleX = readList(stream, scaleKeyFrames, offsets.get(10).getValue(), BattleMeshRead::readSglFixed);
            List<Fixed> scaleY = readList(stream, scaleKeyFrames, offsets.get(11).getValue(), BattleMeshRead::readSglFixed);
            List<Fixed> scaleZ = readList(stream, scaleKeyFrames, offsets.get(12).getValue(), BattleMeshRead::readSglFixed);

            entry.addProperty("translation", zip(translationTimes, zipPoints(translationX, translationY, translationZ)));
            entry.addProperty("rotation", zip(rotationTimes, zipRotations(rotationX, rotationY, rotationZ, rotationTheta)));
            entry.addProperty("scale", zip(scaleTimes, zipPoints(scaleX, scaleY, scaleZ)));

            stream.seek(tempOffset);
            i++;
        }
        header.addProperty("dictionary_end", new HexValue((int) stream.getStreamPosition()));
        return chunk;
    }

    public static <E> Map<Integer, E> zip(List<Integer> times, List<E> values) {
        Map<Integer, E> data = new TreeMap<>();
        //noinspection UnstableApiUsage
        Streams.forEachPair(times.stream(), values.stream(), data::put);
        return data;
    }

    public static List<Point> zipPoints(List<Fixed> x, List<Fixed> y, List<Fixed> z) {
        List<Point> points = new ArrayList<>();
        int length = Math.min(x.size(), Math.min(y.size(), z.size()));
        for (int i = 0; i < length; i++) {
            points.add(new Point(x.get(i), y.get(i), z.get(i)));
        }
        return points;
    }

    public static List<Quarternion> zipRotations(List<Fixed> x, List<Fixed> y, List<Fixed> z, List<Fixed> theta) {
        List<Quarternion> points = new ArrayList<>();
        int length = Math.min(x.size(), Math.min(y.size(), Math.min(z.size(), theta.size())));
        for (int i = 0; i < length; i++) {
            points.add(new Quarternion(x.get(i), y.get(i), z.get(i), theta.get(i)));
        }
        return points;
    }

    public static Angle readAngle(ImageInputStream stream) {
        try {
            return new Angle(stream.readShort());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Fixed readHalfSglFixed(ImageInputStream stream) {
        try {
            return new Fixed(stream.readShort());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Fixed readSglFixed(ImageInputStream stream) {
        try {
            return new Fixed(stream.readInt());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int readShort(ImageInputStream stream) {
        try {
            return stream.readShort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <E> List<E> readList(ImageInputStream stream, int size, int offset, Function<ImageInputStream, E> readFunction) throws IOException {
        List<E> result = new ArrayList<>();
        stream.seek(offset);
        for (int i = 0; i < size; i++) {
            result.add(readFunction.apply(stream));
        }
        return result;
    }


    private static Block readMeshChunk(ImageInputStream stream, Block chunkOffsets) throws IOException {
        Block chunk = new Block("meshes", chunkOffsets.getInt("offset"), chunkOffsets.getInt("data_size"));
        stream.seek(chunk.getStart());
        chunk.addProperty("chunk_offset", new HexValue(chunk.getStart()));
        chunk.addProperty("chunk_length", new HexValue(chunk.getLength()));

        // always 8
        chunk.addProperty("header_size", new HexValue(stream.readInt()));
        // some offsets,counts. always < chunk_length
        chunk.addProperty("skeletonOffset", new HexValue(stream.readInt()));
        chunk.addProperty("meshOffset", new HexValue(stream.readInt()));
        // weapon mesh?
        chunk.addProperty("weaponMeshOffset", new HexValue(stream.readInt()));

        int numMeshes = (chunk.getInt("weaponMeshOffset") - chunk.getInt("meshOffset")) / PolygonDataExtended.SIZE;
        chunk.addProperty("numMeshes", numMeshes);

        chunk.addProperty("header_padding", new HexValue(stream.readInt()));
        chunk.addProperty("offset_after_header", new HexValue((int) stream.getStreamPosition()));

        byte[] data = new byte[chunk.getInt("meshOffset")-(chunk.getInt("offset_after_header")-chunk.getStart())];
        stream.read(data);

        // read meshes
        stream.seek(chunk.getStart() + chunk.getInt("meshOffset"));
        Block bodyMeshes = chunk.createBlock("body_meshes", (int) stream.getStreamPosition(), 0);
        for (int i = 0; i < numMeshes; i++) {
            PolygonDataExtended polygonData = new PolygonDataExtended(stream, "mesh[" + i + "]");
            polygonData.readDetails(data, chunk.getInt("offset_after_header") - chunk.getStart());
            bodyMeshes.addBlock(polygonData);
        }

        // read a single mesh definition (padded with 4 bytes)

        int weaponMeshOffset = chunk.getInt("weaponMeshOffset");
        if (weaponMeshOffset > 0 ) {
            stream.seek(chunk.getStart() + weaponMeshOffset);
            PolygonDataExtended weaponMesh = new PolygonDataExtended(stream, "weaponMesh");
            weaponMesh.readDetails(data, chunk.getInt("offset_after_header") - chunk.getStart());
            chunk.addBlock(weaponMesh);
        }
        chunk.addProperty("offset_pdata", new HexValue((int) stream.getStreamPosition()));

        stream.seek(chunk.getStart() + chunk.getInt("skeletonOffset"));
        int length = chunk.getLength() - chunk.getInt("skeletonOffset");
        List<HexValue> skeletonRawStream = new ArrayList<>();
        for (int i = 0; i < length; i+= 4) {
            int value = stream.readInt();
            skeletonRawStream.add(new HexValue(value));
        }
        chunk.addProperty("skeleton_raw_stream", skeletonRawStream);
        chunk.addProperty("skeleton", readSkeleton(stream, chunk, length));

        return chunk;
    }

    private static Bone readSkeleton(ImageInputStream stream, Block chunk, int length) throws IOException {
        // see SGL Programmer?s Tutorial Chapter 5 Matrices
        stream.seek(chunk.getStart() + chunk.getInt("skeletonOffset"));
        byte[] skeletonCommands = new byte[length];
        stream.readFully(skeletonCommands);

        ByteArrayImageInputStream skeletonStream = new ByteArrayImageInputStream(skeletonCommands);
        skeletonStream.setByteOrder(stream.getByteOrder());
        int boneIndex = 0;
        int meshIndex = 0;
        Bone rootBone = new Bone(-1);
        Stack<Bone> bonestack = new Stack<>();
        bonestack.push(rootBone);
        while (skeletonStream.getStreamPosition() < length) {
            int command = skeletonStream.read();
            switch (command) {
                case 0xfd: {
                    // push matrix, read next animation slot
                    Bone bone = new Bone(boneIndex++);
                    bonestack.peek().addBone(bone);
                    bonestack.push(bone);
                    break;
                }
                case 0xfe:
                    // pop matrix
                    bonestack.pop();
                    if (bonestack.isEmpty()) {
                        return rootBone;
                    }
                    break;
                case 0:
                    // add mesh
                    bonestack.peek().addMesh(meshIndex++);
                    break;
                case 9:
                case 0x10:
                case 0x11:
                case 0x20: {
                    // known tags to read translation
                    // first seek to int32 boundary
                    skeletonStream.seek(((skeletonStream.getStreamPosition() + 3) / 4) * 4);
                    Tag tag = new Tag(command);
                    tag.translation = new Point(skeletonStream);
                    bonestack.peek().addTag(tag);
                    break;
                }
                case 0x30: {
                    // known tags to read translation, rotation and scale
                    // first seek to int32 boundary
                    skeletonStream.seek(((skeletonStream.getStreamPosition() + 3) / 4) * 4);
                    Tag tag = new Tag(command);
                    tag.translation = new Point(skeletonStream);
                    tag.rotation = new Quarternion(readSglFixed(skeletonStream), readSglFixed(skeletonStream), readSglFixed(skeletonStream), readSglFixed(skeletonStream));
                    tag.scale = new Point(skeletonStream);
                    bonestack.peek().addTag(tag);
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown skeleton command 0x"+Integer.toHexString(command));
            }
        }
        return rootBone;
    }

    private static class Bone {
        final int index;
        List<Bone> childs = new ArrayList<>();
        List<Integer> meshes = new ArrayList<>();
        List<Tag> tags = new ArrayList<>();

        public Bone(int index) {
            this.index = index;
        }

        public void addBone(Bone bone) {
            this.childs.add(bone);
        }

        public void addMesh(int meshIndex) {
            this.meshes.add(meshIndex);
        }

        public void addTag(Tag tag) {
            tags.add(tag);
        }

        @Override
        public String toString() {
            return "{" +
                    "index: " + index +
                    (meshes.size() > 0 ? ", meshes: " + meshes : "") +
                    (tags.size() > 0 ? ", tags: " + tags : "") +
                    (childs.size() > 0 ? ", childs: " + childs : "") +
                    '}';
        }
    }

    private static class Tag {
        final int type;
        Point translation;
        Quarternion rotation;
        Point scale;

        public Tag(int type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "{" +
                    "type: " + type +
                    ", translation: " + translation +
                    ", rotation: " + rotation +
                    ", scale: " + scale +
                    '}';
        }
    }

    /** Read Texture Images. */
    private static Block readTextures(ImageInputStream stream, Block chunkOffsets, Block textureDefinition, String filename) throws IOException {
        Block chunk = new Block("textures", chunkOffsets.getInt("offset"), chunkOffsets.getInt("data_size"));
        chunk.addProperty("chunk_offset", new HexValue(chunk.getStart()));
        chunk.addProperty("chunk_length", new HexValue(chunk.getLength()));

        stream.seek(chunk.getStart());
        DecompressedStream decompressedStream = new DecompressedStream(stream);
        ImageInputStream imageStream = decompressedStream.toStream();
        List<Texture> textureDefs = textureDefinition.getObject("defs");

        // read textures
        List<BufferedImage> images = new ArrayList<>();
        for (Texture texture : textureDefs) {
            imageStream.seek(texture.getVramAddress());
            images.add(Sf3Util.readBufferedImage(imageStream, texture.getWidth(), texture.getHeight()));
        }
        chunk.addProperty("_images", images);

        chunk.addProperty("textureFileName", filename+".block0.textures.png");
        return chunk;
    }

    /** Read Chunk with texture structures. */
    private static Block readTexturesDefinition(ImageInputStream stream, Block chunkOffsets) throws IOException {
        Block chunk = new Block("textureDefinitions", chunkOffsets.getInt("offset"), chunkOffsets.getInt("chunk_size"));
        chunk.addProperty("header_size", new HexValue(stream.readInt()));
        int count = stream.readInt();
        chunk.addProperty("count", count);
        // Note:
        // - maybe this is some kind of memory usage hint
        // - or this is the address the textures are placed in VRAM
        // - or this is the needed texture size in vram
        chunk.addProperty("vram_size?", new HexValue(stream.readInt()));
        chunk.addProperty("header_padding", Arrays.asList(stream.readInt(),stream.readInt()));

        List<Texture> textureDefs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            textureDefs.add(new Texture(stream, "defs"));
        }
        chunk.addProperty("defs", textureDefs);

        int padding = chunkOffsets.getInt("chunk_size") - chunkOffsets.getInt("data_size");
        chunk.addBlock(readPadding(stream, padding));
        return chunk;
    }

    private static Block readHeader(ImageInputStream stream) throws IOException {
        Block header = new Block("header", 0, 0x800);
        header.addBlock(readHeaderOffset(stream, "texturesDefinitions"));
        header.addBlock(readHeaderOffset(stream, "textures"));
        header.addBlock(readHeaderOffset(stream, "meshChunk"));
        header.addBlock(readHeaderOffset(stream, "offset[1]"));
        header.addBlock(readPadding(stream, 0x800 - (4 * 16)));
        return header;
    }

    private static Block readPadding(ImageInputStream stream, int length) throws IOException {
        stream.skipBytes(length);
        return new Block("padding", (int) stream.getStreamPosition(), length);
    }

    private static Block readHeaderOffset(ImageInputStream stream, String name) throws IOException {
        Block offset = new Block(name, (int) stream.getStreamPosition(), 4*4);
        offset.addProperty("offset", new HexValue(stream.readInt()));
        offset.addProperty("data_size", stream.readInt());
        offset.addProperty("chunk_size", new HexValue(stream.readInt()));
        offset.addProperty("padding", new HexValue(stream.readInt()));
        return offset;
    }
}
