# Notes

Battle meshes always have the extension ".bin", and the files start with "x8pc".

# File Structure

    +--------------------------+
    | Header (0x800 bytes)     |
    +--------------------------+
    | Texture Definitions      |
    |                          |
    +--------------------------+
    | Compressed Texture Data  |
    |                          |
    +--------------------------+
    | Meshes                   |
    |    Header                |
    |    Body Meshes           |
    |    Weapon Mesh           |
    |    Skeleton              |
    +--------------------------+
    | Animation                |
    |    Header                |
    |    Animation Description |
    |    Keyframes             |
    +--------------------------+

# Header (0x40 bytes)

| Offset | Name                | Type       | Count | Description
|--------|---------------------|------------|-------|-----------------------------
| 0x00   | textureDefinitions  | ChunkEntry |   1   | Description of textures
| 0x10   | textures            | ChunkEntry |   1   | Compressed Texture Data
| 0x20   | meshes              | ChunkEntry |   1   | Mesh Data (vertices, faces, normals, attributes)
| 0x30   | animations          | ChunkEntry |   1   | Animation Data (Keyframes)
| 0x40   | padding             | uint8      |   ?   | padding to start of first chunk

# ChunkEntry (0x10 bytes)

| Offset | Name       | Type   | Count | Description
|--------|------------|--------|-------|-----------------------------
| 0x00   | offset     | uint32 |   1   | absolute offset in file 
| 0x04   | data_size  | uint32 |   1   | number of data bytes in chunk 
| 0x08   | chunk_size | uint32 |   1   | size of chunk (incl. padding)
| 0x0C   | padding    | uint32 |   1   | padding to next 0x10 boundary. always zero.


# Texture Definitions (0x14 bytes + texture defs)

| Offset | Name                | Type       | Count        | Description
|--------|---------------------|------------|--------------|-----------------------------
| 0x00   | header_size         | uint32     |   1          | size of this header
| 0x04   | num_textures        | uint32     |   1          | number of used textures
| 0x08   | vram_size           | uint32     |   1          | size of decompressed textures in vram
| 0x0C   | padding             | uint32     |   2          | padding, always zero
| 0x14   | texture_defs[]      | TEXTURE    | num_textures | TEXTURE structures (see [4], page 10) 
| ??     | pading              | uint8      |  ??          | padding to start of next chunk. always zero.

# Texture

Decompress the texture Chunk. The texture starts at the adjusted 
VRAM adress (CGadr >> 3) of the TEXTURE structure. 
All textures are 16 bit rgb.

The textures are compresses with RLE. 
The encoded/compressed data is split in 0x11 (17) words: one control word and 0x10 data words.
Each bit in the control word determines if the corresponding data word is data or a copy command. 
The order of the bits in the control word is msb to lsb.

* control word bit == 0: copy corresponding word to output buffer
* control word bit == 1: stream copy. read corresponding word as command.
  * command word = 0x0000? End of stream.
  * split command word bits: oooo oooo oool llll
    * o (11 bits) is negative offset in word boundary. so you have to multiply with 2 to get a (negative) byte offset 
    * l (5 bits) is length in words. to get the copy length in bytes mutliply with 2

Note that this (de)compression is independent of texture size/type. You can view this as a stream decompress. 

# Mesh Header (0x14 bytes)

| Offset | Name                | Type       | Count        | Description
|--------|---------------------|------------|--------------|----------------------------
| 0x00   | header_size         | uint32     |   1          | header size, always 0x08
| 0x04   | offset_skeleton     | uint32     |   1          | offset to skeleton
| 0x08   | offset_body_mesh    | uint32     |   1          | offset to body meshes (list of XPDATA)
| 0x0C   | offset_weapon_mesh  | uint32     |   1          | offset to weapon mesh (XPDATA)
| 0x10   | padding             | uint32     |   1          | padding, always zero

# Meshes

Notes: 
* body_mesh and weapon_mesh point to XPDATA ([4], page 3) structures.
* All vertices in the mesh are fixed point values ([3], page 1-8).
* Polygons are always quads ([3], page 2-7). For triangles two vertices share the same coordinates.
* Textures (called characters in vdp1) can only applied as 
  a whole to the quads, there are no texture coordinates (7-7).
* The texno field in ATTR ([4], page 1) indexes into the texture
  definitions above, the dir field mirrors the texture in x or y or 
  both directions. (see also [1] page 77)
* when ATTR#dir indicates a non textured polygon, ATTR#colno 
  indicates the 16 rgba bit color.
* TODO: end code processing and transparent pixel processing
  (end code see [1] page 86, transparent pixel see [1] page 88)
