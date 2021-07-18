# Description

Battle mesh animations have the extension ".bin", and the files start with "x8an".
For each character there is one animation file. The animation is keyframe based.
TODO: Check animation for non player models.

The file has a technical and a logical structure. 

# File Structure

    +------------------------------+
    | Animation 0                  |
    | +--------------------------+ |
    | | Bone 0:                  | |
    | |  * Key Frames            | |
    | |  * translation,          | |
    | |  * rotation,             | |
    | |  * scale                 | |
    | +--------------------------+ |
    | | Bone 1:                  | |
    | |  * Key Frames            | |
    | |  * ...                   | |
    | +--------------------------+ |
    | | Bone n:                  | |
    | +--------------------------+ |
    | |  * Key Frames            | |
    | |  * ...                   | |
    | +--------------------------+ |
    | | Bone 0 offsets:          | |
    | | * offsets to key frames  | |
    | | * offsets to translation | |
    | | * offsets rotation       | |
    | | * scale                  | |
    | +--------------------------+ |
    | | ...                      | |
    | +--------------------------+ |
    | | Bone n offsets:          | |
    | +--------------------------+ |
    | | End of Animation marker  | |
    | +--------------------------+ |
    | padding                      |
    +------------------------------+
    | Animation 1                  |
    | ...                          |
    +------------------------------+
    | Animation n                  |
    +------------------------------+
    | End Of File Marker           |
    +------------------------------+

The file does not have a header. It contains a list of animations which are separated by paddings. The start of each
animation can only be found by reading all previous animations and skipping the null (0x00) padding bytes.
TODO: check if there are offsets to the animations in model files.

| Offset | Name          | Type       | Count | Description
|--------|---------------|------------|-------|-----------------------------
| 0x00   | animation[0]  | Animation  |   1   | first animation
| ?      | padding[0]    | bytes      |   ?   | padding to next animation
| ?      | animation[1]  | Animation  |   1   | second animation
| ?      | padding[1]    | bytes      |   ?   | padding to next animation
| ...    | ...           | ...        |  ...  | ...
| ?      | animation[n]  | Animation  |   1   | last animation
| ?      | EOF_marker    | int32      |   1   | always -1

# Animation

One animation contains key frames with the channels translation, rotation and scale for each bone of the corresponding
model. The channels for translation, rotation and scale can each have a different set of key frames.

Each component, i.e. translation x, translation y and translation z is saved in a separate list, to actually use
these you have to combine the channels.

| Offset | Name                  | Type   | Count                | Description
|--------|-----------------------|--------|----------------------|-----------------------------
| 0x00   | offset_to_offset_table| int32  |       1              | offset to table of offsets. see #bone[0]_offsets
|--------|-----------------------|--------|----------------------|-----------------------------
|        | #bone[0]_keyframes    |        |                      | key frames for bone 0 
| 0x00   | keyframes_translation | uint16 | num_translation_keys | list of key frames for translation channel
| ???    | keyframes_rotation    | uint16 | num_rotation_keys    | list of key frames for rotation channel
| ???    | keyframes_scale       | uint16 | num_scale_keys       | list of key frames for scale channel
| ???    | translation_x         | FIXED  | num_translation_keys | list of translation x component
| ???    | translation_y         | FIXED  | num_translation_keys | list of translation y component
| ???    | translation_z         | FIXED  | num_translation_keys | list of translation z component
| ???    | rotation_x            | CFIXED | num_rotation_keys    | list of rotation quaternion x component
| ???    | rotation_y            | CFIXED | num_rotation_keys    | list of rotation quaternion y component
| ???    | rotation_z            | CFIXED | num_rotation_keys    | list of rotation quaternion z component
| ???    | rotation_angle        | CFIXED | num_rotation_keys    | list of rotation quaternion w component
| ???    | scale_x               | FIXED  | num_scale_keys       | list of scale x component
| ???    | scale_y               | FIXED  | num_scale_keys       | list of scale y component
| ???    | scale_z               | FIXED  | num_scale_keys       | list of scale z component
|--------|-----------------------|--------|----------------------|-----------------------------
|        | #bone[1]_keyframes    |        |                      | key frames for bone 1
|--------|-----------------------|--------|----------------------|-----------------------------
|        |                       |        |                      | key frames for bone ...
|--------|-----------------------|--------|----------------------|-----------------------------
|        | #bone[n]_keyframes    |        |                      | key frames for bone n
|--------|-----------------------|--------|----------------------|-----------------------------
|        | #bone[0]_offsets      |        |                      | offset table for bone 0
| 0x00   | num_translation_keys  | uint32 |   1                  | number of key frames in translation channel. If this is 0xffffffff then this is the end marker
| 0x04   | num_rotation_keys     | uint32 |   1                  | number of key frames in rotation channel
| 0x08   | num_scale_keys        | uint32 |   1                  | number of key frames in scale channel
| 0x0c   | translation_times     | uint32 |   1                  | offset to translation frame numbers
| 0x10   | rotation_times        | uint32 |   1                  | offset to rotation frame numbers
| 0x14   | scale_times           | uint32 |   1                  | offset to scale frame numbers
| 0x18   | translation_x         | uint32 |   1                  | offset to translation x component
| 0x1c   | translation_y         | uint32 |   1                  | offset to translation y component
| 0x20   | translation_z         | uint32 |   1                  | offset to translation z component
| 0x24   | rotation_x            | uint32 |   1                  | offset to rotation axis x
| 0x28   | rotation_y            | uint32 |   1                  | offset to rotation axis y
| 0x2c   | rotation_z            | uint32 |   1                  | offset to rotation axis z
| 0x30   | rotation_angle        | uint32 |   1                  | offset to rotation angle
| 0x34   | scale_x               | uint32 |   1                  | offset to scale x component
| 0x38   | scale_y               | uint32 |   1                  | offset to scale y component
| 0x3c   | scale_z               | uint32 |   1                  | offset to scale z component
|--------|-----------------------|--------|----------------------|-----------------------------
|        | #bone[1]_offsets      |        |                      | offset table for bone 1
|--------|-----------------------|--------|----------------------|-----------------------------
|        | #bone[n]_offsets      |        |                      | offset table for bone n
|--------|-----------------------|--------|----------------------|-----------------------------
|        | end_marker            | int32  |                      | end of animation marker. always -1

