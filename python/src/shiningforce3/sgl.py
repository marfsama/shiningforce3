from shiningforce3 import tools

from typing import List, Any, Dict

_DEFAULT_PROPERTIES = ["$start", "$type", "$size"]


def _serialize(obj: Any, properties: List[str]):
    return tools.object_attributes_to_ordered_dict(
        obj, [item for sublist in [_DEFAULT_PROPERTIES, properties] for item in sublist])


class SglTexture:
    def __serialize__(self):
        return _serialize(self, ["Hsize", "Vsize", "CGadr", "HVsize", "vramAddress"])


class SglExtendedPolygonData:
    def __serialize__(self):
        return _serialize(
            self, ["points_offset", "num_points", "polygon_offset", "num_polygons", "polygon_attributes_offset",
                   "vertex_normals_offset", "points", "polygons", "polygon_attributes"])


class SglFixed:
    def __init__(self):
        self.value = None

    def to_float(self):
        return self.value / 65536.0

    def __serialize__(self):
        return self.to_float()


class SglAngle:
    def __init__(self):
        self.value = None

    def to_degree(self):
        return self.value / (65536.0 / 360.0 )

    def __serialize__(self):
        return self.to_degree()


class SglPoint:
    def __init__(self):
        self.x = None
        self.y = None
        self.z = None

    def __serialize__(self):
        return '{:.3f}, {:.3f}, {:.3f}'.format(self.x.to_float(), self.y.to_float(), self.z.to_float())


class SglPolygon:
    def __serialize__(self):
        return _serialize(self, ["normal", "indices"])


class SglPolygonAttribute:
    def __serialize__(self):
        return _serialize(self, ["flag", "sort", "texture_no", "attributes", "color_no", "gouraud_table", "dir"])


_TYPES = {
    "SglTexture": SglTexture,
    "SglExtendedPolygonData": SglExtendedPolygonData,
    "SglFixed": SglFixed,
    "SglPoint": SglPoint,
    "SglPolygon": SglPolygon,
    "SglPolygonAttribute": SglPolygonAttribute,
}


def add_sgl_types(types : Dict):
    types.update(_TYPES)
