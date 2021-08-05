package com.sf3.util;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;
import com.sf3.binaryview.FindTextureModel;
import com.sf3.vdp1.gui.Vdp1Model;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/** (Re)loads the Image. */
public class ImageLoader {
    private final ValueModel imageModel;

    private final ValueModel imageSize;
    private final ValueModel scaleModel;
    private final ValueModel textureWidth;
    private final ValueModel byteOrder;
    private final ValueModel fileOffset;

    private final InputStreamSuppier<InputStream> inputStreamSuppier;

    public ImageLoader(PresentationModel<?> pm, InputStreamSuppier<InputStream> inputStreamSuppier) {
        this.scaleModel = pm.getModel(Vdp1Model.SCALE);
        this.imageModel = pm.getModel(Vdp1Model.IMAGE);
        this.imageSize = pm.getModel(Vdp1Model.IMAGE_PANEL_SIZE);
        this.textureWidth = pm.getModel(Vdp1Model.TEXTURE_WIDTH);
        this.byteOrder = pm.getModel(Vdp1Model.BYTE_ORDER);
        this.fileOffset = pm.getModel(FindTextureModel.FILE_OFFSET);

        imageSize.addValueChangeListener(evt -> reloadImage());
        scaleModel.addValueChangeListener(evt -> reloadImage());
        textureWidth.addValueChangeListener(evt -> reloadImage());
        byteOrder.addValueChangeListener(evt -> reloadImage());
        fileOffset.addValueChangeListener(evt -> reloadImage());

        this.inputStreamSuppier = inputStreamSuppier;
    }

    public void reloadImage() {
        Dimension imagePanelSize = (Dimension) imageSize.getValue();
        if (imagePanelSize == null
                || scaleModel.getValue() == null
                || textureWidth.getValue() == null
                || byteOrder.getValue() == null
                || fileOffset.getValue() == null) {
            return;
        }
        int imageWidth = imagePanelSize.width / (Integer) scaleModel.getValue();
        int imageHeight = imagePanelSize.height / (Integer) scaleModel.getValue();
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        int textureWidth = (int) this.textureWidth.getValue();
        int columns = imageWidth / textureWidth;
        int offset = (Integer) fileOffset.getValue();
        try (InputStream inputStream = inputStreamSuppier.getInputStream()) {
            if (inputStream == null) {
                return;
            }
            MemoryCacheImageInputStream stream = new MemoryCacheImageInputStream(inputStream);
            stream.setByteOrder((ByteOrder) byteOrder.getValue());
            stream.skipBytes(offset);
            for (int col = 0; col < columns; col++) {
                for (int y = 0; y < imageHeight; y++) {
                    for (int x = 0; x < textureWidth; x++) {
                        int word = stream.readUnsignedShort();
                        int color = Sf3Util.rgb16ToRgb24(word);

                        image.setRGB(x + col * textureWidth, y, color);
                    }
                }
            }
        }
        catch (IOException e) {
//            e.printStackTrace();
        }
        imageModel.setValue(image);
    }

    /** Functional Interface to supply an {@link InputStream}. */
    @FunctionalInterface
    public interface InputStreamSuppier<E> {
        E getInputStream() throws IOException;
    }
}
