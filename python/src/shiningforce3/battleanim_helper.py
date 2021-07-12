import sys
import struct
import logging

import shiningforce3.sgl as sgl


class TableEntry:
    def __init__(self):
        self.config_a = 0
        self.config_b = 0
        self.config_c = 0
        # 13 offsets.
        #       index:  0      1      2   3   4   5   6   7   8   9   10  11  12
        # size (bytes): 2a     2b     2c  4a  4a  4a  2b  2b  2b  2b  4c  4c  4c
        self.offsets = []

    def __str__(self):
        sizes = [t - s for s, t in zip(self.offsets, self.offsets[1:])] + [4]
        return "config: {} sizes: {} offsets: {} ".format([self.config_a, self.config_b, self.config_c], sizes,
                                                          [hex(i) for i in self.offsets])


class Segment:
    def __init__(self):
        self.absolute_offset = 0
        self.table = []
        self.data = []

    def __str__(self):
        return "absolute_offset: {} data size: {}\ntable ({}):\n{}".format(hex(self.absolute_offset),
                                                                           hex(len(self.data)), len(self.table),
                                                                           "\n".join(
                                                                               ["{}".format(t) for t in self.table]))


def read_fully(filename):
    with open(filename, 'rb') as stream:
        return stream.read()


def to_words(data):
    return [x[0] for x in struct.iter_unpack(">H", data)]


def read_int32(stream):
    return struct.unpack(">i", stream.read(4))[0]


def read_table_entries(stream):
    first = read_int32(stream)
    if first == -1:
        return
    entry = TableEntry()
    entry.config_a = first
    entry.config_b = read_int32(stream)
    entry.config_c = read_int32(stream)
    entry.offsets = [read_int32(stream) for _ in range(0, 13)]
    yield entry
    yield from read_table_entries(stream)


def split_segments(stream):
    segment = Segment()
    segment.absolute_offset = stream.tell()

    # skip to next dword not zero
    data_size = 0
    while True:
        data_size_buffer = stream.read(4)
        if len(data_size_buffer) < 4:
            return
        data_size = struct.unpack(">I", data_size_buffer)[0]
        if data_size != 0:
            break

    segment.data = data_size_buffer + stream.read(data_size-len(data_size_buffer))
    # read table
    segment.table = list(read_table_entries(stream))
    yield segment
    yield from split_segments(stream)


def dump_slot(count, offset_index, segment, size, table_entry, unpack_format, formatter_function, description):
    values = []
    for i in range(0, count):
        values += struct.unpack_from(unpack_format, segment.data, table_entry.offsets[offset_index] + i * size)
    print("    {} ({}): {}".format(description, offset_index, ", ".join([formatter_function(value) for value in values])))


def to_sgl_fixed(value):
    fixed = sgl.SglFixed()
    fixed.value = value
    return "{:.3f}".format(fixed.__serialize__())


def to_short_fixed(value):
    return "{:.3f}".format(value / 65536.0)


def to_sgl_angle(value):
    angle = sgl.SglAngle()
    angle.value = value
    return "{:.3f}".format(angle.__serialize__())


def dump_entry(segment: Segment, table_entry: TableEntry):
    #       index:  0      1      2   3   4   5   6   7   8   9   10  11  12
    # size (bytes): 2a     2b     2c  4a  4a  4a  2b  2b  2b  2b  4c  4c  4c

    # I think this is an animated time series with the channels
    # * translation (channel a)
    # * rotation (channel b)
    # * scale (channel c)
    # Slot 0, 1, 2 are the time stamps for translation, rotation, scale
    # slot 3,4,5 is (x, y, z) vector (SglPoint) of the translation
    # slot 6, 7, 8, 9 is the rotation quaternion. the values are 16bits, maybe SglFloat with only fractions?
    # slot 10, 11, 12 is rotation. Strangely this is not an SglFixed. Is this used anyway?
    # Notes:
    # * Time series in one segment are always the same. The slots are bones?
    # * time in segments may start with different starting times.  So for an animation there may be multiple
    #   chained segments.
    dump_slot(table_entry.config_a, 0, segment, 2, table_entry, ">H", hex, "times translation")
    dump_slot(table_entry.config_b, 1, segment, 2, table_entry, ">H", hex, "times rotation")
    dump_slot(table_entry.config_c, 2, segment, 2, table_entry, ">H", hex, "times scale")
    # translation
    dump_slot(table_entry.config_a, 3, segment, 4, table_entry, ">i", to_sgl_fixed, "translation.x")
    dump_slot(table_entry.config_a, 4, segment, 4, table_entry, ">i", to_sgl_fixed, "translation.y")
    dump_slot(table_entry.config_a, 5, segment, 4, table_entry, ">i", to_sgl_fixed, "translation.z")
    # rotation quaternion
    dump_slot(table_entry.config_b, 6, segment, 2, table_entry, ">h", to_short_fixed, "rotation.x")
    dump_slot(table_entry.config_b, 7, segment, 2, table_entry, ">h", to_short_fixed, "rotation.i")
    dump_slot(table_entry.config_b, 8, segment, 2, table_entry, ">h", to_short_fixed, "rotation.j")
    dump_slot(table_entry.config_b, 9, segment, 2, table_entry, ">h", to_short_fixed, "rotation.k")
    # scale
    dump_slot(table_entry.config_c, 10, segment, 2, table_entry, ">H", hex, "scale.x")
    dump_slot(table_entry.config_c, 11, segment, 2, table_entry, ">H", hex, "scale.y")
    dump_slot(table_entry.config_c, 12, segment, 2, table_entry, ">H", hex, "scale.z")


def main():
    logging.basicConfig(level=logging.INFO)

    filename = sys.argv[1]

    with open(filename, 'rb') as stream:
        segments = list(split_segments(stream))
        # print("\n".join([str(s) for s in segments]))
        print("number of segments: {}".format(len(segments)))

        segment = segments[0]
        print(segment)
        for no, table in enumerate(segment.table):
            print("table {}:".format(no))
            dump_entry(segment, table)

        print("first and last keyframe for each segment:")
        for no, segment in enumerate(segments):
            table_entry = segment.table[0]
            values = []
            for i in range(0, table_entry.config_a):
                values += struct.unpack_from(">H", segment.data, table_entry.offsets[0] + i * 2)
            print("Segment {} first: {} last: {} in {} slots".format(no, values[0], values[-1], len(segment.table)))


if __name__ == "__main__":
    main()
