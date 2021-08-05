bl_info = {
    "name": "Import Shining Force Map",
    "blender": (2, 80, 0),
    "category": "Import",
}

import os
import json

import bpy
import bmesh

from bpy_extras.io_utils import ImportHelper
from bpy_extras import object_utils
from collections import OrderedDict
from bpy_extras.object_utils import AddObjectHelper
from bpy.props import (
    BoolProperty,
    BoolVectorProperty,
    EnumProperty,
    FloatProperty,
    FloatVectorProperty,
    StringProperty,
)

DRAW_MODE_TEXTURED = 2
DRAW_MODE_POLY = 4

FLIP_MODES = {
    0: [0, 1, 2, 3],  # don't flip
    1: [1, 0, 3, 2],  # flip x
    2: [3, 2, 1, 0],  # flip y
    3: [2, 3, 0, 1]   # flip both
}


class ImportSf3Map(bpy.types.Operator, ImportHelper):
    """Import Shining Force Map"""      # Use this as a tooltip for menu items and buttons.
    bl_idname = "import.sf3_mpd_import"        # Unique identifier for buttons and menu items to reference.
    bl_label = "SF3 Map (*.json)"         # Display name in the interface.
    bl_options = {'REGISTER', 'UNDO'}  # Enable undo for the operator.

    # ImportHelper mixin class uses this
    filename_ext = ".json"
    filter_glob = StringProperty(default = "*.json", options = {'HIDDEN'})
    # generic transform props
    align_items = (
        ('WORLD', "World", "Align the new object to the world"),
        ('VIEW', "View", "Align the new object to the view"),
        ('CURSOR', "3D Cursor", "Use the 3D cursor orientation for the new object")
    )
    align: EnumProperty(
        name="Align",
        items=align_items,
        default='WORLD',
        update=AddObjectHelper.align_update_callback,
    )
    location: FloatVectorProperty(
        name="Location",
        subtype='TRANSLATION',
    )
    rotation: FloatVectorProperty(
        name="Rotation",
        subtype='EULER',
    )

    def execute(self, context):
        return self.read_some_data(context, self.filepath)

    def create_material(self, context, file_data, path):

        materials = bpy.data.materials

        uv_chunk = file_data.get("uvs")
        texture_file_name = uv_chunk.get("filename")
        texture_name = os.path.splitext(texture_file_name)[0]

        # material already imported?
        material = materials.get(texture_name)
        if material:
            return material

        mat = bpy.data.materials.new(name=texture_name)
        mat.use_nodes = True
        bsdf = mat.node_tree.nodes["Principled BSDF"]
        tex_image = mat.node_tree.nodes.new('ShaderNodeTexImage')
        texture_full_path = os.path.join(path, texture_file_name)
        print("texture_full_path: {}".format(texture_full_path))
        tex_image.image = bpy.data.images.load(texture_full_path)
        mat.node_tree.links.new(bsdf.inputs['Base Color'], tex_image.outputs['Color'])
        return mat

    def read_some_data(self, context, filepath):
        print("running read_some_data...{}".format(filepath))
        f = open(filepath, 'r', encoding='utf-8')
        data = f.read()
        f.close()

        # would normally load the data here
        file_data = json.loads(data)
        material = self.create_material(context, file_data, os.path.dirname(filepath))

        mesh_root = self.create_empty(context, "map_root")

        meshes = self.create_meshes(context, file_data, material, mesh_root)
        meshes = self.create_header_objects(context, file_data, material, mesh_root)
        self.create_surface(context, file_data, material, mesh_root)

        mesh_root.scale = [0.01, 0.01, 0.01]
        mesh_root.rotation_euler[0] = -1.5708

        return {'FINISHED'}


    def create_surface(self, context, file_data, texture_material, mesh_root):
        surface_heights = file_data.get("surface2").get("surface2")

        if "heights" not in surface_heights:
            print("no heightmap")
            return

        surface_tiles = file_data.get("surface_tiles")
        if "surface" not in surface_tiles:
            print("no character map")
            return

        texture_uvs = file_data.get("uvs").get("uv_map")
        texture_map = file_data.get("textures")

        character_encoded = file_data.get("surface_tiles").get("surface").get("characters")
        characters = [ json.loads(line) for line in character_encoded ]

        tile_size = 32

        blender_mesh = bpy.data.meshes.new("surface")
        bm = bmesh.new()
        vertices = bm.verts
        faces = []
        for y, line in enumerate(surface_heights.get("heights")):
            values = json.loads(line)
            for x, height in enumerate(values):
                character = characters[y][x] & 0xff
                if character != 0xff:
                    z1 = height & 0xff
                    z2 = (height >> 8) & 0xff
                    z3 = (height >> 16) & 0xff
                    z4 = (height >> 24) & 0xff
                    v1 = (-x * tile_size, -z3*(tile_size/16), -y * tile_size)
                    v2 = (-(x+1) * tile_size, -z4*(tile_size/16), -y * tile_size)
                    v3 = (-(x+1) * tile_size, -z1*(tile_size/16), -(y+1) * tile_size)
                    v4 = (-x * tile_size, -z2*(tile_size/16), -(y+1) * tile_size)
                    faces.append({ "vertices": (vertices.new(v1), vertices.new(v2), vertices.new(v3), vertices.new(v4)),
                                   "character": character})

        bm.verts.ensure_lookup_table()
        uv_layer = bm.loops.layers.uv.verify()

        for face_config in faces:
            face = bm.faces.new(face_config["vertices"])
            texture_id = face_config["character"]
            uv_index = texture_map.get(str(texture_id)).get("textureImageIndex")
            uv_box = texture_uvs[uv_index]
            uv_coords = [
                (uv_box[0], 1.0 - uv_box[3]),
                (uv_box[2], 1.0 - uv_box[3]),
                (uv_box[2], 1.0 - uv_box[1]),
                (uv_box[0], 1.0 - uv_box[1]),
            ]
            for i, loop in enumerate(face.loops):
                uv = loop[uv_layer].uv
                flip_mode = 0
                uv[0] = uv_coords[FLIP_MODES[flip_mode][i]][0]
                uv[1] = uv_coords[FLIP_MODES[flip_mode][i]][1]


        bm.to_mesh(blender_mesh)
        blender_mesh.update()
        mesh_object = object_utils.object_data_add(context, blender_mesh, operator=self)
        mesh_object.data.materials.append(texture_material)
        mesh_object.parent = mesh_root

        print("{} surface vertices".format(len(bm.verts)))

    def create_header_objects(self, context, file_data, texture_material, mesh_root):
        header = file_data.get("header")

        if "objects" not in header:
            return []

        for name, object in header.get("objects").items():
            object_root = self.create_empty(context, "object_".format(name))
            object_root.parent = mesh_root
            for name, foo in object.items():
                mesh = foo.get("mesh")
                mesh_object = self.create_mesh(context, file_data, mesh, name)
                mesh_object.location = json.loads(foo.get("position"))
                mesh_object.rotation_mode = "XYZ"
                mesh_object.rotation_euler = foo.get("rotation")
                mesh_object.scale = json.loads(foo.get("scale"))
                mesh_object.parent = object_root
                mesh_object.data.materials.append(texture_material)
                print("added mesh {}".format(name))



    def create_meshes(self, context, file_data, texture_material, mesh_root):
        if "map_objects" not in file_data:
            return []

        objects_chunk = file_data.get("map_objects")
        models = objects_chunk.get("models")
        meshes = []

        for name, model_header in models.items():
            mesh = model_header.get("polygonData")[0]
            mesh_object = self.create_mesh(context, file_data, mesh, name)
            mesh_object.location = json.loads(model_header.get("position"))
            mesh_object.rotation_mode = "XYZ"
            mesh_object.rotation_euler = model_header.get("rotation")
            mesh_object.parent = mesh_root
            mesh_object.data.materials.append(texture_material)
            meshes.append(mesh_object)
            print("added mesh {}".format(name))

        return meshes

    def create_mesh(self, context, file_data, mesh, name):
        texture_uvs = file_data.get("uvs").get("uv_map")
        texture_map = file_data.get("textures")
        #solid_colors = file_data.get("textures").get("solid_colors")
        # num_textures = len(file_data.get("textureDefinitions").get("defs"))
        blender_mesh = bpy.data.meshes.new(name)
        bm = bmesh.new()
        points = mesh.get("points")
        for point_str in points:
            point = json.loads(point_str)
            bm.verts.new(point)
        bm.verts.ensure_lookup_table()
        uv_layer = bm.loops.layers.uv.verify()
        polygons = mesh.get("polygons")
        attributes = mesh.get("polygonAttributes")
        for polygon, attribute in zip(polygons, attributes):
            vertex_indices = list(OrderedDict.fromkeys(polygon.get("vertexIndices")))
            bm_vertices = [bm.verts[i] for i in vertex_indices]
            if bm.faces.get(bm_vertices) is None:
                face = bm.faces.new(bm_vertices)
                # add uv if the face is textured
                attribute_dir = int(attribute.get("dir"), 16)
                draw_mode = attribute_dir & 0x0f
                flip_mode = (attribute_dir >> 4) & 0x3
                if draw_mode == DRAW_MODE_TEXTURED:
                    texture_id = attribute.get("texno")
                else:
                    color = int(attribute.get("colno"), 16)
