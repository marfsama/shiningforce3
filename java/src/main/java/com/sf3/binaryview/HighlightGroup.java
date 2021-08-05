package com.sf3.binaryview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Groups of highlighted areas with the same semantics and therefore the same nam and color. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighlightGroup {
    private String name;
    private int color;
    private List<DataRange> highlights = new ArrayList<>();
    private List<Pointer> pointers = new ArrayList<>();

    public HighlightGroup(String name, int color) {
        this.name = name;
        this.color = color;
    }
}
