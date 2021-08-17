# Description

The mpd files (Map Data) contain the 3d map data for normal travel and battle. Maps consist of

* static meshes like buildings, trees
* movable or interactible meshes like barrels and chests
* textures and animated textures
* skybox images
* heightmap with textures
* rotating scrolls used as ground (i.e. water)
* (wild guess) walkmesh
* (wild guess) terrain movement costs

(Note: The file should also contain the mesh data for individual battle, but I haven't found it yet)

The file is split in a header, which is always loaded to 0x290000, and a list of chunks. The individual chunks are
mostly compressed and are loaded to individual memory locations depending on the content.

# File Structure

![01 File Overview](img/01_file_overview.png)

| Offset   | Name         | Type       | Count  | Description |
|----------|--------------|------------|--------|-------------|
| 0x00     | header       | Header     |   1    | File header. See description in Chapter [Header](#header)
| 0x2000   | chunk_table  | ChunkTable |   1    | Table with chunk offsets and sized. See description in Chapter [Chunks](#chunks)
| 0x2100   | chunks       | Chunk[]    | 0..20  | At most 20 chunks. See description in Chapter [Chunks](#chunks)


# Header

The total header size is always 0x2000 bytes. Unused bytes are padded with zero. 


## File header

Size: 12 bytes

The first int32 in the file is an offset to a sub header, which contains just another offset to the final header.

| Offset | Name    | Type  | Count | Description |
|--------|---------|-------|------|-------------|
| 0x00   | offset1 | int32 |  1   | first header indirection. pointer to sub header
| 0x04   |         | int32 |  2   | zero (0x0). padding? 

## Sub Header

Size: 4 bytes

Just an offset to the final header.

| Offset  | Name    | Type  | Count | Description |
|---------|---------|-------|------|-------------|
| 0x00    | offset2 | int32 |  1   | second header indirection. pointer to sub sub header


## Sub Sub Header

Size:  0x58 (88) bytes

This is the header.

| Offset  | Name      | Type  | Count | Description |
|---------|-----------|-------|------|--------------|
|  0x00   | unknown_1 | int16 |  1   | Unknown. Might be map id.
|  0x02   | unknown_2 | int16 |  1   | Always zero 0x0000
|  0x04   | offset1   | int32 |  1   | Always 0x0c. Pointer to 0x20 unknown int16. See (#header-offset-1).
|  0x08   | offset2   | int32 |  1   | Always 0x4c. pointer to a single unknown int32. See (#header-offset-2).
|  0x0C   | offset3   | int32 |  1   | Always 0x50. pointer to 0x20 unknown int16s at the start of the file. Mostly zero or 0x8000. (#header-offset-3)
|  0x10   | unknown_3 | int16 |  1   | Unknown small value. maybe some count?
|  0x12   | unknown_4 | int16 |  1   | Always zero
|  0x14   | offset4   | int32 |  1   | Always 0x90. Pointer to unknown structure. See (#header-offset-4)
|  0x18   | offset_texture_groups | int32 |  1   | Offset to list of texture groups. See (#texture-groups)
|  0x1C   | offset6   | int32 |  1   | Pointer to unknown list. 
|  0x20   | offset7   | int32 |  1   | Pointer to unknown list.
|  0x24   | offset_mesh_1 | int32 |  1   | Pointer to list of 2 moveable/interactable mesh. may be null. 
|  0x28   | offset_mesh_2 | int32 |  1   | Pointer to list of 2 moveable/interactable mesh. may be null.
|  0x2C   | offset_mesh_3 | int32 |  1   | Pointer to list of 2 moveable/interactable mesh. may be null.
|  0x30   | const_1   | int32 |  1   | Const 0x8000b334
|  0x34   | const_2   | int32 |  1   | Const 0x4ccc0000
|  0x38   | offset11  | int32 |  1   | Sometimes pointer to list small uint16, the list end is marked by 0xffff. May be null. 
|  0x3C   | offset_pal_1  | int32 |  1   | Pointer to 256 rgb16 colors. May be null.
|  0x40   | offset_pal_2  | int32 |  1   | Pointer to 256 rgb16 colors. May be null.
|  0x44   | unknown_5 | int32 |  1   | Unknown small value, may be negative.
|  0x48   | const_3   | int32 |  1   | Const 0xc000
|  0x4C   | unknown_6 | int32 |  1   | Unknown. Lower 16 bits often null. May me FIXED.
|  0x50   | unknown_7 | int32 |  1   | Unknown. Small value in upper int16, 0x0000 in lower int16. May me FIXED. 
|  0x54   | offset12  | int32 |  1   | Pointer to unknown list of exactly 8 uint16 in two block with 4 uint16 each.

## Header Offset 1

This list contains exactly 32 (0x20) int16 values. The purpose is unknown.

The values are sometimes repeated in blocks of 2 and strictly ascending and sometimes there is no visible pattern.
The step from one value to the next is often 0x421.

## Header Offset 2

This list contains exactly one int32 value. The purpose is unknown.

This value can be the same in different maps (0xa2246167 in sara02-sara04) or totally different (0xbe6858ef in bochi).

## Header Offset 3

This list contains exactly 16 int32 or 32 int16 values. The purpose is unknown.

This list is most often filled with zeros.

## Header Offset 4

Size: 16 bytes

This list contains some unknown structures. The value_1 are ascending with step 1. The values pointed to by offset_1 
and offset_2 are small (< 0x100) and are in the same range. They don't need to be ascending, but there won't be 
duplicates.


| Offset | Name     | Type  | Count | Description  |
|--------|----------|-------|-------|--------------|
| 0x00   | value_1  | int32 |   1   | Unknown small value. 0xffff when the list is finished. 
| 0x04   | offset_1 | int32 |   1   | Pointer to list of small short values. The list is terminated by 0xffff.
| 0x08   | offset_2 | int32 |   1   | Pointer to list of small short values. The list is terminated by 0xffff.
| 0x0C   | value_2  | int32 |   1   | Unknown value, mostly zero.

## Texture Groups

This is a list of texture groups which themselves have a list of the textures for the animation.
The end of the texture group list is marked by 0xffff (texture_group = 0xffff).

| Offset | Name           | Type  | Count | Description |
|--------|----------------|-------|-------|--------------|
| 0x00   | texture_group  | int16  |   1   | Texture number for SGL.
| 0x02   | texture_width  | int16  |   1   | width of textures in this group
| 0x04   | texture_height | int16  |   1   | height of textures in this group
| 0x06   |                | int16  |   1   | unknown small value. maybe animation speed
| 0x08   | frames[]      | [TextureFrame](#texture-frame) |   ?   | List of Texture Animation Frames.

## Texture Frame

Size: 4 bytes

List of textures in the animation. The offsets points into the
*compressed* data, only one texture image is compressed in this entry.
The end of the list is marked by an offset of 0xfffe.

| Offset | Name    | Type   | Count | Description |
|--------|---------|--------|-------|-------------|
| 0x00   | offset  | uint16 |   1   | offset into compressed data
| 0x02   | unknown | int16  |   1   | unknown small value


## Moveable or interactible objects

Size: 0x1c (28) bytes

The objects list contains meshes for barrels, chest and crates. The end of the list is marked by 0x0.
Sometimes the data is in the file, but the pointer in the header is null, so it is "dangling".


| Offset | Name              | Type  | Count | Description |
|--------|-------------------|-------|-------|--------------|
| 0x00   | offset_polydata   | int32 |   1   | pointer to PDATA (see SGL)
| 0x04   | position          | int16 |   3   | Integer part of the position. Decimal part is 0x0000
| 0x0A   | rotation          | ANGLE |   1   | Rotation of the mesh.
| 0x10   | scale             | FIXED |   3   | Scale of the mesh.

# Chunks

Chunks are memory blocks which may be compressed and are placed in specific memory locations, depending on the content.
The chunks are always in the same order, but not all files need all chunk types. 

| Chunk Index | Name                     | Description
|-------------|--------------------------|------------
|    0        | empty_1                  | This chunk always seem to be empty (size 0).
|    1        | static_meshes            | [Static map objects](#static-meshes). Mesh data and position/rotation/scale.
|    2        | surface                  | Surface texture, surface normals and an unknown surface attribute.
|    3        | texture_animation_images | Images for animated textures. Each image is individually compressed.
|    4        | empty_2                  | This chunk always seem to be empty (size 0).
|    5        | surface_meshes           | Heightmap for each surface tile and some unknown surface attributes. Might be walkmesh and movement costs. The chunk is compressed.
|  6 - 10     | textures                 | Texture images. The chunks are compressed.
| 11 - 13     | object_textures          | Textures for moveable objects (see [Header](#header)). The chunks are compressed.
| 14 - 19     | scroll_panes             | Memory blocks for scroll panes (skybox) and rotating scrolls (ground). The chunks are compressed.

## Compressed Streams

Some Chunks use some kind of LZ77 compression.

The chunk is divided in "lines", which consists of a int16 "control" and
16 int16s data. One bit in control corresponds to one int16 of the data,
starting with msb of control. Cleared bit in control means "data int16",
set bit in control means "command int16".

## Chunk Table

Size: 20 * 8 bytes (160 Bytes)

Always starts at  offset 0x2000. The table only contains the offsets and the sizes. Unused chunks have the offset
of the next chuck (the offset of the next free location in the file) and a size of zero.

The first chunk starts at 0x2100. As the first chunk is always [static meshes](#static-meshes), this is the chunk at
0x2100.

| Offset | Name               | Type           | Count | Description
|--------|--------------------|----------------|-------|-----------------------------
| 0x00   | chunk_offsets      | offset[]       |  20   | offsets and sizes of the chunk.


## Offset

Size: 8 bytes

| Offset | Name    | Type   | Count | Description
|--------|---------|--------|-------|-----------------------------
| 0x0    | offset  | int32  |   1   | offset to chunk
| 0x4    | size    | int32  |   1   | size of chunk



# Static Meshes

Describes the static meshes in the maps. These are mostly buildings, fences, trees and similar. Some maps use static
meshes for floor tiles too.

Note: This whole file is normally loaded to 0x290000, so this chunk will be at offset 0x292100 and all offsets in this
chunk are relative to 0x292100. In some maps only this chunk is loaded to 0x60a0000, so you have to adjust the
logic to make these offsets relative to the file.

TODO:
The structure of the stuff behind the `offset1` and `offset2` is known, but the meaning of the data is still unknown.

## Structure

![mesh overview](img/02_mesh_overview.png)

The chunk consists of the header with the mesh list and the PDATA structures. The order of the PDATA list is: PDATA, 
list of POINTs, list of POLYGONs and list of polygon ATTRs. After the PDATA structure there are two unknown list of 
stuff. 

## Header

| Offset | Name         | Type      | Count       | Description
|--------|--------------|-----------|-------------|-----------------------------
| 0x00   | offset1      | int32     |   1         | offset to unknown stuff. see (#static-objects-offset1). May be null.
| 0x04   | offset2      | int32     |   1         | offset to unknown stuff. see (#static-objects-offset2). May be null.
| 0x08   | num_meshes   | int16      |   1         | number of objects
| 0x0A   |              | int16      |   1         | padding, 0x00
| 0x0C   | meshes[]     | Mesh[]    | num_meshes  | for each mesh a [Mesh](#model-head) structure.    


## Mesh

Size: 0x3C (60) bytes

Each mesh consists of the transform (translation, rotation, scale) and up to 8 polygon meshes.
TODO: this might be some kind of level-of-detail.

Note: for PDATA see SGL Reference.

| Offset | Name            | Type      | Count | Description
|--------|-----------------|-----------|-------|-----------------------------
| 0x00   | pdata_offsets[] | int32     |   8   | 8 offsets to #PDATA structures. These pdata points to the same mesh and different face attributes (#ATTR).
| 0x20   | position        | int16[3]  |   1   | position of the mesh in x,y,z. Only the integer part.
| 0x26   | rotation        | ANGLE[3]  |   1   | rotation of the mesh in SGL ANGLE.
| 0x2C   | scale           | FIXED[3]  |   1   | scale of the mesh
| 0x38   | padding         | uint32    |   1   | padding

## Static Objects Offset1

| Offset | Name     | Type   | Count | Description
|--------|----------|--------|-------|-----------------------------
| 0x00   | offset1  | int32  |   1   | offset to list of ints. size of the list is unknown.
| 0x04   | offset2  | int32  |   1   | offset to list of two ints each. size seems to be the same the list at offset1

Note:
In the list behind offset 2 the first int contains strictly ascending values in the first 16 bits and the last 16 bits.

Example sara06.json:

    "stuff_at_offset_1": {
      "offset_1": "0x113f4 (raw: 0x2a13f4)",
      "offset_2": "0x11570 (raw: 0x2a1570)",
      "count_1": 95,
      "count_1_hex": "0x5f",
      "values_1": [
        "0x39f015f",
        "0x3a0025f",
        "0x29d0262",
        "0x29b0207",
        "0x25d01c7",
        "0x1a301c6",
        "0x1660203",
        "0x164029a",
        "0x1e4029b",
        "0x1e402e3",
        ...
        "0x3de0401",
        "0x3de041e",
        "0x38202a0",
        "0x3800280",
        "0x3610280",
        "0x361029e"
      ],
      "count_2": 93,
      "count_2_hex": "0x5d",
      "values_2": [
        "0x1 0x7fbe1c00",
        "0x4c 0x41c34b00",
        "0x10002 0xbf871b00",
        "0x20003 0xff6b1a00",
        "0x30004 0xe0a51900",
        "0x40005 0xc0321800",
        "0x50006 0x9fdd1700",
        "0x60007 0x807c1600",
        "0x70008 0x40711500",
        "0x80009 0x80391400",
        "0x9000a 0xbfcf1300",
        "0xa000b 0x80421200",
        "0xb000c 0xbfaf1100",
        "0xc000d 0x805d1000",
        "0xd000e 0x406c0f00",
        "0xe000f 0x7d290e00",
        "0xf0010 0x41610d00",
        ...
        "0x500051 0x7f7a5000",
        "0x510052 0x3f7a4f00",
        "0x530054 0x40215200",
        "0x540055 0x7ee55300",
        "0x550056 0xc0bf5400",
        "0x57005a 0x3f705700",
        "0x570058 0xff735800",
        "0x580059 0x40685500",
        "0x59005a 0x80b35600",
        "0x5b005c 0xfe1a5c00",
        "0x5c005d 0xc0635b00",
        "0x5d005e 0x80005a00"
      ]
    },

## Static Objects Offset2

| Offset | Name     | Type   | Count | Description
|--------|----------|--------|-------|-----------------------------
| 0x00   | offset   | int32  |  265  | list of 256 offsets which each point to a list of shorts.


List of shorts:
| Offset | Name       | Type   | Count | Description
|--------|------------|--------|-------|-----------------------------
| 0x00   | value      | int16  |  ?    | small unknown short
| ??     | end_marker | int16  |  1    | 0xFFFF. Denotes end of list.


These list form some kind of triangles (see example). A very wild guess might be some kind of 
[BSP Tree](https://en.wikipedia.org/wiki/Binary_space_partitioning) to detect meshes which doesn't need to be drawn.

Example sara06.json:

    "stuff_at_offset_2": {
      "size": 256,
      "offsets": {
        "0x11c58": "[]",
        "0x11c5a": "[]",
        "0x11c5c": "[]",
        "0x11c5e": "[]",
        "0x11c60": "[]",
        "0x11c62": "[]",
        "0x11c64": "[]",
        "0x11c66": "[]",
        "0x11c68": "[]",
        "0x11c6a": "[]",
        "0x11c6c": "[]",
        "0x11c6e": "[]",
        "0x11c70": "[]",
        "0x11c72": "[]",
        "0x11c74": "[]",
        "0x11c76": "[]",
        "0x11c78": "[]",
        "0x11c7a": "[]",
        "0x11c7c": "[]",
        "0x11c7e": "[]",
        "0x11c80": "[]",
        "0x11c82": "[]",
        "0x11c84": "[]",
        "0x11c86": "[0x4e, 0x4f]",
        "0x11c8c": "[0x4e, 0x4f]",
        "0x11c92": "[]",
        "0x11c94": "[]",
        "0x11c96": "[]",
        "0x11c98": "[]",
        "0x11c9a": "[]",
        "0x11c9c": "[]",
        "0x11c9e": "[]",
        "0x11ca0": "[]",
        "0x11ca2": "[]",
        "0x11ca4": "[]",
        "0x11ca6": "[]",
        "0x11ca8": "[]",
        "0x11caa": "[]",
        "0x11cac": "[0x0, 0x1]",
        "0x11cb2": "[0x0, 0x1, 0x2d, 0x4e, 0x4f]",
        "0x11cbe": "[0x1, 0x2c, 0x2d, 0x4e, 0x4f]",
        "0x11cca": "[0x2c, 0x2d]",
        "0x11cd0": "[]",
        "0x11cd2": "[]",
        "0x11cd4": "[]",
        "0x11cd6": "[]",
        "0x11cd8": "[]",
        "0x11cda": "[]",
        "0x11cdc": "[]",
        "0x11cde": "[]",
        "0x11ce0": "[0x5, 0x6, 0x7, 0x2a, 0x2b]",
        "0x11cec": "[0x5, 0x6, 0x7, 0x2a, 0x2b]",
        "0x11cf8": "[0x3, 0x4, 0x5, 0x28, 0x2b]",
        "0x11d04": "[0x3, 0x4, 0x5, 0x28, 0x2b]",
        "0x11d10": "[0x0, 0x1]",
        "0x11d16": "[0x0, 0x1, 0x2d, 0x4e, 0x4f]",
        "0x11d22": "[0x1, 0x2c, 0x2d, 0x4e, 0x4f]",
        "0x11d2e": "[0x2c, 0x2d]",
        "0x11d34": "[0x31, 0x32, 0x33, 0x34, 0x35]",
        "0x11d40": "[0x31, 0x32, 0x33, 0x34, 0x35]",
        "0x11d4c": "[0x35, 0x36, 0x37]",
        "0x11d54": "[0x35, 0x36, 0x37]",
        "0x11d5c": "[]",
        "0x11d5e": "[]",
        "0x11d60": "[]",
        "0x11d62": "[]",
        "0x11d64": "[0x5, 0x6, 0x7, 0x8, 0x29, 0x2a, 0x2b]",
        "0x11d74": "[0x5, 0x6, 0x7, 0x8, 0x9, 0x29, 0x2a, 0x2b]",
        "0x11d86": "[0x2, 0x3, 0x4, 0x5, 0x8, 0x9, 0x20, 0x23, 0x28, 0x29, 0x2b]",
        "0x11d9e": "[0x2, 0x3, 0x4, 0x5, 0x20, 0x23, 0x28, 0x29, 0x2b]",
        "0x11db2": "[0x0, 0x2, 0x21, 0x22, 0x23, 0x5a, 0x5b, 0x5c]",
        "0x11dc4": "[0x0, 0x2, 0x21, 0x22, 0x23, 0x5a, 0x5b, 0x5c]",
        "0x11dd6": "[0x2c]",
        "0x11dda": "[0x2c]",
        "0x11dde": "[0x2f, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35]",
        "0x11dee": "[0x2f, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x4a, 0x4b, 0x4c, 0x4d]",
        "0x11e06": "[0x35, 0x36, 0x37, 0x49, 0x4a, 0x4b, 0x4c, 0x4d]",
        "0x11e18": "[0x35, 0x36, 0x37, 0x49, 0x4a]",
        "0x11e24": "[]",
        "0x11e26": "[]",
        "0x11e28": "[]",
        "0x11e2a": "[]",
        "0x11e2c": "[0x7, 0x8, 0xa, 0xb, 0xc, 0xd, 0x29, 0x2a]",
        "0x11e3e": "[0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0x26, 0x27, 0x29, 0x2a]",
        "0x11e56": "[0x2, 0x3, 0x8, 0x9, 0xa, 0x20, 0x23, 0x24, 0x26, 0x27, 0x28, 0x29]",
        "0x11e70": "[0x2, 0x3, 0x20, 0x23, 0x24, 0x27, 0x28, 0x29]",
        "0x11e82": "[0x0, 0x2, 0x21, 0x22, 0x23, 0x5a, 0x5b, 0x5c]",
        "0x11e94": "[0x0, 0x2, 0x21, 0x22, 0x23, 0x50, 0x51, 0x52, 0x5a, 0x5b, 0x5c]",
        "0x11eac": "[0x2c, 0x2e, 0x3e, 0x3f, 0x50, 0x51, 0x52]",
        "0x11ebc": "[0x2c, 0x2e, 0x3e, 0x3f]",
        "0x11ec6": "[0x2e, 0x2f, 0x30, 0x31, 0x3b, 0x3c, 0x3d, 0x3e]",
        "0x11ed8": "[0x2e, 0x2f, 0x30, 0x31, 0x3b, 0x3c, 0x3d, 0x3e, 0x44, 0x45, 0x46, 0x48, 0x4a, 0x4b, 0x4c, 0x4d]",
        "0x11efa": "[0x37, 0x44, 0x45, 0x46, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d]",
        "0x11f10": "[0x37, 0x48, 0x49, 0x4a]",
        "0x11f1a": "[]",
        "0x11f1c": "[]",
        "0x11f1e": "[]",
        "0x11f20": "[]",
        "0x11f22": "[0xa, 0xb, 0xc, 0xd, 0xe]",
        "0x11f2e": "[0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0x10, 0x11, 0x25, 0x26, 0x27]",
        "0x11f48": "[0x9, 0xa, 0xe, 0xf, 0x10, 0x11, 0x20, 0x24, 0x25, 0x26, 0x27]",
        "0x11f60": "[0x20, 0x24, 0x25, 0x27]",
        "0x11f6a": "[0x21]",
        "0x11f6e": "[0x21, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55]",
        "0x11f7e": "[0x2c, 0x2e, 0x3e, 0x3f, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55]",
        "0x11f94": "[0x2c, 0x2e, 0x3e, 0x3f]",
        "0x11f9e": "[0x2e, 0x2f, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x47]",
        "0x11fb2": "[0x2e, 0x2f, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x44, 0x45, 0x46, 0x47, 0x48, 0x4d]",
        "0x11fd2": "[0x37, 0x38, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4d]",
        "0x11fe6": "[0x37, 0x38, 0x48, 0x49]",
        "0x11ff0": "[]",
        "0x11ff2": "[]",
        "0x11ff4": "[]",
        "0x11ff6": "[]",
        "0x11ff8": "[0xd, 0xe]",
        "0x11ffe": "[0xd, 0xe, 0xf, 0x10, 0x11, 0x12, 0x25, 0x26]",
        "0x12010": "[0xe, 0xf, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x1f, 0x20, 0x24, 0x25, 0x26]",
        "0x12032": "[0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x1f, 0x20, 0x24, 0x25]",
        "0x1204a": "[0x18, 0x19, 0x1f, 0x21]",
        "0x12054": "[0x18, 0x19, 0x1f, 0x21, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59]",
        "0x1206c": "[0x3f, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59]",
        "0x1207e": "[0x3f]",
        "0x12082": "[0x39, 0x3a, 0x3b, 0x47]",
        "0x1208c": "[0x38, 0x39, 0x3a, 0x3b, 0x44, 0x46, 0x47]",
        "0x1209c": "[0x37, 0x38, 0x44, 0x46, 0x47]",
        "0x120a8": "[0x37, 0x38]",
        "0x120ae": "[]",
        "0x120b0": "[]",
        "0x120b2": "[]",
        "0x120b4": "[]",
        "0x120b6": "[]",
        "0x120b8": "[0x11, 0x12]",
        "0x120be": "[0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x1f, 0x20]",
        "0x120d4": "[0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x1f, 0x20]",
        "0x120e8": "[0x18, 0x19, 0x1f, 0x21]",
        "0x120f2": "[0x18, 0x19, 0x1f, 0x21, 0x56, 0x57, 0x58, 0x59]",
        "0x12104": "[0x3f, 0x56, 0x57, 0x58, 0x59]",
        "0x12110": "[0x3f]",
        "0x12114": "[]",
        "0x12116": "[]",
        "0x12118": "[]",
        "0x1211a": "[]",
        "0x1211c": "[]",
        "0x1211e": "[]",
        "0x12120": "[]",
        "0x12122": "[]",
        "0x12124": "[]",
        "0x12126": "[]",
        "0x12128": "[]",
        "0x1212a": "[]",
        "0x1212c": "[0x19]",
        "0x12130": "[0x19]",
        "0x12134": "[0x3f]",
        "0x12138": "[0x3f]",
        "0x1213c": "[]",
        "0x1213e": "[]",
        "0x12140": "[]",
        "0x12142": "[]",
        "0x12144": "[]",
        "0x12146": "[]",
        "0x12148": "[]",
        "0x1214a": "[]",
        "0x1214c": "[]",
        "0x1214e": "[]",
        "0x12150": "[]",
        "0x12152": "[]",
        "0x12154": "[0x19]",
        "0x12158": "[0x19]",
        "0x1215c": "[0x3f]",
        "0x12160": "[0x3f]",
        "0x12164": "[]",
        "0x12166": "[]",
        "0x12168": "[]",
        "0x1216a": "[]",
        "0x1216c": "[]",
        "0x1216e": "[]",
        "0x12170": "[]",
        "0x12172": "[]",
        "0x12174": "[]",
        "0x12176": "[]",
        "0x12178": "[]",
        "0x1217a": "[]",
        "0x1217c": "[0x19, 0x1a, 0x1b, 0x1c]",
        "0x12186": "[0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x40, 0x41, 0x42, 0x43]",
        "0x1219a": "[0x1c, 0x1d, 0x3f, 0x40, 0x41, 0x42, 0x43]",
        "0x121aa": "[0x3f, 0x40]",
        "0x121b0": "[]",
        "0x121b2": "[]",
        "0x121b4": "[]",
        "0x121b6": "[]",
        "0x121b8": "[]",
        "0x121ba": "[]",
        "0x121bc": "[]",
        "0x121be": "[]",
        "0x121c0": "[]",
        "0x121c2": "[]",
        "0x121c4": "[]",
        "0x121c6": "[]",
        "0x121c8": "[0x19, 0x1a, 0x1b, 0x1c]",
        "0x121d2": "[0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x40, 0x41, 0x42, 0x43]",
        "0x121e8": "[0x1c, 0x1d, 0x1e, 0x3f, 0x40, 0x41, 0x42, 0x43]",
        "0x121fa": "[0x3f, 0x40]",
        "0x12200": "[]",
        "0x12202": "[]",
        "0x12204": "[]",
        "0x12206": "[]",
        "0x12208": "[]",
        "0x1220a": "[]",
        "0x1220c": "[]",
        "0x1220e": "[]",
        "0x12210": "[]",
        "0x12212": "[]",
        "0x12214": "[]",
        "0x12216": "[]",
        "0x12218": "[]",
        "0x1221a": "[0x1d, 0x1e, 0x43]",
        "0x12222": "[0x1d, 0x1e, 0x43]",
        "0x1222a": "[]",
        "0x1222c": "[]",
        "0x1222e": "[]",
        "0x12230": "[]",
        "0x12232": "[]",
        "0x12234": "[]",
        "0x12236": "[]",
        "0x12238": "[]",
        "0x1223a": "[]",
        "0x1223c": "[]",
        "0x1223e": "[]",
        "0x12240": "[]",
        "0x12242": "[]",
        "0x12244": "[]",
        "0x12246": "[]",
        "0x12248": "[]",
        "0x1224a": "[]",
        "0x1224c": "[]",
        "0x1224e": "[]",
        "0x12250": "[]",
        "0x12252": "[]",
        "0x12254": "[]",
        "0x12256": "[]",
        "0x12258": "[]",
        "0x1225a": "[]",
        "0x1225c": "[]",
        "0x1225e": "[]",
        "0x12260": "[]",
        "0x12262": "[]",
        "0x12264": "[]",
        "0x12266": "[]",
        "0x12268": "[]",
        "0x1226a": "[]",
        "0x1226c": "[]",
        "0x1226e": "[]",
        "0x12270": "[]",
        "0x12272": "[]",
        "0x12274": "[]",
        "0x12276": "[]"
      }
    }

# Surface

The surface chunk consists of three parts which each describe some detail about the map tiles.
The first part is the tile character (aka "texture"), the second is the tile surface normal, and the third part is
unknown.

The map consists of 64x64 tiles. In this chunk the tiles are not saved row oder column based. The map is split in 16x16
blocks of 4x4 tiles each. The tiles in these blocks are saved in row major order, and the blocks themselves are saved in
row major order too. Some blocks (normals, unknown) describe the corner points of the tiles, so the blocks are 5x5. 

The surface chunk is used primary when the ground is uneven (hills, slopes) or has different heights (platforms). In this
case the height map in the surface heights chunk describes the geometry.

This chunk is not present when the map uses rotating scrolls as the ground texture.

## Surface Characters

![Surface Characters sara06.mpd](img/03_sara06_surface.png)

The surface characters are always 16x16 pixels and can be partly transparent. Each tile is one uint16 which is the 
character number. In some cases the saturn uses the higher bits in the character number to represent a mirror around
the horizontal or vertical axis. I haven't seen one of these yet.

## Surface Normals

The surface normals are saved as 3 compressed FIXED for each tile.
Note that the blocks are 5x5 here. So the normals are vertex normals and not face normals.

## Unknown Surface Attribute

In the unknown Attribute stuff the blocks are 5x5 each and each element is a single int8. The meaning is not (yet) known.

# Texture Animation Images

When the header has some animated textures, the corresponding images are here. The header points directly to a single 
image (character) into this chunk, which is compressed individually. The position and number of images in the chunk
can only be determined from the header.


# Surface Mesh Chunk

The surface mesh chunk is completely compressed.

This chunk describes the tiles of the map in row major order directly (no blocks as in the (#surface)s).
The size of the map is always 64x64 tiles. The parts are: height map, unknown int16 and unknown int8

## Height Map

Each tiles height of the 4 corners is saved as 4 uint8 (there are no negative heights). Multiply this value by 2 to get
the correct coordinate. Neighbouring tiles do not need to have the same height at the edge, so there can be a "gap" in
the mesh. This gap will either be closed by [objects](#static-meshes) or left open when is cannot be seen during normal
play.

## unknown (maybe slopes)

These are 2 int8 for each tile. The purpose is unknown, but it may be the center of the tile when viewed as a slope.
So the first byte is the center when it is a horizontal slope and the second for a vertical slope. 

## unknown (int8)

Each tile is saved as an int8. The purpose is unknown, but this is normally mostly 0x00. As a wild guess this might
be triggers for scripting in the corresponding map binary (x1*.bin).

# Texture Chunk

The texture Chunk is compressed, inclusive header and texture definition list. Up to 4 texture chunks are possible.
The header in each chunk defines how many textures are in the chunk and the id of the first texture.
Normally the texture ids are strictly ascending without holes.

Unused texture chunks are present (size > 0), but the num_textures are 0.

![Texture Sheet sara06.mpd](img/04_sara06_textures.png)

## Texture Chunk Header

| Offset | Name            | Type      | Count        | Description
|--------|-----------------|-----------|--------------|-----------------------------
| 0x00   | num_textures    | int16      |   1          | number of  textures in chunk
| 0x02   | texture_id_start| int16      |   1          | start id of the textures in this block.
| 0x04   | texture_def[]   | TextureDefinition | num_textures | texture definitions
| ???    | texture_data    | byte      |   ?          | texture data         

## Texture Definition

Size: 4 byte

| Offset | Name     | Type | Count | Description
|--------|----------|------|-------|-----------------------------
| 0x00   | width    | uint8 |   1   | width of texture
| 0x01   | height   | uint8 |   1   | height of texture
| 0x02   | offset   | int16 |   1   | byte offset of texture in decompressed data stream


# Scroll Panes

## Saturn Capabilities

The Sega Saturn has two kinds of scroll panes: normal scroll panes and rotating scroll panes.
The normal scroll panes are 2d scrolls which can scale and shrink in any direction, rotate around the screen axis
and, well, scroll. These panes are used as skyboxes in battles.

The rotating scroll pane is a 3d scroll which can rotate into the screen and has a correct perspective
transformation applied. It can be viewed as a single huge quadratic polygon. Most games (inclusive SF3) use these as
the ground texture. Note that there can be only one rotating scroll pane. For further details see [1].

The scroll pane content of both types can be supplied in two formats: cell format and bitmap format.
The bitmap format is simply the pixels row major order, either rgb16 or paletted format. In SF3 only the 256 colors 
palette format is used.
The cell format places character images in a repeating pattern to form a very big image. The exact format is described 
in [1].

## Scroll Pane Chunks

The scroll pane chunks are always compressed.

The chunks can have three different kind of data: character images, pattern name data or bitmap data.
Character images are simply 8x8 pixels 256 colors characters. The characters I've encountered always used the first 
palette. Most often the first two chunks are character data.

The pattern name data chunks contains the character numbers for a page (see [1]). Sf3 used the word format, where the 
least significant 12 bits are the character number. The upper 4 bits should be flip configuration, but SF3 seems not to
use them. One chunk contains 8 pages with 64x64 characters each. 
Most often the third chunk contains the pattern name data and sometimes the 6th chunk contains another pattern name 
data table. 
Implementation note: The character indices must be shifted right one bit, so only bits 1..11 is used for character id.
The lsb is always zero. Is the first bit the palette id?

TODO: Somehow the map must be configured. A map is an array of 4x4 pages. The location of this configuration is not 
known. The scroll panes are sometimes animated (water), the animation configuration is unknown too.

Bitmap chunks are raw 512x128 images in 256 color. These are mostly used .
Bitmaps in chunk 4 are skyboxes and are battle backgrounds (I think, there are still issues, see examples). Skyboxes mostly use the second palette. 
Bitmaps in chunk 0 and 1 are used as floor texture, for example grass and static water. Bitmaps floor doesn't seem to 
be animated. Floor bitmaps mostly use the first palette.

TODO: the bitmap scroll pane needs at least 512x256 pixels, which can be seen in the game. The lower half of the 
background is not encoded in the map file. Where is the lower half of the image? Wild guess: x2*.bin files.

## Examples

![Cells of sara02.mpd](img/05_sara02_cells.png)

Cells of sara02.mpd, scroll pane chunk 0 and 1, in columns of 4. On top of the column the hexadecimal tile id of the
first tile is shown. The white areas are part of the file, they are zeroed out (or rather 0xFE) in the file too.

![First page of sara02.mpd](img/06_sara02_page0.png)
![Second page of sara02.mpd](img/07_sara02_page1.png)

![water animation sara02.mpd](img/08_sara02_water.gif)

First and second page of the rotating scroll screen of sara02.mpd. The pages in sara02 form an animation, so presumably
the scroll map is filled with 4x4 of the same page. Every few frames the next page is used in the scroll map and this
way the animation is created.

![Skybox from sara06.mpd](img/09_sara06_skybox.png)

![Skybox from in game from bridge battle (taken with yabause)](img/10_sara06_ingame_skybox.png)

Skybox from the file sara06.mpd chunk 4 (512x128) and taken from the game with yabause (NBG0, 320x320). Note the mountain in the
background which matches in both files, whereas in the game there are some buildings and crates which are not in the mpd file. 

# Issues and open points

* document header
* read list at offset6 and offset7. used in sara02 and sara04
* plot small values (< 0x100) throughout the various lists so similarities can be found.


# References

[1] VDP2 User Manual (ST-058-R2-060194)

