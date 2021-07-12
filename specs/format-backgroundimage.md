# Preamble

This document describes the file format for Shining Force 3 background images.
Background images are saved in files with the pattern "x4en*.bin".
Note: the "en" in the filename might be a language code.

The file is in big endian format.

# File Structure

    +------------------------....-+
    | Pallete (0x200 bytes)       |
    +-----------------------------+
    | Image Data (0x2_0000 bytes) |
    +-----------------------------+

# Palette

The palette consists of 256 colors in RGB555 format.

# Image Data

The x4en* files are always 512x256 pixels. The data is color indexed into the above pallete.
