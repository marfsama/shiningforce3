package com.sf3.vdp1;

import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.vdp1.model.ColorRam;
import com.sf3.vdp1.model.Command;
import com.sf3.vdp1.model.Point;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.ByteOrder;

/** Shows the command*/
public class CommandTexturePanel extends JPanel {

    private ColorRam colorRam;
    private byte[] vram;
    private Command command;

    public ColorRam getColorRam() {
        return colorRam;
    }

    public byte[] getVram() {
        return vram;
    }

    public void setVram(byte[] vram) {
        this.vram = vram;
    }

    public void setColorRam(ColorRam colorRam) {
        this.colorRam = colorRam;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Rectangle rect = graphics.getClipBounds();
        graphics.clearRect(0,0, rect.width, rect.height);
        if (colorRam == null || command == null) {
            return;
        }

        int tileWith = 20;
        Point size = command.getCharacterSize();

        ByteArrayImageInputStream stream = new ByteArrayImageInputStream(vram);
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        try {
            stream.seek(command.getCharacterAddress());
            for (int y = 0; y < size.getY();y++) {
                for (int x = 0; x < size.getX();x++) {
                    int word = stream.readUnsignedShort();
                    int r = (word & 0x1f) << 3;
                    int g = ((word >> 5) & 0x1f) << 3;
                    int b = ((word >> 10) & 0x1f) << 3;
                    int color = (r << 16) + (g << 8) + b;

                    graphics.setColor(new Color(color));
                    graphics.fillRect(x*tileWith, y*tileWith, tileWith, tileWith);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
/*
        for (int y = 0; y < size.getY();y++) {
            for (int x = 0; x < size.getX();x++) {
                int color = getColor(command, vram, x,y);
                g.setColor(new Color(color));
                g.fillRect(x*tileWith, y*tileWith, tileWith, tileWith);
            }
        }
*/
    }

    private int getColor(Command command, byte[] vram, int x, int y) {
        int characterOffset = command.getCharacterAddress();
        int offset = y * command.getCharacterSize().getX() + x;
        int colorOffset = (characterOffset << 3) + (offset << 1);

        int word = ((vram[colorOffset] & 0xff) << 8) + (vram[colorOffset] & 0xff);
        int r = (word & 0x1f) << 3;
        int g = ((word >> 5) & 0x1f) << 3;
        int b = ((word >> 10) & 0x1f) << 3;
        return (r << 16) + (g << 8) + b;

    }
}
