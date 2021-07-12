package com.sf3.binaryview;

import com.jgoodies.binding.PresentationModel;
import com.sf3.util.ImageLoader;
import com.sf3.util.ImagePanel;
import com.sf3.util.SettingsPanel;
import com.sf3.util.StatusBar;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FindTextureMain {

    public static void main(String[] args) {
        String baseDir = System.getProperty("user.home")+"/project/games/shiningforce3/data/disk/";
        String filename = baseDir+"bin/x8pc00a.bin";
//        String filename = baseDir + "mpd/sara02.mpd";
//        String filename = baseDir + "mpd/void.mpd";
//        String filename = baseDir + "mpd/tesmap.mpd";
//        String filename = baseDir + "mpd/nasu00.mpd";
//        String filename = baseDir = "bin/x8an00.bin";
        Path path = Paths.get(filename);

        PresentationModel<FindTextureModel> pm = new PresentationModel<>(new FindTextureModel());

        ImageLoader imageLoader = createImageLoader(pm);

        JFrame frame = new JFrame("Find Textures");
        frame.setSize(1000, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(createImagePanel(pm));
        frame.add(new SettingsPanel(pm), BorderLayout.EAST);
        frame.add(new StatusBar(pm), BorderLayout.SOUTH);
        frame.setVisible(true);

        pm.getModel(FindTextureModel.PATH)
                .addValueChangeListener(evt -> frame.setTitle("Find Textures - "+((Path)evt.getNewValue()).getFileName()));
        pm.getBean().setPath(path);
    }

    private static ImageLoader createImageLoader(PresentationModel<FindTextureModel> pm) {
        ImageLoader imageLoader = new ImageLoader(pm, () -> {
            Path path = (Path) pm.getModel(FindTextureModel.PATH).getValue();
            return path == null ? null : Files.newInputStream(path);
        });
        pm.getModel(FindTextureModel.PATH).addValueChangeListener(evt -> {
            try {
                Path path = (Path) pm.getModel(FindTextureModel.PATH).getValue();
                pm.getModel(FindTextureModel.FILE_SIZE).setValue((int) Files.size(path));
                // note: setting file_offset triggers image reload
                pm.getModel(FindTextureModel.FILE_OFFSET).setValue((int) 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return imageLoader;

    }

    private static JPanel createImagePanel(PresentationModel<FindTextureModel> pm) {
        ImagePanel imagePanel = new ImagePanel(pm.getModel(FindTextureModel.IMAGE),
                pm.getModel(FindTextureModel.SCALE),
                pm.getModel(FindTextureModel.IMAGE_PANEL_SIZE),
                pm.getModel(FindTextureModel.MOUSE_POS)
        );
        return imagePanel;
    }

}
