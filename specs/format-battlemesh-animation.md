# Description

Battle mesh animations always have the extension ".bin", and the files start with "x8an".
For each character there is one animation file.
The animation is keyframe based.

The first structure in an animation file is a "segment" (named b/c of lack of a better name).
The segment contains a few animation slots (naming again temporary), which looks like they belong to a single bone each.

One animation slot contains three channels: translation (vector3), a rotation (looks like a quaternion) and a strange 
vector3 channel which might be scale. 

For each channel there is a list of keyframe times and an equal amount of translation/rotation/scale.

Translation channel is the first channel. Translation is simply stored as  an SGL POINT.

Rotation is stored as 4 int16 values, which look like the fractional part of an SGL FLOAT. As a quarternion is 4 floats
and the values are unit vectors (so each value is in the range [-1 .. 1]), it does make sense to not waste space for the
fixed part. TODO: how is the sign encoded? Should the 15th bit sign extended? 
Note: this may also be a rotation axis (x,y,z normalized) and an angle. 
   @see void slRotAX(FIXED vctX, FIXED vctY, FIXED vctZ, ANGLE A)
   Rotates relative to the desired axis about the origin point. The axis is 
   specified by a unit vector.

Possible arguments for a quarternion:
* see https://stackoverflow.com/questions/8919086/why-are-quaternions-used-for-rotations
* speed: need fewer multiplications than matrix multiplication
* interpolation: quarternions can easily be interpolated

Scale channel seems to be always only one key frame, so the scale does not change over time. The value is (u)int32 and
always 0x0000 0x0001. This is 1 in uint32, but it would be nearly zero in SGL FIXED. A 1.0 in SGL FIXED is 
0x0001 0x0000. I guess this means the scale channel is unused. 

All animation slots within a segment have the same start and end times, but the exact keyframe times in between do not 
need to match (TODO: check this)

## Segments and logical groups

The segments are logically loosely grouped into blocks, in which the start and end times are consecutive. In the next
logical block the start time of the first segment restarts again. Please note that the start time does not need to be 0
and indeed most often it is not. All segments in the same logical block have the same amount of animation slots (bones).
One estimation is that the logical block corresponds to a mesh animation and the animation is split up into segments to
save precious working memory.

Example x8an00.bin (Synbios): 

    | Segment   | Times (start, end) | animation slots (bones) | remark
    +-----------+--------------------+-------------------------+-----------
    | Segment 0 | 35 - 85            |   22 slots              | start of animation 1
    | Segment 1 | 133 - 150          |   22 slots              | animation 1, part 2
    | Segment 2 | 133 - 150          |   22 slots              | animation 1, part 2 (alternative)
    | Segment 3 | 275 - 380          |   22 slots              | animation 1, part 4 (note: part 3 is the last segment in the block)
    | Segment 4 | 380 - 505          |   22 slots              | animation 1, part 5
    | Segment 5 | 150 - 275          |   22 slots              | animation 1, part 3 (out of order)
    | Segment 6 | 35 - 85            |   22 slots              | next animation
    | Segment 7 | 133 - 150          |   22 slots              | 
    | Segment 8 | 133 - 150          |   22 slots              | 
    | Segment 9 | 275 - 345          |   22 slots              | 
    | Segment 10 | 345 - 437         |   22 slots              | 
    | Segment 11 | 437 - 609         |   22 slots              | 
    | Segment 12 | 35 - 85           |   22 slots              | next animation

Some notes to this animation:
* animation times 133 to 150 is saved two times. Either this is an alternative animation part or it is an error
* the parts of the animations are not consecutive. Part 3 of the animation with times 150 - 275 is at the end of the
  segment
* total number of segment blocks (mesh animations): 8
* the number of animation slots (bones) is between 20 and 23. 
  Maybe relevant: the model has 19 meshes and one weapon mesh, in total 20.

Please note that there is momentarily no possibility to go directly to a specific segment.
After you have read all animation slots (channel size is -1) you simply skip all zero uints until one is not zero 
anymore (this is the data size of the next segment) or there are no more bytes (EOF). 


## Animation slot table

This table has one entry for each animation slot. First the number of entries for each channel are stored and then for
each channel component (details later) an offset into the segments data array. Please note that the offset is 
calculated from the start of the segment, which beginns with 4 bytes (uint32) offset to the animation slot table.
So the first offset into the data array is 0x04.

