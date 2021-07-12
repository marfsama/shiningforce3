package com.sf3.vdp1.gui;

import com.jgoodies.binding.PresentationModel;
import com.sf3.util.StatusBar;
import com.sf3.util.ImageLoader;
import com.sf3.util.ImagePanel;
import com.sf3.util.SettingsPanel;
import com.sf3.vdp1.model.SaveState;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;

/** Graphical view of the VDP1. */
public class Vdp1View extends JFrame {

    private final PresentationModel<Vdp1Model> pm;
    private final ImageLoader imageLoader;

    public Vdp1View() {
        super("Vdp1View");
        setSize(1000, 1000);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.pm = new PresentationModel<>(new Vdp1Model());
        new SaveStateLoader(pm);

        this.imageLoader = new ImageLoader(pm, () -> {
            SaveState saveState = (SaveState) pm.getModel(Vdp1Model.SAVESTATE).getValue();
            return saveState == null ? null : new ByteArrayInputStream(saveState.getVdp1().getVram().getContent());
        });

        initComponents();
        initBindings();
    }

    private void initBindings() {
        this.pm.getModel(Vdp1Model.PATH).addValueChangeListener(
                evt -> setTitle("Vdp1View - "+((Path)evt.getNewValue()).getFileName()));

        this.pm.getModel(Vdp1Model.SAVESTATE).addValueChangeListener(evt -> this.imageLoader.reloadImage());
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(createSidebarPanel(), BorderLayout.WEST);
        add(createImagePanel(pm), BorderLayout.CENTER);
        add(new StatusBar(pm), BorderLayout.SOUTH);
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.PAGE_AXIS));
        sidebar.add(createSettingsPanel());
        sidebar.add(createTextureControlPanel());
        return sidebar;
    }

    private JPanel createTextureControlPanel() {
        JPanel texturePanel = new TextureControlPanel(pm);
        texturePanel.setBorder(BorderFactory.createTitledBorder("Textures"));

        return texturePanel;
    }

    private SettingsPanel createSettingsPanel() {
        SettingsPanel settingsPanel = new SettingsPanel(pm);
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        return settingsPanel;
    }

    private static JPanel createImagePanel(PresentationModel<Vdp1Model> pm) {
        return new ImagePanel(pm.getModel(Vdp1Model.IMAGE),
                pm.getModel(Vdp1Model.SCALE),
                pm.getModel(Vdp1Model.IMAGE_PANEL_SIZE),
                pm.getModel(Vdp1Model.MOUSE_POS)
        );
    }


    public static void main(String[] args) {
        Vdp1View vdp1view = new Vdp1View();
        vdp1view.setVisible(true);
    }
}