* TODO: color calculation ([1] page 93), mesh enable ([1], page 85)

# Skeleton

The skeleton is based on `slPushMatrix (void)`and `slPophMatrix (void)` from SGL (see [3] Chapter 5 Matrices).
The skeleton is build by commands which represent push matrix 
(together with translation and rotation interpolated along keyframes), pop matrix, draw mesh and draw external mesh.

Algorithm:
* initialize bone_index to 0
* initialize mesh_index to 0
* create empty bone stack
* create root bone, add root bone to bone stack
* read byte from skeleton stream
  * byte == 0xfd? push matrix
    * create new bone with index `bone_index`
    * increase `bone_index`
    * add new bone as child to bone on top of stack
    * add new bone to bone stack
  * byte == 0xfe? pop matrix
    * pop bone from bone stack and discard the bone
    * is the stack empty? exit alghorithm. return root bone
  * byte == 0x00 draw mesh
    * add/draw mesh `mesh_index` to bone at top of stack
    * increase `mesh_index`
  * byte is in (tag with location)? draw external mesh
    * (this is some kind of tag for additional meshes)
    * skip to 32bit boundary. the skipped bytes are always 0x00
    * read POINT (3 x FIXED). This is the translation part for the tag
  * byte is (tag with location, rotation and scale)? draw external mesh with rotation and scale
    * skip to 32bit boundary. the skipped bytes are always 0x00
    * read translation: POINT (3 x FIXED)
    * read rotation quarternion: 4 x FIXED (x,y,z, w) 
    * read scale: 3 x FIXED
  
## Notes

The saturn probably  used this structure to directly draw the model. Maybe this is easiest reproduced
with OpenGL commands.

Tag positions are absolute as are the mesh vertices. The preceding bone may animate the tag.

Rotation is a quaternion. In contrast to the animations the rotation is a full 32bit FIXED here.

Known Tags:

| Tag | Transformations  | Description
|------|-----------------|-------------
| 0x01 | loc             | unknown
| 0x09 | loc             | tag_chest
| 0x10 | loc             | tag_weapon_base
| 0x11 | loc             | tag_weapon_tip
| 0x12 | loc             | unknown
| 0x20 | loc             | tag_hand? nearly the same as 30 (tag_weapon), but without rotation/scale
| 0x21 | loc             | unknown
| 0x22 | loc             | unknown
| 0x30 | loc, rot, scale | tag_weapon (translation, rotation, scale)


Unknown Command:
* 0x80 - no transformations (so no padding). Might be a "draw mesh" command or some kind of tag.

# Animation Chunk

The animations are split into two files: each model has a corresponding x8an*.bin file [7].
The first block in the animation chunk is the animation description, so which keyframe range to play for which 
animation. The second block is an embedded keyframe animation table.

## Header 

| Offset | Name                     | Type        | Count        | Description
+--------+--------------------------+-------------+--------------+-----------------------------
| 0x00   | bounding_box?            | FIXED       |   6          | 6 fixed floats, which may or may not be some kind of bounding box
| 0x18   | offset_bone_keyframe     | int32       |   1          | relative offset to BoneKeyframe list (see also [7])
| 0x1C   | animation_events?        | int32       |   0..n       | list of Animation objects with events (see [5])
|   ??   | end_marker               | int32       |   1          | always 0xffffffff (-1). Denotes end of animations list.

### Notes

The bounding_box? field does not correspond to the vertices, so it is not exactly a mesh bounding box. But the first
3 floats (x1, y1, z1) are always smaller than the corresponding second 3 floats (x2, y2, z3). It seems that y1 is always
the negative value of y2.

Example x8pc00a.bin (Synbios):
"bounding_box_min": "[ 0.3832 , -10.9194 , 0.1782 ]",
"bounding_box_max": "[ 4.6951 , 10.9194 , 3.5723 ]",


## Animation

| Offset | Name                     | Type        | Count        | Description
|--------|--------------------------|-------------|--------------|-----------------------------
| 0x00   | start_frame              | uint16      |   1          | start of frame for animation
| 0x02   | number_of_frames         | uint16      |   1          | number of frames for the animation
| 0x04   | type                     | uint16      |   1          | type of animation
| 0x06   | distance_from_enemy      | uint16      |   1          | distance to place the character from the enemy
| 0x08   | offset_events            | uint32      |   1          | relative offset to AnimationEvent list 

Notes:
* see also [5]
* one animation "stream" can contain multiple animations. `start_frame` and `number_of_frames` select the part
  of the animation stream which should be played.
