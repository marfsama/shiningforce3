package com.sf3.vdp1.gui;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.sf3.util.SwingBindings;
import com.sf3.vdp1.model.SaveState;
import com.sf3.vdp1.model.Texture;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/** Shows textures from VRAM. */
public class TextureControlPanel extends JPanel {
    private TexturePanel texturePanel;

    public TextureControlPanel(PresentationModel<Vdp1Model> pm) {
        initComponents(pm);
    }

    private void initComponents(PresentationModel<Vdp1Model> pm) {
        ValueModel savestateModel = pm.getModel(Vdp1Model.SAVESTATE);
        ValueModel selectedTexture = pm.getModel(Vdp1Model.SELECTED_TEXTURE);

        setLayout(new FormLayout(
                "right:pref, 6dlu, 100dlu",  // columns
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref"));           // rows
        add(new JLabel("# Textures: "), CC.xy(1,1));
        JLabel numTexturesLabel = new JLabel("-");
        savestateModel.addValueChangeListener(evt -> {
            SaveState savestate = (SaveState) evt.getNewValue();
            if (savestate == null) {
                numTexturesLabel.setText("-");
            }
            else {
                numTexturesLabel.setText(""+savestate.getTextures().size());
            }
        });
        add(numTexturesLabel, CC.xy(3,1));

        add(new JLabel("Selected: "), CC.xy(1,3));
        JLabel selectedLabel = new JLabel("-");
        add(selectedLabel, CC.xy(3,3));

        add(createTextureSlider(savestateModel, selectedTexture), CC.xy(3,5));

        add(new JLabel("Size: "), CC.xy(1,7));
        JLabel sizeLabel = new JLabel("-");
        add(sizeLabel, CC.xy(3,7));

        add(new JLabel("ColorMode: "), CC.xy(1,9));
        JLabel colorLabel = new JLabel("-");
        add(colorLabel, CC.xy(3,9));

        add(new JButton(new SavePngAction()), CC.xy(3,11));

        add(new JButton(new SaveRawAction(savestateModel, selectedTexture)), CC.xy(3,13));

        selectedTexture.addValueChangeListener(evt -> {
            Integer textureIndex = (Integer) evt.getNewValue();
            SaveState savestate = (SaveState) savestateModel.getValue();
            if (textureIndex == null || savestate == null) {
                selectedLabel.setText("-");
                sizeLabel.setText("-");
                colorLabel.setText("-");
            } else {
                Texture texture = new ArrayList<>(savestate.getTextures().values()).get(textureIndex);
                selectedLabel.setText(""+textureIndex+" (0x"+Integer.toHexString(texture.getOffset())+")");
                sizeLabel.setText(""+texture.getWidth()+"x"+texture.getHeight());
                colorLabel.setText(""+texture.getColorMode()+"(0x"+Integer.toHexString(texture.getColorbank())+")");
            }
        });

        this.texturePanel = createTexturePanel(savestateModel, selectedTexture);
        add(texturePanel, CC.xywh(1,15,3,1));
    }

    private TexturePanel createTexturePanel(ValueModel savestate, ValueModel selectedTexture) {
        return new TexturePanel(savestate, selectedTexture);
    }

    private JSlider createTextureSlider(ValueModel savestateModel, ValueModel selectedTexture) {
        JSlider textureSlider = SwingBindings.createSlider(
                selectedTexture, JSlider.HORIZONTAL, 0,10);

        savestateModel.addValueChangeListener(evt -> {
            SaveState savestate = (SaveState) savestateModel.getValue();
            if (savestate != null) {
                textureSlider.setMaximum(savestate.getTextures().size()-1);
                textureSlider.setValue(0);
            }
        });

        return textureSlider;
    }

    private class SavePngAction extends AbstractAction {
        private JFileChooser fileChooser;

        public SavePngAction() {
            super("save png");
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("png files", "png"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BufferedImage image = texturePanel.getTextureImage();
            if (image == null) {
                return;
            }

            try {
                int result = fileChooser.showSaveDialog(TextureControlPanel.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    ImageIO.write(image, "png", file);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private class SaveRawAction extends AbstractAction {
        private final ValueModel savestateModel;
        private final ValueModel selectedTexture;
        private JFileChooser fileChooser;

        public SaveRawAction(ValueModel savestateModel, ValueModel selectedTexture) {
            super("save raw");
            this.savestateModel = savestateModel;
            this.selectedTexture = selectedTexture;
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("raw files", "raw"));
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            SaveState savestate = (SaveState) this.savestateModel.getValue();
            Integer selectedTexture = (Integer) this.selectedTexture.getValue();
            if (savestate == null || selectedTexture == null) {
                return;
            }


            try {
                int result = fileChooser.showSaveDialog(TextureControlPanel.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();

                    byte[] vram = savestate.getVdp1().getVram().getContent();
                    Texture texture = new ArrayList<>(savestate.getTextures().values()).get(selectedTexture);
                    int bitsPerColor = texture.getColorMode().getBitsPerColor();
                    int length = (texture.getWidth() * texture.getHeight() * bitsPerColor) / 8;
                    ByteArrayInputStream stream = new ByteArrayInputStream(vram, texture.getOffset(), length);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    for (int i = 0; i < length/2; i++) {
                        int b1 = stream.read();
                        int b2 = stream.read();
                        out.write(b2);
                        out.write(b1);
                    }

                    Files.write( file.toPath(), out.toByteArray());
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }


}
