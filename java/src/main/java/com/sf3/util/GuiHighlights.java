package com.sf3.util;

import com.sf3.binaryview.HighlightGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Highlights with gui additions. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuiHighlights {
    private String path;
    private Set<String> showAreas = new HashSet<>();
    private List<HighlightGroup> highlightGroups = new ArrayList<>();

    public GuiHighlights(String path, List<HighlightGroup> highlightGroups) {
        this.path = path;
        this.highlightGroups = highlightGroups;
    }

    public GuiHighlights(GuiHighlights guiHighlights) {
        this.path = guiHighlights.path;
        this.showAreas = new HashSet<>(guiHighlights.showAreas);
        this.highlightGroups = new ArrayList<>(guiHighlights.highlightGroups);
    }
}
