package com.sf3.gamedata.background;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.sf3.gamedata.mpd.DecompressedStream;
import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.util.Sf3Util;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Convert various Shining Force 3 image formats to png. */
@Slf4j
public class ImageConvert {

    @Parameters(commandDescription = "Converts Shining Force 3 images to png.")
    private static class ToPngCommand {
        @Parameter(description = "input.spr  output.png")
        private List<String> files = new ArrayList<>();

        @Parameter(names = {"-w", "--width"}, description = "width of image")
        private Integer width = 1;

        @Parameter(names = {"-h", "--height"}, description = "height of image")
        private Integer height = -1;

        @Parameter(names = {"-p", "--palette"}, description = "when set the image has a palette")
        private boolean usePalette = false;

        @Parameter(names = {"-c", "--compressed"}, description = "when set the image is rle compressed")
        private boolean compressed = false;
    }

    @Parameters(commandDescription = "Converts png images to Shining Force 3 images.")
    private static class ToSingleCommand {
        @Parameter(description = "input.png  output.ext")
        private List<String> files = new ArrayList<>();

        @Parameter(names = {"-p", "--palette"}, description = "when set the image has a palette")
        private boolean usePalette = false;

        @Parameter(names = {"-c", "--compressed"}, description = "when set the image is rle compressed")
        private boolean compressed = false;
    }

    private int[] palette;
    private int pixelSize = 2;
    private Function<ImageInputStream, Integer> readPixelFunction = this::getRgb16Pixel;

    private BufferedImage readImage(ImageInputStream stream, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x,y,readPixelFunction.apply(stream));
            }
        }
        return image;
    }

    private int getRgb16Pixel(ImageInputStream stream) {
        int value = Sf3Util.readUnsignedShort(stream);
        return Sf3Util.rgb16ToRgb24(value);
    }

    private int getPalettePixel(ImageInputStream stream) {
        int value = Sf3Util.readUnsignedByte(stream);
        return palette[value];
    }

    private int[] readPalette(ImageInputStream stream)  {
        int[] localPalette = new int[256];
        for (int i = 0; i < localPalette.length; i++) {
            localPalette[i] = getRgb16Pixel(stream);
        }
        return localPalette;
    }

    private void convertToPng(ToPngCommand command) throws IOException {
        if (command.files.size() != 2) {
            log.info("Usage: ImageConvert -w <width> [-h <height>] input.spr output.png");
            System.exit(1);
        }

        Path path = Paths.get(command.files.get(0));
        try (InputStream is = Files.newInputStream(path)) {
            ImageInputStream stream = new MemoryCacheImageInputStream(is);
            stream.setByteOrder(ByteOrder.BIG_ENDIAN);

            if (command.usePalette) {
                log.info("reading palette");
                this.palette = readPalette(stream);
                readPixelFunction = this::getPalettePixel;
                this.pixelSize = 1;
            }

            int length = (int) (Files.size(path) - stream.getStreamPosition());
            byte[] data = new byte[length];
            stream.readFully(data);
            if (command.compressed) {
                log.info("decompressing stream");
                try (ByteArrayImageInputStream compressedDataStream = new ByteArrayImageInputStream(data)) {
                    compressedDataStream.setByteOrder(stream.getByteOrder());
                    // in compressed images there is a 2 byte padding after the palette
                    if (command.usePalette) {
                        compressedDataStream.skipBytes(2);
                    }
                    data = new DecompressedStream(compressedDataStream).getResult();
                }
            }


            if (command.height < 0) {
                command.height = data.length / (command.width * this.pixelSize);
                log.info("setting height to %d (data length = %d)%n", command.height, data.length);
            }
            ByteArrayImageInputStream imageStream = new ByteArrayImageInputStream(data);
            imageStream.setByteOrder(stream.getByteOrder());
            BufferedImage image = this.readImage(imageStream, command.width, command.height);
            File output = new File(command.files.get(1));
            ImageIO.write(image, "png", output);
            log.info("write "+output);
        }
    }

    private void convertToShiningForce(ToSingleCommand command) throws IOException {
        if (command.files.size() != 2) {
            log.info("Usage: ImageConvert -w <width> [-h <height>] input.spr output.png");
            System.exit(1);
        }
        Path inFile = Paths.get(command.files.get(0));
        Path outFile = Paths.get(command.files.get(1));

        BufferedImage image = ImageIO.read(inFile.toFile());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageOutputStream outputStream = new MemoryCacheImageOutputStream(out);
        outputStream.setByteOrder(ByteOrder.BIG_ENDIAN);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                int saturnColor = Sf3Util.rgb24ToRgb16(color);
                outputStream.writeShort(saturnColor);
            }
        }
        outputStream.close();
        Files.write(outFile, out.toByteArray());
        log.info("written {}", outFile.toAbsolutePath());
    }

    public static void main(String[] args) throws IOException {
        ImageConvert imageConvert = new ImageConvert();

        ToPngCommand toPngCommand = new ToPngCommand();
        ToSingleCommand toSingleCommand = new ToSingleCommand();

        JCommander jc = JCommander.newBuilder()
                .addCommand("convertToPng", toPngCommand)
                .addCommand("convertToSf3", toSingleCommand)
                .build();
        jc.parse(args);
        switch (jc.getParsedCommand()) {
            case "convertToPng":
                imageConvert.convertToPng(toPngCommand);
                break;
            case "convertToSf3":
                imageConvert.convertToShiningForce(toSingleCommand);
                break;
            default:
                log.info("unknown command");
                System.exit(1);
        }


    }

}
