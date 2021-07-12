package com.sf3.vdp1.model;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Command {

    private final boolean endBit;
    private final JumpSelect jumpSelect;
    private final ZoomPoint zoomPoint;
    private final CharacterReadDirection characterReadDirection;
    private final CommandSelect commandSelect;
    private final int commandlink;
    private final int colorControl;
    private final int characterAddress;
    private final Point characterSize;
    private final List<Point> vertexCoordinates;
    private final int gouraudShadingTable;
    private final boolean msbOn;
    private final boolean highSpeedShrink;
    private final boolean preClippingDisable;
    private final boolean clippingEnable;
    private final boolean insideClipping;
    private final boolean meshEnable;
    private final boolean endCodeDisable;
    private final boolean transparentPixelDisable;
    private final ColorMode colorMode;
    private final int colorCalculation;
    private final int drawmode;

    public Command(ImageInputStream stream) throws IOException {
        int controlword = stream.readUnsignedShort();

        this.endBit = (controlword >> 15) > 0;
        this.jumpSelect = JumpSelect.of((controlword >> 12) & 3);
        this.zoomPoint = ZoomPoint.of((controlword >> 8) & 0xF);
        this.characterReadDirection = CharacterReadDirection.of((controlword >> 4) & 3);
        this.commandSelect = CommandSelect.of(controlword & 0xF);
        this.commandlink = stream.readUnsignedShort() << 3;

        this.drawmode = stream.readUnsignedShort();
        this.msbOn = (drawmode >> 15) > 0;
        this.highSpeedShrink = ((drawmode >> 12) & 1) > 0;
        this.preClippingDisable = ((drawmode >> 11) & 1) > 0;
        this.clippingEnable = ((drawmode >> 10) & 1) > 0;
        this.insideClipping = ((drawmode >> 9) & 1) > 0;
        this.meshEnable = ((drawmode >> 8) & 1) > 0;
        this.endCodeDisable = ((drawmode >> 7) & 1) > 0;
        this.transparentPixelDisable = ((drawmode >> 6) & 1) > 0;
        this.colorMode = ColorMode.of((drawmode >> 3) & 7);
        this.colorCalculation = drawmode & 0xf;


        this.colorControl = stream.readUnsignedShort();
        this.characterAddress = stream.readUnsignedShort() << 3;
        int characterSizeWord = stream.readUnsignedShort();
        this.characterSize = new Point(((characterSizeWord >> 8) & 0x3f) * 8, characterSizeWord & 0xff);

        this.vertexCoordinates = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            vertexCoordinates.add(new Point(stream.readShort(), stream.readShort()));
        }

        this.gouraudShadingTable = stream.readUnsignedShort();
        // skip word
        stream.readUnsignedShort();

    }

    public boolean isEndBit() {
        return endBit;
    }

    public JumpSelect getJumpSelect() {
        return jumpSelect;
    }

    public ZoomPoint getZoomPoint() {
        return zoomPoint;
    }

    public CharacterReadDirection getCharacterReadDirection() {
        return characterReadDirection;
    }

    public CommandSelect getCommandSelect() {
        return commandSelect;
    }

    public int getCommandlink() {
        return commandlink;
    }

    public int getColorControl() {
        return colorControl;
    }

    public int getCharacterAddress() {
        return characterAddress;
    }

    public Point getCharacterSize() {
        return characterSize;
    }

    public List<Point> getVertexCoordinates() {
        return vertexCoordinates;
    }

    public int getGouraudShadingTable() {
        return gouraudShadingTable;
    }

    public boolean isMsbOn() {
        return msbOn;
    }

    public boolean isHighSpeedShrink() {
        return highSpeedShrink;
    }

    public boolean isPreClippingDisable() {
        return preClippingDisable;
    }

    public boolean isClippingEnable() {
        return clippingEnable;
    }

    public boolean isInsideClipping() {
        return insideClipping;
    }

    public boolean isMeshEnable() {
        return meshEnable;
    }

    public boolean isEndCodeDisable() {
        return endCodeDisable;
    }

    public boolean isTransparentPixelDisable() {
        return transparentPixelDisable;
    }

    public ColorMode getColorMode() {
        return colorMode;
    }

    public int getColorCalculation() {
        return colorCalculation;
    }

    @Override
    public String toString() {
        return "Command{" +
                "endBit=" + endBit +
                "\n jumpSelect=" + jumpSelect +
                ", commandlink=0x" + Integer.toHexString(commandlink) +
                "\n zoomPoint=" + zoomPoint +
                "\n commandSelect=" + commandSelect +
                "\n colorControl=" + colorControl +
                "\n characterAddress=0x" + Integer.toHexString(characterAddress)
                + " (0x"+Integer.toHexString(characterAddress*8)+")"+
                ", characterSize=" + characterSize +
                ", characterReadDirection=" + characterReadDirection +
                "\n vertexCoordinates=" + vertexCoordinates +
                "\n gouraudShadingTable=" + gouraudShadingTable +
                "\n drawMode "+Integer.toBinaryString(this.drawmode)+" {" +
                "colorMode=" + colorMode +
                ", colorCalculation=" + colorCalculation +
                ", msbOn=" + msbOn +
                ", highSpeedShrink=" + highSpeedShrink +
                ", preClippingDisable=" + preClippingDisable +
                ", clippingEnable=" + clippingEnable +
                ", insideClipping=" + insideClipping +
                ", meshEnable=" + meshEnable +
                ", endCodeDisable=" + endCodeDisable +
                ", transparentPixelDisable=" + transparentPixelDisable +
                "}" +
                "\n}";
    }

}
