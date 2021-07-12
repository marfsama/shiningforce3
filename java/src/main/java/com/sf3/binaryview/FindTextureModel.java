package com.sf3.binaryview;

import com.jgoodies.binding.beans.Model;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteOrder;
import java.nio.file.Path;

/** Model class. */
public class FindTextureModel extends Model {
    public static final String PATH = "path";
    public static final String IMAGE = "image";
    public static final String SCALE = "scale";
    public static final String IMAGE_PANEL_SIZE = "imagePanelSize";
    public static final String TEXTURE_WIDTH = "textureWidth";
    public static final String MOUSE_POS = "mousePos";
    public static final String BYTE_ORDER = "byteOrder";
    public static final String FILE_SIZE = "fileSize";
    public static final String FILE_OFFSET = "fileOffset";


    /** Currently read file. */
    private Path path;
    /** Scale Factor. */
    private int scale = 4;
    /** the image. */
    private BufferedImage image;
    /** Size of the image Panel. */
    private Dimension imagePanelSize;
    /** Size of one column in the image. */
    private int textureWidth = 16;
    /** Position of the mouse. */
    private Point mousePos;
    /** Byte order of the stream. */
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    /** Size of current file. */
    private int fileSize;
    /** Offset in file where to start the image. */
    private int fileOffset;


    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        Path old = this.path;
        this.path = path;
        firePropertyChange(PATH, old, path);
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        int old = this.scale;
        this.scale = scale;
        firePropertyChange(SCALE, old, scale);
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        BufferedImage old = this.image;
        this.image = image;
        firePropertyChange(IMAGE, old, image);
    }

    @SuppressWarnings("unused")
    public Dimension getImagePanelSize() {
        return imagePanelSize;
    }

    @SuppressWarnings("unused")
    public void setImagePanelSize(Dimension imagePanelSize) {
        Dimension old = this.imagePanelSize;
        this.imagePanelSize = imagePanelSize;
        firePropertyChange(IMAGE_PANEL_SIZE, old, imagePanelSize);
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public void setTextureWidth(int textureWidth) {
        int old = this.textureWidth;
        this.textureWidth = textureWidth;
        firePropertyChange(TEXTURE_WIDTH, old, textureWidth);
    }

    public Point getMousePos() {
        return mousePos;
    }

    public void setMousePos(Point mousePos) {
        Point old = this.mousePos;
        this.mousePos = mousePos;
        firePropertyChange(MOUSE_POS, old, mousePos);
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        ByteOrder old = this.byteOrder;
        this.byteOrder = byteOrder;
        firePropertyChange(BYTE_ORDER, old, byteOrder);
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        System.out.println("set file offset to "+fileOffset);
        int old = this.fileSize;
        this.fileSize = fileSize;
        firePropertyChange(FILE_SIZE, old, fileSize);
    }

    public int getFileOffset() {
        return fileOffset;
    }

    public void setFileOffset(int fileOffset) {
        int old = this.fileOffset;
        this.fileOffset = fileOffset;
        firePropertyChange(FILE_OFFSET, old, fileOffset);
    }
}
