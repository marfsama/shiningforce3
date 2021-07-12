package com.sf3.vdp1;

import com.sf3.util.ByteArrayImageInputStream;
import com.sf3.vdp1.model.*;
import org.apache.commons.io.IOUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Test extends JPanel {

    private final BufferedImage image;

    public Test(BufferedImage image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(image, 0,0 , image.getWidth()*3, image.getHeight()*3,this);
    }

    @Override
    public Dimension getSize(Dimension rv) {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        return getSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return getSize();
    }

    public static void main(String[] args) throws IOException {
        String path = System.getProperty("user.home")+"/.mednafen/mcs/DRAGON_FORCE.d57a240e99a6235862c1606a58af46cf.mc1";

        File file = new File(path);

        ImageInputStream stream = getStream(file);
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        Header header = new Header(stream);

        Map<String, Chunk> chunks = readChunks(stream);

        VDP1 vdp1 = new VDP1(stream, chunks.get("VDP1"));
//        Files.write(Paths.get(System.getProperty("user.home")+"/.mednafen/mcs/vram.mc9"), vdp1.getVram().getContent());


        VDP2 vdp2 = new VDP2(stream, chunks.get("VDP2"));
        ColorRam colorRam = vdp2.getColorRam();
        System.out.println("Color RAM Mode: "+colorRam.getType());
//        Files.write(Paths.get(System.getProperty("user.home")+"/.mednafen/mcs/DRAGON_FORCE.d57a240e99a6235862c1606a58af46cf.cram.mc1"), colorRam.getContent());

        JFrame frame = new JFrame("test");
        frame.setSize(1800, 1000);
        JTabbedPane tabbedPane = new JTabbedPane();
        frame.add(tabbedPane);

        ColorRamPanel colorRamPanel = new ColorRamPanel();
        colorRamPanel.setColorRam(colorRam);
        tabbedPane.add("ColorRam", colorRamPanel);

        int offset = 0;
        int count = 0;
        Command lastCommand;
        do {
            count++;
            Command command = getCommand(vdp1, offset);
            lastCommand = command;
            System.out.println(command);
            switch (command.getJumpSelect()) {
                case JUMP_ASSIGN:
                    offset = command.getCommandlink();
                    break;
                case JUMP_NEXT:
                    offset += 0x20;
                    break;
                default:
                    System.out.println("jump mode "+command.getJumpSelect()+": aborted");
                    count = 5000;
                    break;
            }
            if (command.getCommandSelect().ordinal() <= 2) {
                CommandTexturePanel commandTexturePanel = new CommandTexturePanel();
                commandTexturePanel.setColorRam(colorRam);
                commandTexturePanel.setCommand(lastCommand);
                commandTexturePanel.setVram(vdp1.getVram().getContent());
                tabbedPane.add("Command "+count, commandTexturePanel);
            }

        } while (count < 30);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        stream.close();
/*
        JFrame frame = new JFrame("test");
        frame.setSize(1800, 1000);
        frame.add(new JScrollPane(new Test(image)));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

 */
    }

    private static ImageInputStream getStream(File file) throws IOException {
        // MemoryCacheImageInputStream
        InputStream stream = new FileInputStream(file);
        int b1 = stream.read();
        int b2 = stream.read();
        stream.close();
        if (b1 == 0x1f && b2 == 0x8b) {
            // gzipped Stream. deflate whole stream to memory
            try(GZIPInputStream input = new GZIPInputStream(new FileInputStream(file))) {
                byte[] content = IOUtils.toByteArray(input);
                return new ByteArrayImageInputStream(content);
            }
        }
        // not gzipped
        return new FileImageInputStream(file);
    }

    private static Command getCommand(VDP1 vdp1, int offset) throws IOException {
        ImageInputStream vramStream = new ByteArrayImageInputStream(vdp1.getVram().getContent());
        vramStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        vramStream.seek(offset);
        return new Command(vramStream);
    }

    private static Map<String, Chunk> readChunks(ImageInputStream stream) throws IOException {
        Map<String, Chunk> chunks = new HashMap<>();
        while (stream.getStreamPosition() < stream.length()) {
            Chunk chunk = new Chunk(stream);
            chunks.put(chunk.getMagic(), chunk);
        }
        return chunks;
    }

    private static void saveChunk(Chunk chunk, File file) throws IOException {
        Files.write(file.toPath(), chunk.getContent());
    }
}