* some animations are placed in the corresponding x8an* file. In this case the 12th bit is set. Bitmask: 0x1000
* the animation group is selected by the mesh file, see discussion in [7]

## Known Animations:

| Type | Description     |
|------|-----------------|
| 0x00 | idle            |
| 0x10 | hit             |
| 0x20 | block           |
| 0x30 | unknown         |
| 0x40 | unknown         |
| 0x50 | unknown         |
| 0x60 | ranged attack?? |
| 0x70 | attack          |
| 0x80 | unknown         |
| 0x90 | unknown         |
| 0xa0 | unknown         |

## Some notes:

| Character | type      | Frames     | Animation name
|-----------|-----------|------------|----------------
| Synbios a |  0x00     |   1 -  35  | idle
| Synbios e |  0x10     |  35 -  85  | attack
| Synbios a |  0x20     |  85 - 105  | block
| Synbios a |  0x70     | 105 - 133  | hit
|-----------|-----------|------------|----------------
| Synbios e |  0x00     |   1 -  35  | idle
| Synbios e |  0x10     |  35 -  85  | attack
| Synbios e |  0x20     |  85 - 105  | block
| Synbios e |  0x70     | 105 - 133  | hit

* frame start + frame count is exclusive, so the last frame is not played


## AnimationEvent

| Offset | Name                     | Type        | Count        | Description
|--------|--------------------------|-------------|--------------|-----------------------------
| 0x00   | frame                    | uint16      |   1          | frame at which the event occurs
| 0x02   | eventCode                | uint16      |   1          | event id to trigger

Note:
* a 0xffff denotes the end of the animation event list.
* the event code may be some sound trigger or similar.

# BoneKeyframes

The bone keyframes are in the same format as the animations in [7]. Note that the offsets are relative to the animation
chunk start.

# Notes & Discussions

## Mesh Names

| Nr | Character
|-----|-------------
| 00a | Synbios Sword
| 00b | Synbios Rapier
| 00c | Synbios Blade
| 00d | Synbios Knife (note: no useable weapon tag)
| 00e | Synbios Sword
| 00f | Synbios Rapier
| 00g | Synbios Blade
| 00h | Synbios Knife (note: no useable weapon tag)
|-----|-------------
| 01a | Dantares Lance
| 01b | Dantares Spear  (note: no useable weapon tag)
| 01c | Dantares Halberd
| 01e | Dantares Lance
| 01f | Dantares Spear  (note: no useable weapon tag)
| 01g | Dantares Halberd
|-----|-------------
| 02a | Masquirin Rod
| 02b | Masquirin Wand
| 02c | Masquirin Ankh
| 02e | Masquirin Rod
| 02f | Masquirin Wand
| 02g | Masquirin Ankh
|-----|-------------
| 03a | Grace Rod (note: does not import correctly)
| 03b | Grace Wand (note: no useable weapon tag)
| 03c | Grace Ankh
| 03e | Grace Rod
| 03f | Grace Wand
| 03g | Grace Ankh
|-----|-------------
| 04 | Hayward
| 05 | Obright
| 06 | Irene
| 07 | Julian
| 08 | Cybel
| 09 | Eldar
| 10 | Kahn
| 11 | Noon (promoted only)
| 12 | Justin (promoted only)
| 13 | Horst (promoted only)
| 14 | Pen
| 15 | Ratchet
| 16 | Frank
| 17 | Hagane
| 18 | Murasame
| 19 | Fynnding
|-----|-------------
| 700 | Bat/Dragon (note: skeleton is not imported correctly)
| 701 | Soldier with pike
| 702 | Heavy Soldier with Axe
| 704 | Horse Knight with Lance
| 705 | Horse Knight with Lance
| 707 | Horse Knight with Bow
| 709 | Knight with Lance 
| 713 | Heavy Knight with Mace
| 715 | Lizard with Axe
| 717 | Hell Hound
| 718 | Cerberus
| 719 | Goblin with Knife
| 720 | Dwarf with Axe
| 721 |
|...|...
| 900 | Dragon
| 901 | Strange head
| 902 | Mage
| 903 | Collosus one arm
| 904 | Collosus Head


## Mesh Parts

Names for some meshes. The first number is the mesh index in the order from the mesh chunk.

### x8pc00a.bin (Synbios + Sword)

List of meshes:
* 0 - hip
* 1 - upper_leg.R
* 2 - lower_leg.R
* 3 - foot.R
* 4 - upper_leg.L
* 5 - lower_leg.L
* 6 - foot.L
* 7 - cape? (cape on back)
* 8 - torso
* 9 - upper_arm.R
* 10 - lower_arm.R
* 11 - hand.R
* 12 - finger.R
* 13 - upper_arm.L
* 14 - lower_arm.L
* 15 - hand.L
* 16 - finger.L
* 17 - shield
* 18 - head
* weapons mesh - sword

