package com.sf3.gamedata.mpd;

import com.sf3.gamedata.sgl.Fixed;
import com.sf3.gamedata.sgl.Point;
import com.sf3.util.Sf3Util;

import javax.imageio.stream.ImageInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Surface which is *not* modeled as RBG (rotating background). */
public class Surface {
    private int width = 16;
    private int height = 16;
    /** List of 16x16 Tiles with character texture ids. Each tile is 4x4. */
    private List<Tile<Integer>> characters = new ArrayList<>();
    /** List of 16x16 Tiles with character texture ids. Each tile is 5x5. */
    private List<Tile<Point>> vertexNormals = new ArrayList<>();
    /** List of 16x16 Tiles with an unknown byte. Each tile is 5x5. */
    private List<Tile<Integer>> walkmeshHeights = new ArrayList<>();

    public Surface(ImageInputStream stream) {
        for (int i = 0; i < 16*16; i++) {
            characters.add(readTile(stream, 4, 4, Sf3Util::readUnsignedShort));
        }
        for (int i = 0; i < 16*16; i++) {
            vertexNormals.add(readTile(stream, 5, 5, this::readNormal));
        }
        for (int i = 0; i < 16*16; i++) {
            walkmeshHeights.add(readTile(stream, 5, 5, Sf3Util::readUnsignedByte));
        }
    }

    public Point readNormal(ImageInputStream stream) {
        Fixed a = Sf3Util.readCompressedSglFixed(stream);
        Fixed b = Sf3Util.readCompressedSglFixed(stream);
        Fixed c = Sf3Util.readCompressedSglFixed(stream);
        return new Point(a, b, c);
    }

    private <E> Tile<E> readTile(ImageInputStream stream, int width, int height, Function<ImageInputStream, E> readFunction) {
        return new Tile<E>(width, height, readFunction, stream) {
            @Override
            public int xyToIndex(int x, int y) {
                return width * y + x;
            }
        };
    }

    public Tile<Integer> getCharacterTile(int x, int y) {
        return characters.get(width * y + x);
    }

    public Integer getCharacter(int x, int y) {
        return getCharacterTile(x / 4, y / 4).get(x % 4, y % 4);
    }

    public Tile<Integer> getWalkmeshTile(int x, int y) {
        return walkmeshHeights.get(width * y + x);
    }

    public Integer getWalkmeshHeight(int x, int y) {
        return getWalkmeshTile(x / 5, y / 5).get(x % 5, y % 5);
    }

    public Tile<Point> getVertexNormalTile(int x, int y) {
        return vertexNormals.get(width * y + x);
    }

    public Point getVertexNormal(int x, int y) {
        return getVertexNormalTile(x / 5, y / 5).get(x % 5, y % 5);
    }

    @Override
    public String toString() {
        List<String> characters = new ArrayList<>();
        for (int y = 0; y < height*4; y++) {
            List<Integer> characterLine = new ArrayList<>();
            for (int x = 0; x < width*4; x++) {
                characterLine.add(getCharacter(x,y));
            }
            characters.add("\""+characterLine+"\"");
        }
        // skip surface normals
        List<String> vertexNormals = new ArrayList<>();
        for (int y = 0; y < height*5; y++) {
            List<String> vertexNormalLine = new ArrayList<>();
            for (int x = 0; x < width*5; x++) {
                Point vertexNormal = getVertexNormal(x, y);
                vertexNormalLine.add("["+vertexNormal.getX()+","+vertexNormal.getY()+","+vertexNormal.getZ()+"]");
            }
            vertexNormals.add("\""+vertexNormalLine+"\"");
        }
        // dump unknown stuff
        List<String> walkmeshHeights = new ArrayList<>();
        for (int y = 0; y < height*5; y++) {
            List<Integer> walkmeshLine = new ArrayList<>();
            for (int x = 0; x < width*5; x++) {
                walkmeshLine.add(getWalkmeshHeight(x,y));
            }
            walkmeshHeights.add("\""+walkmeshLine+"\"");
        }
        return "{\"characters\": " +
                "["+String.join(", ", characters)+"]," +
                "\"vertex_normals\":"+
                "["+String.join(", ", vertexNormals)+"]," +
                "\"walkmesh_heights\":"+
                "["+String.join(", ", walkmeshHeights)+"]}";
    }
}
