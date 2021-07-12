package com.sf3.vdp1.gui;

import com.jgoodies.binding.value.ValueModel;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.util.Sf3Util;
import com.sf3.vdp1.model.ColorMode;
import com.sf3.vdp1.model.ColorRam;
import com.sf3.vdp1.model.SaveState;
import com.sf3.vdp1.model.Texture;

import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;

/** Shows a single Texture. */
public class TexturePanel extends JPanel {
    private final ValueModel savestate;
    private final ValueModel selectedTexture;

    private BufferedImage textureImage;

    public TexturePanel(ValueModel savestate, ValueModel selectedTexture) {
        this.savestate = savestate;
        this.selectedTexture = selectedTexture;

        this.selectedTexture.addValueChangeListener(evt -> updateTexture());
    }

    public BufferedImage getTextureImage() {
        return textureImage;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getWidth());
    }

    private void updateTexture() {
        textureImage = null;
        SaveState savestate = (SaveState) this.savestate.getValue();
        Integer selectedTexture = (Integer) this.selectedTexture.getValue();
        if (savestate == null || selectedTexture == null) {
            repaint();
            return;
        }
        Texture texture = new ArrayList<>(savestate.getTextures().values()).get(selectedTexture);
        byte[] vram = savestate.getVdp1().getVram().getContent();
        ImageInputStream stream = new ByteArrayImageInputStream(vram);
        ColorReader colorReader = new ColorReader(stream, texture.getColorMode(), texture.getColorbank(), savestate.getVdp2().getColorRam());

        if (texture.getWidth() > 0 && texture.getHeight() > 0) {
            BufferedImage image = new BufferedImage(texture.getWidth(), texture.getHeight(), BufferedImage.TYPE_INT_RGB);
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            try {
                stream.seek(texture.getOffset());
                for (int y = 0; y < texture.getHeight(); y++) {
                    for (int x = 0; x < texture.getWidth(); x++) {
                        int color = colorReader.nextColor();
                        image.setRGB(x, y, color);
                    }
                }
                this.textureImage = image;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        repaint();
    }

    private static class ColorReader {
        private final ImageInputStream stream;
        private final ColorRam colorRam;
        private final int colorBank;
        private final ColorMode colorMode;

        private int intermediateByte;
        private int intermediateByteIndex = 0;

        public ColorReader(ImageInputStream stream, ColorMode colorMode, int colorbank, ColorRam colorRam) {
            this.stream = stream;
            this.colorMode = colorMode;
            this.colorRam = colorRam;
            this.colorBank = colorbank;
            System.out.println("color Mode: "+colorMode+" ColorRam Type: "+colorRam.getType()
                    +" ("+colorRam.getNumColors()+" Colors) ColorBank: 0x"+Integer.toHexString(colorBank));
        }

        public int nextColor() throws IOException {
            if (intermediateByteIndex == 0) {
                intermediateByte = stream.readUnsignedShort();
                intermediateByteIndex = colorMode.getNumColors();
            }
            intermediateByteIndex--;
            int colorIndex = (intermediateByte >> (intermediateByteIndex*colorMode.getBitsPerColor())) & colorMode.getMask();

            switch (colorMode) {
                case LOOKUP_TABLE_4_BPP_16_COLORS:
                    long pos = stream.getStreamPosition();
                    stream.seek((colorBank << 3)+colorIndex*2);
                    int color = Sf3Util.rgb16ToRgb24(stream.readUnsignedShort());
                    stream.seek(pos);
                    return color;

                case COLOR_BANK_4_BPP_16_COLORS:
                case COLOR_BANK_8_BPP_64_COLORS:
                case COLOR_BANK_8_BPP_128_COLORS:
                case COLOR_BANK_8_BPP_256_COLORS:
                    return colorRam.getColor(colorBank, colorIndex);

                case RGB_16_BPP:
                default:
                    return Sf3Util.rgb16ToRgb24(colorIndex);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0,0,getWidth(), getHeight());
        if (textureImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            Dimension size = getSize();
            double scaley = size.getHeight() / textureImage.getHeight();
            double scalex = size.getWidth() / textureImage.getWidth();
            double scale = Math.min(scalex, scaley);
            g2d.setTransform(AffineTransform.getScaleInstance(scale, scale));
            g2d.drawImage(textureImage,0,0,this);
        }
    }
}