### x8pc01a.bin (Dantares + Lance)

00 - front_hip
01 - torso
02 - head
03 - upper_arm.R
04 - lower_arm.R
05 - hand.R
06 - upper_arm.L
07 - lower_arm.L (with shield)
08 - hand.L
09 - front_upper_leg.L
10 - front_lower_leg.L
11 - front_foot.L
12 - front_upper_leg.R
13 - front_lower_leg.R
14 - front_foot.R
15 - rear_hip
16 - tail
17 - rear_shoulder.L
18 - rear_upper_leg.L
19 - rear_lower_leg.L
20 - rear_foot.L
21 - rear_shoulder.R 
22 - rear_upper_leg.R
23 - rear_lower_leg.R
24 - rear_foot.R
weapon mesh - lance

# Skeleton Discussion

Skeleton of Synbios:

root:
  0xfd - bone 0
    0xfd - bone 1
    0x00 - mesh 0 (hip)
      0xfd - bone 2
      0x00 - mesh 1 (upper_leg.R)
        0xfd - bone 3
        0x00 - mesh 2 (lower_leg.R)
          0xfd - bone 4 
          0x00 3 - foot.R
        0xfe
      0xfe
    0xfe
      0xfd - bone 5
      0x00 - mesh 4 (upper_leg.L)
        0xfd - bone 6
        0x00 - mesh  5 (lower_leg.L)
          0xfd - bone 7
          0x00 - mesh  6 (foot.L)
        0xfe
      0xfe
    0xfe
  0xfe (torso is one level less than hip)
    0xfd - bone 8
      0xfd - bone 9
      0x00 - mesh 7 (torso)
    0xfe
    0x00  - mesh 8 (cape on back)
      0xfd - bone 10
        0xfd - bone 11
        0x00 - mesh 9 (upper_arm.R)
          0xfd - bone 12
          0x00 - mesh 10 (lower_arm.R)
            0xfd - bone 13
            0x00 - mesh 11 (hand.R)
              0xfd - bone 14
              0x00 - mesh 12 (finger.R)
            0xfen (back to level hand.R)
              0xfd - bone 15
              0x1100 tag_weapon_tip
                  -4.2241,
                  -9.9907,
                  -20.3054,
              0x10000000 tag_weapon_start
                  -4.1674,
                  -10.0212, 
                  -2.4978,
              0x30000000 tag_weapon
                  -4.1988,
                  -9.9337, 
                  0.3238,
                  0.0000,
                  0.0000,
                  0.0000,
                  1.0000,
                  1.0000,
                  1.0000,
                  1.0000,
              0xfe
            0xfe
          0xfe
        0xfe
      0xfe (back to kinda torso level)
        0xfd - bone 16
        0x00 - mesh 13 (upper_arm.L)
          0xfd - bone 17
          0x00  - mesh 14 (lower_arm.L)
            0xfd - bone 18 
            0x00 - mesh 15 (hand.L)
              0xfd - bone 19 
              0x00 - mesh 16 (finger.L)
            0xfe
          0xfe (back to lower arm)
            0xfd - bone 20 
            0x00 - mesh 17 (shield)
          0xfe
        0xfe
      0xfe
        0xfd - bone 21 
        0x00 - mesh 18 (head)
      0xfe (back to kinda torso level)
      0x09 tag_chest
        -0.0099,
        -19.1857, 
        -1.6724,
    0xfe
  0xfe
0xfe (back to root level. so this is the end marker)
0x00 (padding to int32 size)

The mesh is structured in a simple hierarchy, where each node can transform (translate/rotate/scale) the following nodes.
So it is not really a skeleton animation.
We can draw the meshes simply without any transformations and the result looks like the model should be. So each (mesh) 
part of the model is already pre transformed and the model without any skeleton hierarchy can be treated as a 
bind pose. The animations transforms the mesh in the order translation, rotation, scale. Remember to apply these 
transformations in reverse order when doing matrix multiplication.


# References

[1] ST-013-R3-061694 - VDP1 User's Manual.pdf
[2] ST-058-R2-060194 - VDP2 User Manual.pdf
[3] ST-237-R1-051795 - SGL Developers Manual.pdf
[4] ST-238-R1-051795 - SGL Functions Reference.pdf
[5] knight0fdragon model reader: https://github.com/knight0fdragon/PlayerModelTest
[6] https://github.com/ijacquez/libyaul/tree/develop/libsega3d
[7] format-battlemesh-animation.md
