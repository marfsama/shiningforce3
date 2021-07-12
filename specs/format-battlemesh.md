# Notes

Battle meshes always have the extension ".bin", and the files start with "x8pc".

# File Structure

    +-------------------------+
    | Header (0x800 bytes)    |
    +-------------------------+
    | Texture Definitions     |
    |                         |
    +-------------------------+
    | Compressed Texture Data |
    |                         |
    +-------------------------+
    | Meshes                  |
    |    Header               |
    |    Body Meshes          |
    |    Weapon Mesh          |
    |    Skeleton             |
    +-------------------------+
    | Animation               |
    |    Header               |
    |    Unknown Anim Stuff   |
    |    Keyframes            |
    +-------------------------+

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

Note that this decryption is independent of texture size/type. You can view this as a stream decompress. 

# Mesh Header (0x14 bytes)

| Offset | Name                | Type       | Count        | Description
|--------|---------------------|------------|--------------|----------------------------
| 0x00   | header_size         | uint32     |   1          | unknown, always 0x08
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
  * byte is in (0x09, 0x10, 0x11, 0x20)? draw external mesh
    * (this is some kind of tag for additional meshes)
    * skip to 32bit boundary. the skipped bytes are always 0x00
    * read POINT (3 x FIXED). This is the translation part for the tag
  * byte is 30? draw external mesh with rotation and scale
    * skip to 32bit boundary. the skipped bytes are always 0x00
    * read translation: POINT (3 x FIXED)
    * read rotation: 3 x FIXED (x,y,z) and 1 x ANGLE + padding or 3 x FIXED 
      (x,y,z) and 1 x FIXED (angle in some other format)
    * read scale: 3 x FIXED
  
## Notes

The saturn probably  used this structure to directly draw the model. Maybe this is easiest reproduced
with OpenGL commands.

Tag positions are absolute. See discussion about skeletal animation below in the discussion chapter.

With the rotation in tag 0x30 (which is the weapon tag) I'm unsure. Most likely this is used in the
slRotAX(x, y, z, angle) function call, but the angle in this case is 32bit and in an unusual format.

Known Tags:
* 09  tag_chest
* 10  tag_weapon_base
* 11  tag_weapon_tip
* 20  tag_hand? nearly the same as 30 (tag_weapon), but without rotation/scale
* 30  tag_weapon (translation, rotation, scale)


# Animation Chunk

The animations are split into two files: each model has a corresponding x8an*.bin file [7].
The first block in the animation chunk is unknown. The description is taken from [5].
The second block is a keyframe table.


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

Maybe this has something to do with the skeleton scale. See discussion about the skeleton below.


## Animation

Please note that the fields in the animation block is taken from [5].

| Offset | Name                     | Type        | Count        | Description
+--------+--------------------------+-------------+--------------+-----------------------------
| 0x00   | animationId              | uint16      |   1          | unknown.
| 0x02   | numberOfFrames           | uint16      |   1          | unknown.
| 0x04   | unknown2                 | uint16      |   1          | unknown.
| 0x06   | distanceFromEnemy        | uint16      |   1          | unknown
| 0x08   | offset_events            | uint32      |   1          | relative offset to AnimationEvent list 

The animation seems to list is the complete animation: various attacks, magic, heal, hit,...
Each animation has a list of events, these seems to supply parts of the animation. 
One event has a number which grows and a small number which seems to reference the 
segment groups from the x8an??.bin file [7]. TODO: check this.

## AnimationEvent

| Offset | Name                     | Type        | Count        | Description
+--------+--------------------------+-------------+--------------+-----------------------------
| 0x00   | frame                    | uint16      |   1          | unknown
| 0x02   | eventCode                | uint16      |   1          | unknown

Note: a 0xffff denotes the end of the animation event list.
Note: the eventCode is a small positive integer. Maybe this is a reference into the animation file [7].

# BoneKeyframes

Each bone in the skeleton has an attached keyframe list which is used to animate the bone through interpolation.
The BoneKeyframes Header is repeated once for each bone (see skeleton), additionally the end of the list is denoted
by 0xffff ffff.

