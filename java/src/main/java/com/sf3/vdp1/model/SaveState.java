package com.sf3.vdp1.model;

import com.sf3.util.ByteArrayImageInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** A save state. */
public class SaveState {
    private final VDP1 vdp1;
    private final VDP2 vdp2;
    private final Map<Integer, Command> commands;
    private final Map<Integer, Texture> textures;

    public SaveState(VDP1 vdp1, VDP2 vdp2) {
        this.vdp1 = vdp1;
        this.vdp2 = vdp2;
        this.commands = readCommands();
        this.textures = getTextures(this.commands);
        System.out.println("read "+commands.size()+" commands, "+textures.size()+" unique textures.");
    }

    private SortedMap<Integer, Texture> getTextures(Map<Integer, Command> commands) {
        SortedMap<Integer, Texture> textures = new TreeMap<>();
        for (Command command : commands.values()) {
            if (command.getCommandSelect().isSprite()) {
                int textureAddress = command.getCharacterAddress();
                if (!textures.containsKey(textureAddress)) {
                    Point textureSize = command.getCharacterSize();
                    ColorMode colorMode = command.getColorMode();
                    int colorControl = command.getColorControl();
                    Texture texture = new Texture(textureSize.getX(), textureSize.getY(), colorMode, textureAddress, colorControl);
                    textures.put(textureAddress, texture);
                }
            }
        }
        return textures;
    }

    private Map<Integer, Command> readCommands() {
        SortedMap<Integer, Command> commands = new TreeMap<>();

        try {
            ImageInputStream vramStream = new ByteArrayImageInputStream(vdp1.getVram().getContent());
            vramStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            int currentOffset = 0;
            int returnOffset = -1;

            while (true) {
                vramStream.seek(currentOffset);
                Command command = new Command(vramStream);
                if (!commands.containsKey(currentOffset)) {
                    commands.put(currentOffset, command);
                }
                if (command.isEndBit()) {
                    break;
                }
                switch (command.getJumpSelect()) {
                    case JUMP_NEXT:
                    case SKIP_NEXT:
                        currentOffset = currentOffset + 0x20;
                        break;
                    case JUMP_ASSIGN:
                    case SKIP_ASSIGN:
                        currentOffset = command.getCommandlink();
                        break;
                    case JUMP_CALL:
                    case SKIP_CALL:
                        if (returnOffset > 0) {
                            throw new IllegalStateException("nested Subroutine call. First call in 0x"+Integer.toHexString(returnOffset)+", this call in 0x"+Integer.toHexString(currentOffset));
                        }
                        returnOffset = currentOffset;
                        currentOffset = command.getCommandlink();
                        break;
                    case JUMP_RETURN:
                    case SKIP_RETURN:
                        if (returnOffset < 0) {
                            throw new IllegalStateException("return without call. command 0x"+Integer.toHexString(currentOffset));
                        }
                        currentOffset = returnOffset+0x20;
                        returnOffset = -1;
                        break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return commands;
    }

    public VDP1 getVdp1() {
        return vdp1;
    }

    public VDP2 getVdp2() {
        return vdp2;
    }

    public Map<Integer, Command> getCommands() {
        return commands;
    }

    public Map<Integer, Texture> getTextures() {
        return textures;
    }

    private Command getCommand(VDP1 vdp1, int offset) throws IOException {
        ImageInputStream vramStream = new ByteArrayImageInputStream(vdp1.getVram().getContent());
        vramStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        vramStream.seek(offset);
        return new Command(vramStream);
    }

}
