package com.sf3.util;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;
import com.sf3.binaryview.FindTextureModel;

import javax.swing.*;
import java.awt.*;
import java.nio.ByteOrder;
import java.util.function.Consumer;

public class StatusBar extends JPanel {
    private final JLabel mousePosLabel;
    private final JLabel textureSizeLabel;
    private final JLabel byteOrderLabel;
    private final JLabel offsetLabel;

    public StatusBar(PresentationModel<?> pm) {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        this.mousePosLabel = new JLabel("Mouse Pos: ");
        this.textureSizeLabel = new JLabel("Texture Size: ");
        this.byteOrderLabel = new JLabel("ByteOrder: ");
        this.offsetLabel = new JLabel("Offset: ");

        add(mousePosLabel);
        add(new JLabel("|"));
        add(textureSizeLabel);
        add(new JLabel("|"));
        add(byteOrderLabel);
        add(new JLabel("|"));
        add(offsetLabel);


        pm.getModel(FindTextureModel.MOUSE_POS)
                .addValueChangeListener(evt -> updateMousePos(pm, evt));

        initLabel(pm, FindTextureModel.TEXTURE_WIDTH, this::updateTextureWidth);
        initLabel(pm, FindTextureModel.BYTE_ORDER, this::updateByteOrder);
        initLabel(pm, FindTextureModel.FILE_OFFSET, this::updateOffset);
    }

    @SuppressWarnings("unchecked")
    private <E> void initLabel(PresentationModel<?> pm, String property, Consumer<E> updateFunction) {
        ValueModel model = pm.getModel(property);
        model.addValueChangeListener(evt -> updateFunction.accept((E) evt.getNewValue()));
        updateFunction.accept((E) model.getValue());
    }

    private void updateOffset(Integer offset) {
        offsetLabel.setText("Offset: 0x"+Integer.toHexString(offset));
    }

    private void updateByteOrder(ByteOrder byteOrder) {
        byteOrderLabel.setText("ByteOrder: "+byteOrder.toString());
    }

    private void updateTextureWidth(Integer textureSize) {
        textureSizeLabel.setText("Texture Size: "+textureSize);
    }

    private void updateMousePos(PresentationModel<?> pm, java.beans.PropertyChangeEvent evt) {
        ValueModel textureWidthModel = pm.getModel(FindTextureModel.TEXTURE_WIDTH);
        ValueModel imageSizeModel = pm.getModel(FindTextureModel.IMAGE_PANEL_SIZE);
        ValueModel scaleModel = pm.getModel(FindTextureModel.SCALE);
        ValueModel offsetModel = pm.getModel(FindTextureModel.FILE_OFFSET);
        Point value = (Point) evt.getNewValue();
        int textureWidth = (int) textureWidthModel.getValue();
        Dimension imageSize = (Dimension) imageSizeModel.getValue();
        int scale = (int) scaleModel.getValue();
        // number of pixels in one column
        int columnSize = textureWidth * (imageSize.height / scale);
        // which column is the mouse pointer?
        int column = value.x / textureWidth;
        int row = value.y;
        int col = value.x - (column * textureWidth);
        // index in the image
        int imageIndex = column * columnSize + row * textureWidth + col;
        // each pixel represents 2 bytes.
        int byteIndex = imageIndex * 2;
        // at last add the current offset
        int fileIndex = byteIndex + (Integer) offsetModel.getValue();

        mousePosLabel.setText("Mouse Pos: " + value.x+", "+value.y+" (0x"+Integer.toHexString(fileIndex)+", "+fileIndex+")");
    }
}