Note: all offsets are relative to the start of the animation.

# SGL Types

* FIXED is a SGL FIXED Data Type
* CFIXED is a SGL compressed FIXED Data type. Read signed 16bit value from stream, sign extend it to 32 bits, 
  shift left by 2. Interpret the resulting int32 as an SGL FIXED. This is used for values -1 <= x <= 1, so there is no
  need for a "big" integer part.
* combining the translation components (3 x FIXED) results in a SGL VECTOR
* combining the rotation components (4 x compressed FIXED) results in a rotation quaternion. These may or may not be
  usable for `void slRoaAX(vctx , vcty , vctz , anga)`, nevertheless they can be used in matrix multiplication.
* combining the scale components (3 x FIXED) results in a scale VECTOR

# Models and Animations

Each animation file (with the list of animations) corresponds to a group of meshes. For characters the meshes belong to the same character, unpromoted
and promoted.

The animations in this file is loosely grouped by keyframe: The frames of consecutive animations are ascending and 
belong to the same mesh. Once the frame numbers restart at a low value, the animations belong to a new
mesh. Each animation in the same group has the same number of animated bones and these are the same as the number of
bones in the mesh.

Note that in the mesh files there are typically already 3 or 4 animations embedded: idle, attack, hit, block.
The animations files typically only have attack animations.

Examples are for Synbios: 
* x8an00.bin - animations file
* x8pc00a.bin - x8pc00d.bin - unpromoted mesh
* x8pc00e.bin - x8pc00h.bin - promoted mesh

## Grouping Animations

| Animation   | Times (start, end) | animation slots (bones) | remark
|-------------|--------------------|-------------------------|-----------
| Animation 0 | 35 - 85            |   22 slots              | start of animation group 0
| Animation 1 | 133 - 150          |   22 slots              | animation 1, part 2
| Animation 2 | 133 - 150          |   22 slots              | animation 1, part 2 (alternative)
| Animation 3 | 275 - 380          |   22 slots              | animation 1, part 4 (note: part 3 is the last animation in the group)
| Animation 4 | 380 - 505          |   22 slots              | animation 1, part 5
| Animation 5 | 150 - 275          |   22 slots              | animation 1, part 3 (out of order)
|-------------|--------------------|-------------------------|-----------
| Animation 6 | 35 - 85            |   22 slots              | start of animation group 1
| Animation 7 | 133 - 150          |   22 slots              | ..
| Animation 8 | 133 - 150          |   22 slots              | 
| Animation 9 | 275 - 345          |   22 slots              | 
| Animation 10 | 345 - 437         |   22 slots              | 
| Animation 11 | 437 - 609         |   22 slots              |
|-------------|--------------------|-------------------------|-----------
| Animation 12 | 35 - 85           |   22 slots              | start of animation group 2
| ...          |                   |                         |

Notes:
* Animation 1 and 2 as well as 7 and 8 are present twice in this file. This happens a lot in other animations with different Animation  
* the parts of the animations are not consecutive. Part 3 of the animation with times 150 - 275 is at the end of the
  segment

## Groups

This is the whole file, with the animations already grouped.

| Nr | Animation Group |  Frames  | Animation Bones | Model | Mesh Bones | Notes
|----|-----------------|----------|-----------------|-------|------------|------
|  0 | 0 - 5    (6)    | 35 - 505 |   22            | 00a   |  22        |
|  1 | 6 - 11   (6)    | 35 - 609 |   22            | 00b   |  22        |
|  2 | 12 - 17  (6)    | 35 - 715 |   22            | 00c   |  22        |
|  3 | 18 - 24  (7)    | 35 - 413 |   23            | 00d   |  23        | additional 0x60 entry in animation table, second 0x30 tag
|  4 | 25 - 30  (6)    | 35 - 505 |   20            | 00e   |  20        |
|  5 | 31 - 36  (6)    | 35 - 609 |   20            | 00f   |  20        |
|  6 | 37 - 42  (6)    | 35 - 715 |   20            | 00g   |  20        |
|  7 | 43 - 49  (7)    | 35 - 413 |   21            | 00h   |  21        | additional 0x60 entry in animation table, second 0x30 tag

Notes:
* "Model" denotes the mesh file for which the animation group is for.
* Promoted Synbios mesh has 2 bones less than unpromoted
* Meshes with Dagger (00d unpromoted, 00h promoted) have an additional bone,
  an additional entry with type 0x60 in the animation table and a second weapon tag (tag type 0x30)
  see [1] for details.
* at least for now there is no technical link between animation group in this file and the mesh (like an offset or so).

# Open Points

1. Check animation for non player models. Update this description if neccessary.
1. Find a better method to skip the padding at the end of the animation.
1. Find a better (technical) method to separate the animations into (mesh) groups.
1. Find a better (technical) method to link the model with the animation group.

# References

[1] format-battlemesh.md
[2] ST-238-R1-051795 - SGL Functions Reference.pdf

