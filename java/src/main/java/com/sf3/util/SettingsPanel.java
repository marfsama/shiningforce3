package com.sf3.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.sf3.binaryview.FindTextureModel;
import com.sf3.binaryview.HighlightGroup;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/** Change Settings. */
public class SettingsPanel extends JPanel {
    private JFileChooser fileChooser;

    public SettingsPanel(PresentationModel<?> presentationModel) {
        initializeComponents(presentationModel);
    }

    private void initializeComponents(PresentationModel<?> presentationModel) {
        FormLayout layout = new FormLayout(
                // columns
                "right:pref, 6dlu, 100dlu",
                // rows
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

        JSlider scaleSlider = createScaleSlider(presentationModel);
        JSlider textureWidthSlider = createTextureWidthSlider(presentationModel);
        JSlider offsetSlider = createOffsetSlider(presentationModel);
        JTextField offsetTextField = createOffsetTextField(presentationModel);

        ValueModel pathModel = presentationModel.getModel(FindTextureModel.PATH);
        ValueModel byteOrderModel = presentationModel.getModel(FindTextureModel.BYTE_ORDER);
        ValueModel highlightsModel = presentationModel.getModel(FindTextureModel.HIGHLIGHT_GROUPS);

        setLayout(layout);
        add(new JButton(new OpenAction(highlightsModel, pathModel)), CC.xy(3, 1));

        add(new JButton(new OpenHighlightsAction(highlightsModel, pathModel)), CC.xy(3, 3));

        add(new JLabel("Scale"),               CC.xy(1, 5));
        add(scaleSlider,                            CC.xy(3, 5));

        add(new JLabel("Texture Width"),       CC.xy(1, 7));
        add(textureWidthSlider,                     CC.xy(3, 7));

        add(new JLabel("Byte Order"),          CC.xy(1, 9));
        add(createByteOrderCheckbox(byteOrderModel), CC.xy(3, 9));

        add(new JLabel("Offset"),              CC.xy(1, 11));
        add(offsetSlider,                           CC.xy(3, 11));


        add(new JLabel("0x"),                  CC.xy(1, 13));
        add(offsetTextField,                        CC.xy(3, 13));

        JScrollPane highlightGroupsPanel = new JScrollPane(new HighlightGroupsPanel(presentationModel)
                , JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(highlightGroupsPanel, CC.xyw(1, 15, 3));

        this.fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileHidingEnabled(false);
    }

    private JTextField createOffsetTextField(PresentationModel<?> presentationModel) {
        JTextField offsetTextField = new JTextField();
        presentationModel.getModel(FindTextureModel.FILE_OFFSET)
                .addValueChangeListener(evt -> offsetTextField.setText(Integer.toHexString((Integer) evt.getNewValue())));
        offsetTextField.addActionListener(
                (evt) -> {
                    String valueString = ((JTextField)evt.getSource()).getText();
                    try {
                        int value = Integer.parseInt(valueString, 16);
                        presentationModel.getModel(FindTextureModel.FILE_OFFSET).setValue(value);
                    }
                    catch ( NumberFormatException e) {
                        System.out.println("'"+valueString+"' is not a hex value.");
                    }
                });
        return offsetTextField;
    }

    private JSlider createOffsetSlider(PresentationModel<?> presentationModel) {
        JSlider scaleSlider = SwingBindings.createSlider(
                presentationModel.getModel(FindTextureModel.FILE_OFFSET), JSlider.HORIZONTAL, 0,100);

        Bindings.bind(scaleSlider, "maximum", presentationModel.getModel(FindTextureModel.FILE_SIZE));
        presentationModel.getModel(FindTextureModel.TEXTURE_WIDTH).addValueChangeListener(evt -> {
            int width = (Integer) evt.getNewValue();
            scaleSlider.setMinorTickSpacing(width*2);
            scaleSlider.setMajorTickSpacing(width*20);
        });

        scaleSlider.setSnapToTicks(true);
        scaleSlider.setMinorTickSpacing(32);
        scaleSlider.setMajorTickSpacing(32*20);
        scaleSlider.setPaintTicks(true);
        return scaleSlider;
    }

    private JCheckBox createByteOrderCheckbox(ValueModel byteOrderModel) {
        JCheckBox checkBox = new JCheckBox(new ByteOrderAction(byteOrderModel));
        byteOrderModel.addValueChangeListener(evt -> checkBox.setSelected(evt.getNewValue() == ByteOrder.LITTLE_ENDIAN));
        checkBox.setSelected(byteOrderModel.getValue() == ByteOrder.LITTLE_ENDIAN);
        return checkBox;
    }

    private JSlider createScaleSlider(PresentationModel<?> presentationModel) {
        JSlider scaleSlider = SwingBindings.createSlider(
                presentationModel.getModel(FindTextureModel.SCALE), JSlider.HORIZONTAL, 1,20);
        scaleSlider.setMinorTickSpacing(1);
        scaleSlider.setMajorTickSpacing(9);
        scaleSlider.setPaintLabels(true);
        scaleSlider.setPaintTicks(true);
        return scaleSlider;
    }

    private JSlider createTextureWidthSlider(PresentationModel<?> presentationModel) {
        JSlider textureWidthSlider = SwingBindings.createSlider(
         presentationModel.getModel(FindTextureModel.TEXTURE_WIDTH), JSlider.HORIZONTAL, 1,512);
        textureWidthSlider.setMajorTickSpacing(1);
        textureWidthSlider.setSnapToTicks(true);
        textureWidthSlider.setPaintLabels(true);
        textureWidthSlider.setPaintTicks(true);
        return textureWidthSlider;
    }

    private int getOpenDialog() {
        Properties settings = readSettings();
        if (settings.containsKey("lastDir")) {
            fileChooser.setCurrentDirectory(new File(settings.getProperty("lastDir")));
        }
        return fileChooser.showOpenDialog(SettingsPanel.this);
    }

    private class OpenAction extends AbstractAction {

        private final ValueModel pathModel;
        private final ValueModel highlightsModel;

        public OpenAction(ValueModel highlightsModel, ValueModel pathModel) {
            super("Open...");
            this.pathModel = pathModel;
            this.highlightsModel = highlightsModel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int result = getOpenDialog();
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                writeSettings(file);
                SwingUtilities.invokeLater(() -> pathModel.setValue(file.toPath()));
                SwingUtilities.invokeLater(() -> highlightsModel.setValue(new GuiHighlights()));
            }
        }
    }

    private class OpenHighlightsAction extends AbstractAction {

        private final ValueModel highlightsModel;
        private final ValueModel pathModel;

        public OpenHighlightsAction(ValueModel highlightsModel, ValueModel pathModel) {
            super("Open with Highlights...");
            this.highlightsModel = highlightsModel;
            this.pathModel = pathModel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int result = getOpenDialog();
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    writeSettings(file);
                    DataClass value = mapper.readValue(file, DataClass.class);
                    GuiHighlights guiHighlights = new GuiHighlights(file.getAbsolutePath(), value.getHighlights());
                    guiHighlights.getShowAreas().addAll(guiHighlights.getHighlightGroups().stream().map(HighlightGroup::getName).collect(Collectors.toList()));
                    SwingUtilities.invokeLater(() -> highlightsModel.setValue(guiHighlights));
                    SwingUtilities.invokeLater(() -> pathModel.setValue(Paths.get(value.getFilename())));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    private Properties readSettings() {
        Path configPath = Paths.get(System.getProperty("user.home")).resolve(".config/binaryview");
        Path configFile = configPath.resolve("config.properties");
        if (!Files.exists(configPath)) {
            return new Properties();
        }
        try {
            String propertiesString = Files.readString(configFile, StandardCharsets.UTF_8);
            Properties properties = new Properties();
            properties.load(new StringReader(propertiesString));
            return properties;
        } catch (IOException e) {
            e.printStackTrace();
            return new Properties();
        }
    }

    private void writeSettings(File loadedFile) {
        try {
            String lastDir = loadedFile.toPath().getParent().toAbsolutePath().toString();
            Path configPath = Paths.get(System.getProperty("user.home")).resolve(".config/binaryview");
            Files.createDirectories(configPath);
            Files.write(configPath.resolve("config.properties"),
                    ("lastDir="+lastDir).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class DataClass {
        private String filename;
        private List<HighlightGroup> highlights;
    }


    private static class ByteOrderAction extends AbstractAction {
        private final ValueModel byteOrderModel;

        public ByteOrderAction(ValueModel byteOrderModel) {
            super("Little Endian");
            this.byteOrderModel = byteOrderModel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox checkbox = (JCheckBox) e.getSource();
            boolean checked = checkbox.isSelected();
            byteOrderModel.setValue(checked ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        }
    }
}
