# Description

The mpd files (Map Data) contain the 3d map data for normal travel and battle. Maps consist of

* Meshes like buildings, trees
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
| 0x00     | header       | Header     |   1    | File header. See description in [Chapter Header](#header)
| 0x2000   | chunk_table  | ChunkTable |   1    | Table with chunk offsets and sized. See description in [Chapter Chunks](#chunks)
| 0x2100   | chunks       | Chunk[]    | 0..20  | At most 20 chunks. See description in [Chapter Chunks](#chunks)


# Header

The total header size is always 0x2000 bytes. Unused bytes
are padded with zero.

## File header: size: 12 bytes

| Offset | Name    | Type  | Count | Description |
|--------|---------|-------|------|-------------|
| 0x00   | offset1 | dword |  1   | first header indirection. pointer to sub header
| 0x04   |         | dword |  2   | zero (0x0). padding? 



## Sub Header: size: 4 bytes

| Offset  | Name    | Type  | Count | Description |
|---------|---------|-------|------|-------------|
| 0x00    | offset2 | dword |  1   | second header indirection. pointer to sub sub header


## Sub Sub Header: size:  0x58 (88) bytes
 
| Offset  | Name    | Type  | Count | Description |
|---------|---------|-------|------|--------------|
|  0x00   |         | word  |  1   | two small bytes. maybe 2 counts
|  0x02   |         | word  |  1   | zero 0x0000
|  0x04   | offset1 | dword |  1   | always 0x0c. pointer to 0x20 unknown words at the start of the file.
|  0x08   | offset2 | dword |  1   | always 0x4c. pointer to unknown dword at the start of the file.
|  0x0C   | offset3 | dword |  1   | always 0x50. pointer to 0x20 unknown words at the start of the file. Mostly zero or 0x8000
|  0x10   |         | dword |  1   | unknown. count? maybe two words
|  0x14   | offset4 | dword |  1   | always 0x90. 4 bytes, always 0xffff
|  0x18   | offset_texture_groups | dword |  1   | offset to list of texture groups
|  0x1C   | offset6 | dword |  1   | pointer to unknown list. 
|  0x20   | offset7 | dword |  1   | pointer to unknown list.
|  0x24   | offset8 | dword |  1   | pointer to #polydata_list @see "Polydata List". the list contains 2 entries. may be null 
|  0x28   | offset9 | dword |  1   | pointer to #polydata_list @see "Polydata List". the list contains 2 entries. may be null
|  0x2C   |         | dword |  1   | always zero 0x0 
|  0x30   |         | dword |  1   | const 0x8000b334
|  0x34   |         | dword |  1   | const 0x4ccc0000
|  0x38   | offset10| dword |  1   | sometimes pointer to list of size 0x10, sometimes unused 
|  0x3C   | offset11| dword |  1   | pointer to list of 0x100 words. these may be FIXED
|  0x40   | offset12| dword |  1   | pointer to list of 0x100 words. looks similar to offset11. may be unused.
|  0x44   |         | dword |  1   | small value, may be negative
|  0x48   |         | dword |  1   | const 0xc000
|  0x4C   |         | dword |  1   | zero 
|  0x50   |         | dword |  1   | unknown. small value in upper word, 0x0000 in lower word. 
|  0x54   | offset12| dword |  1   | pointer to unknown list 


## Polydata List: size: 0x1c (28) bytes
The polydata list is optional and may be missing.
Sometimes the data is in the file, but there is no pointer
to the structure in the header, so it is "dangling". 
The list usually contains 2 of these entries. 
 
| Offset | Name              | Type  | Count | Description |
|--------|-------------------|-------|-------|--------------|
| 0x00   | offset_polydata_1 | dword |   1   | pointer to PDATA (see SGL)
| 0x04   |                   | dword |   6   | unknown. maybe FIXED.


## Texture Animations (size: 8 bytes + texture animation frames)

Index into Texture Chunk textures[0]. This is a list which is delimited by a texture group 
of 0xffff. In case of a texture group of 0xffff no further data is read.

| Offset | Name           | Type  | Count | Description |
|--------|----------------|-------|-------|--------------|
| 0x00   | texture_group  | word  |   1   | Texture number for SGL.
| 0x02   | texture_width  | word  |   1   | width of textures in this group
| 0x04   | texture_height | word  |   1   | height of textures in this group
| 0x06   |                | word  |   1   | unknown small value. maybe animation speed
| 0x08   | frames[]      | TextureFrame |   ?   | List of Texture Animation Frames.

## Texture Frame (size: 4 bytes)

List of textures in Animation. Please note that this offsets into the 
*compressed* data. Only one texture image is compressed in this entry.
The end of the list is denoted by an offset of 0xfffe. When encountering this
end-of-list marker stop processing the list, don't read the unknown word. 

| Offset | Name    | Type  | Count | Description |
|--------|---------|-------|-------|-------------|
| 0x00   | offset  | word  |   1   | offset into compressed data
| 0x02   | unknown | word  |   1   | unknown small value


## Structure of header (everything < 0x2000)
<!-- language: lang-none -->
    +-------------------------------------------+
    | #start_of_file (0x00)                     |
    | - size always 0xc bytes                   |
    | - offset to #subheader                    |
    +-------------------------------------------+
    | #list1 (0xc)                              |
    | - size always 0x40 bytes                  |
    | #list2 (0x4c)                             |
    | - size always 0x4 bytes                   |
    | #list3 (0x50)                             |
    | - size always 0x40 bytes                  |
    | #list4 (0x90)                             |
    | - size always 0x4 bytes                   |
    | #list5                                    |
    | #list6                                    |
    | #list7                                    |
    | #list12                                   |
    | #list10                                   |
    | #list11                                   |
    +-------------------------------------------+
    | #subsubheader                             |
    | - offset to #list1                        |
    | - offset to #list2                        |
    | - offset to #list3                        |
    | - offset to #list4                        |
    | - offset to #list5                        |
    | - offset to #list6                        |
    | - offset to #list7                        |
    | - offset to #list8                        |
    | - offset to #list9                        |
    | - offset to #list10                       |
    | - offset to #list11                       |
    | - offset to #list12                       |
    +-------------------------------------------+
    | #subheader                                |
    | - offset to #subsubheader                 |
    +-------------------------------------------+
    |  - this block is optional and may be      |
    |    missing                                |
    |  +-------------------------------------+  |
    |  | #vertices_pdata1                    |  |
    |  |  - list of #SGL_POINT for #pdata1   |  |
    |  | #faces_pdata1                       |  |
    |  |  - list of #SGL_POLYGON for #pdata1 |  |
    |  | #face_attributes_pdata1             |  |
    |  | - list of #SGL_ATTR  for #pdata1    |  |
    |  | #pdata1 (#SGL_PDATA)                |  |
    |  +-------------------------------------+  |
    |  | #vertices_pdata2                    |  |
    |  | #faces_pdata2                       |  |
    |  | #face_attributes_pdata2             |  |
    |  | #pdata2 (#SGL_PDATA)                |  |
    |  +-------------------------------------+  |
    |  | #Polydata_List1                     |  |
    |  +-------------------------------------+  |
    |                                           |
    |  +-------------------------------------+  |
    |  | #vertices_pdata1                    |  |
    |  |  - list of #SGL_POINT for #pdata1   |  |
    |  | #faces_pdata1                       |  |
    |  |  - list of #SGL_POLYGON for #pdata1 |  |
    |  | #face_attributes_pdata1             |  |
    |  | - list of #SGL_ATTR  for #pdata1    |  |
    |  | #pdata1 (#SGL_PDATA)                |  |
    |  +-------------------------------------+  |
    |  | #vertices_pdata2                    |  |
    |  | #faces_pdata2                       |  |
    |  | #face_attributes_pdata2             |  |
    |  | #pdata2 (#SGL_PDATA)                |  |
    |  +-------------------------------------+  |
    |  | #Polydata_List2                     |  |
    |  +-------------------------------------+  |
    +-------------------------------------------+


# Chunks

Chunks are memory blocks which may be compressed and are placed in specific memory locations, depending on the content.
The chunks are always in the same order, but not all files need all chunk types. 

| Chunk Index | Name                     | Description
|-------------|--------------------------|------------
|    0        | empty_1                  | This chunk always seem to be empty (size 0).
|    1        | static_objects           | Static map objects. Mesh data and position/rotation/scale.
|    2        | surface                  | Surface texture, surface normals and an unknown surface attribute. The chunk is compressed
|    3        | texture_animation_images | Images for animated textures. Each image is individually compressed.
|    4        | empty_2                  | This chunk always seem to be empty (size 0).
|    5        | surface_meshes           | Heightmap for each surface tile and some unknown surface attributes. Might be walkmesh and movement costs. The chunk is compressed.
|  6 - 10     | textures                 | Texture images. The chunks are compressed.
| 11 - 13     | object_textures          | Textures for moveable objects (see [Header](#header)). The chunks are compressed.
| 14 - 19     | scroll_panes             | Memory blocks for scroll panes (skybox) and rotating scrolls (ground). The chunks are compressed.

## Chunk Table

Size: 20 * 8 bytes (160 Bytes)

Always starts at  offset 0x2000. The table only contains the offsets and the sizes. Unused chunks have the offset
of the next chuck (so, the offset of the next free location in the file) and a size of zero.

The first chunk starts at 0x2100. As the first chunk is always [static objects](#static-objects), this is the chunk at
0x2100.

| Offset | Name               | Type           | Count | Description
|--------|--------------------|----------------|-------|-----------------------------
| 0x00   | chunk_offsets      | offset[]       |  20   | offsets and sizes of the chunk.


## Offset

Size: 8 bytes

| Offset | Name    | Type   | Count | Description
|--------|---------|--------|-------|-----------------------------
| 0x0    | offset  | dword  |   1   | offset to chunk
| 0x4    | size    | dword  |   1   | size of chunk



# Static Objects

Describes the static meshes in the maps. These are mostly buildings, fences, trees and similar. Some maps use static
objects for floor tiles too.

Note: This whole file is normally loaded to 0x290000, so this chunk will be at offset 0x292100 and all offsets in this
chunk are relative to 0x292100. In some maps only this chunk is loaded to 0x60a0000, so you have to adjust the
logic to make these offsets relative to the file.

## Header

| Offset | Name         | Type      | Count       | Description
|--------|--------------|-----------|-------------|-----------------------------
| 0x00   | offset1      | dword     |   1         | offset to unknown stuff. see (#offset1)
| 0x04   | offset2      | dword     |   1         | offset to unknown stuff. see (#offset1)
| 0x08   | num_objects  | word      |   1         | number of objects 
| 0x0A   |              | word      |   1         | padding, 0x00
| 0x0C   | model_head[] | ModelHead | num_objects | for each model a [Model Head](#model-head) structure.    


## Model Head

Size: 0x3C (60) bytes

| Offset | Name            | Type      | Count | Description
|--------|-----------------|-----------|-------|-----------------------------
| 0x00   | pdata_offsets[] | dword     |   8   | 8 offsets to #PDATA structures. These pdata points to the same mesh and different face attributes (#ATTR).
| 0x20   |                 | dword     |   2   | unknwon dwords. They do look like fixed point values.
| 0x28   | orientation     | ANGLE     |   2   | unknown, maybe rotation. 2nd value may be unused (padding)
| 0x2C   | scale           | FIXED     |   4   | always 1.0 1.0 1.0 0.0 (or very close). maybe scale?



## Structure of Map Objects chunk
<!-- language: lang-none -->
    +----------------------------+
    | #start_chunk (0x2100)      |
    | - offset to #offset1       |
    | - offset to #offset2       |
    | - num_objects              |
    | +------------------------+ |
    | | ModelHead[0]           | |
    | | - 8 x offset to #PDATA | |
    | +------------------------+ |
    | | ModelHead[1]           | |
    | +------------------------+ |
    | | ...                    | |
    | +------------------------+ |
    | | ModelHead[num_objects-1] |
    | +------------------------+ |
    |                            |
    | +------------------------+ |
    | | #PDATA                 | |
    | | #POINT                 | |
    | | #POLYGON               | |
    | | #ATTR                  | |
    | +------------------------+ |
    | | #PDATA                 | |
    | | #POINT                 | |
    | | #POLYGON               | |
    | | #ATTR                  | |
    | +------------------------+ |
    | ...
    | +------------------------+ |
    | | #offset1               | |
    | |  - offset to #offset1a | |
    | |  - offset to #offset1b | |
    | +------------------------+ |
    | | #offset1a              | |
    | +------------------------+ |
    | | #offset1b              | |
    | +------------------------+ |
    | +------------------------+ |
    | | #offset2               | |
    | |  - list of #offsets2a[]| |
    | |    exactly 0x100       | |
    | +------------------------+ |
    | | #offsets2a[0]          | |
    | |  - words terminated by | |
    | |     0xffff             | |
    | +------------------------+ |
    | | #offsets2a[1]          | |
    | +------------------------+ |
    | | ...                    | |
    | +------------------------+ |
    | | #offsets2a[0xff]       | |
    | +------------------------+ |
    | #end of chunk              |
    +----------------------------+
    
Notes:

    sara02.mpd
    0x141cc end of model heads:
    
    0x381a8 -- pdata
    0x381bc -- vertices
    0x382dc -- face
    0x3854c -- attr
    0x38594 -- 0x8 - #offset1
      0x3859c -- 0x2C0 - #offset1a - these look like words
      0x3885c -- 0x5C8 - #offset1b - unknown
    
    0x38e24 -- 0x400 - #offset2: list of 100 pointers
    0x39224 -- end of pointer list
      -- these pointers are word lists terminated by 0xffff 
      0x39224 => 0xffff
      ...
      0x396de - 0x16 (20)
      0x396f4 - 0x2a (42)
      0x3971e
      ...
      0x39bd6 - last list
    0x39bd8 - end of chunk - start  of new chunk



# Map Surface @ 0x2100 (@see Chunk Dictionary #offset_surface1)

## Header
| Offset | Name         | Type      | Count       | Description
|--------|--------------|-----------|-------------|-----------------------------
| 0x00   | offset       | dword     |   1         | unknown


## MiniTile (size: 16 x 2 = 32 bytes)

Each MiniTile is a word. The tile is 4x4 "pixels".
The order is as follows:

    +----+----+----+----+
    | 12 | 13 | 14 | 15 |
    +----+----+----+----+
    |  8 |  9 | 10 | 11 |
    +----+----+----+----+
    |  4 |  5 |  6 |  7 |
    +----+----+----+----+
    |  0 |  1 |  2 |  3 |
    +----+----+----+----+

# Compressed Streams

Some Chunks use some kind of RLE compression.

The chunk is divided in "lines", which consists of a word "control" and 
16 words data. One bit in control corresponds to one word of the data, 
starting with msb of control. Cleared bit in control means "data word",
set bit in control means "command word".

# Texture Chunk

The texture Chunk is compressed.

## header
| Offset | Name            | Type      | Count        | Description
|--------|-----------------|-----------|--------------|-----------------------------
| 0x00   | num_textures    | word      |   1          | number of  textures in chunk
| 0x02   | texture_id_start| word      |   1          | start id of the textures in this block.
| 0x04   | texture_def[]   | TexDef    | num_textures | texture definitions
| ???    | compressed_data | byte      |   ?          | compressed texture data         

## TexDef (size: 4 byte)
| Offset | Name     | Type | Count | Description
|--------|----------|------|-------|-----------------------------
| 0x00   | width    | byte |   1   | width of texture
| 0x01   | height   | byte |   1   | height of texture
| 0x02   | offset   | word |   1   | byte offset of texture in decompressed data stream




| block      | sara02           | nasu00            | sara06
|------------|------------------|-------------------|-----------------
|unknown     |  0x2100: 0x0     |  0x2100: 0x0      |  0x2100: 0x0
|mapObjects  |  0x2100: 0x37ad8 |  0x2100: 0x39c8   |  0x2100: 0x10178
|surfaceData | 0x39bd8: 0x0     |  0x5ac8: 0xcf00   | 0x12278: 0xcf00
|texture_anims | 0x39bd8: 0x4684  | 0x129c8: 0x0      | 0x1f178: 0x6518
|textures[0] | 0x3e25c: 0x0     | 0x129c8: 0x0      | 0x25690: 0x0
|textures[1] | 0x3e25c: 0x49a   | 0x129c8: 0x412    | 0x25690: 0x450
|textures[2] | 0x3e6f8: 0x7050  | 0x12ddc: 0x5a40   | 0x25ae0: 0x7596
|textures[3] | 0x45748: 0x7f5c  | 0x1881c: 0x43a0   | 0x2d078: 0x7a44
|textures[4] | 0x4d6a4: 0x7602  | 0x1cbbc: 0x8      | 0x34abc: 0x7b26
|textures[5] | 0x54ca8: 0x7e26  | 0x1cbc4: 0x8      | 0x3c5e4: 0x4136
|textures[6] | 0x5cad0: 0x38c   | 0x1cbcc: 0x8      | 0x4071c: 0x8
|textures[7] | 0x5ce5c: 0x0     | 0x1cbd4: 0xae0    | 0x40724: 0xae0
|textures[8] | 0x5ce5c: 0x0     | 0x1d6b4: 0x942    | 0x41204: 0x942
|textures[9] | 0x5ce5c: 0x0     | 0x1dff8: 0x494    | 0x41b48: 0x494 (this is the last chunk with vertical lines.)
|textures[10]| 0x5ce5c: 0x8692  | 0x1e48c: 0x10f8   | 0x41fdc: 0xbdde
|textures[11]| 0x654f0: 0x86f0  | 0x1f584: 0x10f0   | 0x4ddbc: 0xbdde
|textures[12]| 0x6dbe0: 0x10d4  | 0x20674: 0x0      | 0x59b9c: 0x0
|textures[13]| 0x6ecb4: 0x0     | 0x20674: 0x844    | 0x59b9c: 0x844
|textures[14]| 0x6ecb4: 0x0     | 0x20eb8: 0x4fee   | 0x5a3e0: 0x483e
|textures[15]| 0x6ecb4: 0x844   | 0x25ea8: 0x22e    | 0x5ec20: 0x0
|textures[16]| 0x0 : 0x0        | 0x0 : 0x0         | 0x0 : 0x0


