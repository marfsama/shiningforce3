package com.sf3.util;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.sf3.binaryview.FindTextureModel;
import com.sf3.binaryview.HighlightGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shows {@link com.sf3.binaryview.HighlightGroups} and some controls to display or hide them. */
public class HighlightGroupsPanel extends JPanel {

    private ValueModel highlightsModel;

    public HighlightGroupsPanel(PresentationModel<?> presentationModel) {
        initializeComponents(presentationModel);
    }

    private void initializeComponents(PresentationModel<?> presentationModel) {
        FormLayout layout = new FormLayout("right:150, 6dlu, 20dlu, 3dlu, 20dlu, 3dlu, 20dlu, 3dlu");
        setLayout(layout);

        this.highlightsModel = presentationModel.getModel(FindTextureModel.HIGHLIGHT_GROUPS);
        highlightsModel.addValueChangeListener(event ->
                SwingUtilities.invokeLater(() -> updateLayout((GuiHighlights) event.getNewValue())));
    }

    private void updateLayout(GuiHighlights guiHighlights) {
        removeAll();
        FormLayout layout = (FormLayout) getLayout();
        layout.appendRow(RowSpec.decode("pref"));
        layout.appendRow(RowSpec.decode("3dlu"));
        add(new JLabel("name"), CC.xy(1,1));
        add(new JLabel("color"), CC.xy(3,1));
        add(new JLabel("area"), CC.xy(5,1));
        add(new JLabel("pointer"), CC.xy(7,1));

        // get HighlightGroups and make a copy of the list
        List<HighlightGroup> groups = new ArrayList<>(guiHighlights.getHighlightGroups());
        // sort by name
        Collections.sort(groups, (a,b) -> a.getName().compareTo(b.getName()));
        int currentRow = 3;
        for (var group : groups) {
            if (layout.getRowCount() < currentRow) {
                layout.appendRow(RowSpec.decode("pref"));
                layout.appendRow(RowSpec.decode("3dlu"));
            }
            JLabel name = new JLabel(group.getName());
            name.setToolTipText(group.getName());
            add(name, CC.xy(1, currentRow));
            JComponent colorBox = createColorBox();
            colorBox.setBackground(new Color(group.getColor()));
            colorBox.setBorder(BorderFactory.createLineBorder(new Color(group.getColor()).darker(), 2));
            add(colorBox, CC.xy(3, currentRow));
            add(createAreaCheckbox(guiHighlights, group.getName()), CC.xy(5, currentRow));
            add(new JCheckBox(), CC.xy(7, currentRow));
            currentRow += 2;
        }
        getParent().revalidate();
    }

    private JCheckBox createAreaCheckbox(GuiHighlights guiHighlights, String name) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.addActionListener(event -> {
            JCheckBox button = (JCheckBox) event.getSource();
            boolean selected = button.isSelected();
            GuiHighlights newGuiHighlights = new GuiHighlights(guiHighlights);
            if (selected) {
                newGuiHighlights.getShowAreas().add(name);
            }
            else {
                newGuiHighlights.getShowAreas().remove(name);
            }
            SwingUtilities.invokeLater(() -> highlightsModel.setValue(newGuiHighlights));
        });
        checkBox.setSelected(true);
        return checkBox;
    }

    private JComponent createColorBox() {
        return new JPanel();
    }
}