#                    col_index = solid_colors.index(color)
#                    texture_id = num_textures + col_index
                    texture_id = -1

                if texture_id >= 0:
                    # first look up the uv index by texture id. Note: the texture id must not be strictly consecutive
                    uv_index = -1
                    if str(texture_id) not in texture_map:
                        print("texture id {} not in texture map".format(texture_id))
                    else:
                        uv_index = texture_map.get(str(texture_id)).get("textureImageIndex")
                    if uv_index >= 0:
                        uv_box = texture_uvs[uv_index]
                        uv_coords = [
                            (uv_box[0], 1.0 - uv_box[1]),
                            (uv_box[2], 1.0 - uv_box[1]),
                            (uv_box[2], 1.0 - uv_box[3]),
                            (uv_box[0], 1.0 - uv_box[3]),
                        ]
                        for i, loop in enumerate(face.loops):
                            uv = loop[uv_layer].uv
                            uv[0] = uv_coords[FLIP_MODES[flip_mode][i]][0]
                            uv[1] = uv_coords[FLIP_MODES[flip_mode][i]][1]
        bm.to_mesh(blender_mesh)
        blender_mesh.update()
        mesh_object = object_utils.object_data_add(context, blender_mesh, operator=self)
        return mesh_object

    def create_empty(self, context, name):
        mesh_root = bpy.data.objects.new( "empty", None )
        mesh_root.name = name
        mesh_root.empty_display_size = 1
        mesh_root.empty_display_type = 'CUBE'
        context.collection.objects.link(mesh_root)
        return mesh_root

def menu_func(self, context):
    self.layout.operator(ImportSf3Map.bl_idname)

def register():
    bpy.utils.register_class(ImportSf3Map)
    bpy.types.TOPBAR_MT_file_import.append(menu_func)

def unregister():
    bpy.utils.unregister_class(ImportSf3Map)
    bpy.types.TOPBAR_MT_file_import.remove(menu_func)


# This allows you to run the script directly from Blender's Text editor
# to test the add-on without having to install it.
if __name__ == "__main__":
    register()