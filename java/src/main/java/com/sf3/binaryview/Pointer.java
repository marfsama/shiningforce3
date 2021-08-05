package com.sf3.binaryview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pointer from one part of the file to another. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pointer {
    /** Offset where the pointer is located. */
    private int start;
    /** Destination where the pointer points to. */
    private int destination;
}