| Offset | Name                | Type   | Count | Description
+--------+---------------------+--------+-------+-----------------------------
| 0x00   | translation_keys    | uint32 |   1   | number of key frames in translation channel
| 0x04   | rotation_keys       | uint32 |   1   | number of key frames in rotation channel
| 0x08   | scale_keys          | uint32 |   1   | number of key frames in scale channel
| 0x0c   | translation_times   | uint32 |   1   | offset to translation frame numbers
| 0x10   | rotation_times      | uint32 |   1   | offset to rotation frame numbers
| 0x14   | scale_times         | uint32 |   1   | offset to scale frame numbers
| 0x18   | translation_x       | uint32 |   1   | offset to translation x component
| 0x1c   | translation_y       | uint32 |   1   | offset to translation y component
| 0x20   | translation_z       | uint32 |   1   | offset to translation z component
| 0x24   | rotation_x          | uint32 |   1   | offset to rotation axis x
| 0x28   | rotation_y          | uint32 |   1   | offset to rotation axis y
| 0x2c   | rotation_z          | uint32 |   1   | offset to rotation axis z
| 0x30   | rotation_angle      | uint32 |   1   | offset to rotation angle
| 0x34   | scale_x             | uint32 |   1   | offset to scale x component
| 0x38   | scale_y             | uint32 |   1   | offset to scale y component
| 0x3c   | scale_z             | uint32 |   1   | offset to scale z component

Notes:
* Translation components are stored as FIXED.
* Rotation axis is stored as kinda half FIXED. It is the decimal uint16 part of a FIXED. The integer part is always 
  zero, as the axis vector has to be a normalized vector. Also note that the axis components are always positive.
* Rotation angle is stored as ANGLE
* Scale components are stored as FIXED and are mostly 1.0. In Masquirin the scale is not exactly 1.0, 
  but slightly more or less
  
# Example: Dantates Bone 0 (x8pc02b.bin)

    "translation": {
      "0": "[ 0.0000 , 0.0000 , 0.4399 ]",
      "10": "[ 0.0000 , 0.4051 , 0.4399 ]",
      "19": "[ 0.0000 , 0.0270 , 0.4399 ]",
      ...
      "429": "[ 0.0000 , 0.1389 , 0.4399 ]"
    },
    "rotation": {
      "0": "[ 0.0000 , 0.0000 , 0.0000, 1.5707964 ]",
      "110": "[ 0.0000 , 0.0000 , 0.0000, 1.5707964 ]",
      "131": "[ 0.0000 , 0.0000 , 0.0000, 1.5707964 ]",
      "140": "[ 0.0000 , 0.0000 , 0.0000, 1.5707964 ]",
      "250": "[ 0.0000 , 0.0000 , 0.0000, 1.5707964 ]",
      "261": "[ 0.0000 , 0.0000 , 0.0000, 1.5707964 ]",
      "270": "[ 0.0000 , 0.0000 , 0.0000, 1.5707964 ]",
      "380": "[ 0.0000 , 0.0000 , 0.0000, 1.5707964 ]"
    },
    "scale": {
      "0": "[ 0.9556 , 1.0303 , 1.0084 ]",
      "110": "[ 0.9556 , 1.0303 , 1.0084 ]",
      "131": "[ 0.9556 , 1.0303 , 1.0084 ]",
      "140": "[ 0.9556 , 1.0303 , 1.0084 ]",
      "250": "[ 0.9556 , 1.0303 , 1.0084 ]",
      "261": "[ 0.9556 , 1.0303 , 1.0084 ]",
      "270": "[ 0.9556 , 1.0303 , 1.0084 ]",
      "380": "[ 0.9556 , 1.0303 , 1.0084 ]"
    }


# Notes & Discussions

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
"idle pose". I'd expect that the translations in the BoneKeyframes would start at frame 1 with nearly no translation or 
rotation, so frame 0 is kinda the bind pose. During animation with ascending frames there should be only a small 
transformation in each bone (or joint), as these transformation stack upon each other. This is the result of each
slPushMatrix() call copying the current transformation matrix and applying the next joints transformation on the 
last matrix.

Strangely the transformation are way bigger than expected and point in strange directions.

Examples are from Synbios + Sword (x8pc00a.bin)

Bone 0 is the model root bone. In frame 1 the translation is [ 0.0000 , 2.5119 , 0.0353 ], which places the  
model 2.5 units below the ground level (note: the model "grows" in negative y direction, see [3] chapter 4, 
Coordinate System). 
Bone 1 places the hip and is [ -1.5118 , -14.4179 , -1.5453 ]. This moves the hip above the ground again, but
slightly (about 1.5 units) to the left and back. This happens with all bones. The model which looks good without
a bone hierarchy looks very fragmented with the bone hierarchy.

So either the BoneKeyframes interpretation is wrong or the interactions between the skeleton and the BoneKeyframes
is not the way I thought.


# References

[1] ST-013-R3-061694 - VDP1 User's Manual.pdf
[2] ST-058-R2-060194 - VDP2 User Manual.pdf
[3] ST-237-R1-051795 - SGL Developers Manual.pdf
[4] ST-238-R1-051795 - SGL Functions Reference.pdf
[5] knight0fdragon model reader: https://github.com/knight0fdragon/PlayerModelTest
[6] https://github.com/ijacquez/libyaul/tree/develop/libsega3d
[7] format-battlemesh-animation.md
