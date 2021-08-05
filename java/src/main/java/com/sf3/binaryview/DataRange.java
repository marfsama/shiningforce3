package com.sf3.binaryview;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Highlight for an area of the file. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataRange {
    /** offset where the range starts. */
    private int start;
    /** size in bytes of the data range. */
    private int size;

    /** Returns the offset which is *not* part of the range anymore. */
    @JsonIgnore
    public int getEnd() {
        return start + size;
    }
}