Example x8an00.bin (Synbios), segment 49 (last segment), slot 1 (counted from 0, so this is the 2nd slot):

    0x05, 0x07, 0x01, 0x80', 0x8a, 0x98, 0x9c, 0xb0, 0xc4, 0xd8, 0xe6, 0xf4, 0x102, 0x110, 0x114, 0x118
    Breakdown:
    0x05, 0x07, 0x01 - 5 translation key frames, 7 rotation key frames, 1 scale key frame
    0x80,  - offset to 5 uint16 translation key frame times (10 bytes) 
    0x8a,  - offset to 7 uint16 rotation key frame times (14 bytes)
    0x98,  - offset to 5 uint16 scale key frame times (2 bytes)
    0x9c,  - offset to 5 SGL FIXED translation x
    0xb0,  - offset to 5 SGL FIXED translation y
    0xc4,  - offset to 5 SGL FIXED translation z
    0xd8,  - offset to 7 int16 (abbreviated SGL FIXED?) rotation quarternion real part 
    0xe6,  - offset to 7 int16 (abbreviated SGL FIXED?) rotation quarternion i
    0xf4,  - offset to 7 int16 (abbreviated SGL FIXED?) rotation quarternion j
    0x102, - offset to 7 int16 (abbreviated SGL FIXED?) rotation quarternion k
    0x110, - offset to 1 int32 (strange SGL FIXED) scale x
    0x114, - offset to 1 int32 (strange SGL FIXED) scale y
    0x118  - offset to 1 int32 (strange SGL FIXED) scale z

## Animation data

Example x8an00.bin (Synbios), segment 49 (last segment), slot 1 (counted from 0, so this is the 2nd slot).
The animation data is sorted by channel for readability.

    times translation (0): 0x140,  0x14b,  0x14f,  0x159,  0x162
    translation.x (3):     2.864,  2.864,  2.864,  2.864,  2.863
    translation.y (4):    -0.896, -0.896, -1.322, -1.322, -0.934
    translation.z (5):     4.359,  4.359,  5.601,  5.601,  4.486

    times rotation (1): 0x140,  0x14b,  0x14d,  0x14f,  0x159,  0x15d,  0x162
    rotation.x (6):     0.041,  0.041,  0.046,  0.051,  0.051,  0.047,  0.042
    rotation.i (7):     0.071,  0.071,  0.071,  0.071,  0.071,  0.071,  0.071
    rotation.j (8):    -0.012, -0.012, -0.011, -0.009, -0.009, -0.010, -0.012
    rotation.k (9):     0.236,  0.236,  0.235,  0.234,  0.234,  0.235,  0.236

    times scale (2): 0x140
    scale.x (10):      0x1
    scale.y (11):      0x1
    scale.z (12):      0x1


# File Structure

    +-------------------------+
    | Segment 1               |
    | +---------------------+ |
    | | Data Section        | |
    | |                     | |
    | +---------------------+ |
    | +---------------------+ |
    | | Animation Slot Table| |
    | |                     | |
    | +---------------------+ |
    | padding                 |
    +-------------------------+
    | Segment 2               |
    | ...                     |
    +-------------------------+
    | Segment n               |
    +-------------------------+

# Segment

| Offset | Name          | Type       | Count | Description
|--------|---------------|------------|-------|-----------------------------
| 0x00   | data          | bytes      |   ?   | animation and keyframe data
| ???    | animation_slot_table | AnimationSlotTable |   1   | animation descriptions

# Data Section

| Offset | Name                | Type       | Count | Description
|--------|---------------------|------------|-------|-----------------------------
| 0x00   | slot_table_offset   | uint32     |   1   | start of animation slots table
| ???    | data                |            | dictionary_offset - 4 | data for animations


# AnimationSlotTable

| Offset | Name                | Type               | Count | Description
|--------|---------------------|--------------------|-------|-----------------------------
| 0x00   | slot_table_entry    | AnimationSlotEntry |   n   | variable number of slot entries
| ???    | end (0xffffffff)    | int32              |   1   | channel size of -1 denotes end of table

# AnimationSlotEntry (size: 0x40 bytes)

| Offset | Name                | Type   | Count | Description
|--------|---------------------|--------|-------|-----------------------------
| 0x00   | translation_keys    | uint32 |   1   | number of key frames in translation channel
| 0x04   | rotation_keys       | uint32 |   1   | number of key frames in rotation channel
| 0x08   | scale_keys          | uint32 |   1   | number of key frames in scale channel
| 0x0c   | translation_times   | uint32 |   1   | offset to translation channel times
| 0x10   | rotation_times      | uint32 |   1   | offset to rotation channel times
| 0x14   | scale_times         | uint32 |   1   | offset to scale channel times
| 0x18   | translation_x       | uint32 |   1   | offset to translation x component
| 0x1c   | translation_y       | uint32 |   1   | offset to translation y component
| 0x20   | translation_z       | uint32 |   1   | offset to translation z component
| 0x24   | rotation_real       | uint32 |   1   | offset to rotation quaternion real component
| 0x28   | rotation_i          | uint32 |   1   | offset to rotation quaternion i component
| 0x2c   | rotation_j          | uint32 |   1   | offset to rotation quaternion j component
| 0x30   | rotation_k          | uint32 |   1   | offset to rotation quaternion k component
| 0x34   | scale_x             | uint32 |   1   | offset to scale x component
| 0x38   | scale_y             | uint32 |   1   | offset to scale y component
| 0x3c   | scale_z             | uint32 |   1   | offset to scale z component

Note: all offsets are relative to the start of the segment