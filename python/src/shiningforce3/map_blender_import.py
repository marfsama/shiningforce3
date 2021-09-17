bl_info = {
    "name": "Import Shining Force Map",
    "blender": (2, 80, 0),
    "category": "Import",
}

import os
import json
import math

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

    def create_texture_material(self, context, file_data, path):
        uv_chunk = file_data.get("uvs")
        texture_file_name = uv_chunk.get("filename")
        return self.create_material(path, texture_file_name)

    def create_material(self, path, texture_file_name):
        texture_name = os.path.splitext(texture_file_name)[0]
        # material already imported?
        materials = bpy.data.materials
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
        material = self.create_texture_material(context, file_data, os.path.dirname(filepath))

        map_root = self.create_empty(context, "map_root")

        self.create_meshes(context, file_data, material, map_root)
        #self.create_header_objects(context, file_data, material, map_root)
        self.create_surface(context, file_data, material, map_root)

        self.create_scroll_planes(context, os.path.dirname(filepath), file_data, map_root)

        map_root.scale = [0.01, 0.01, 0.01]
        map_root.rotation_euler[0] = -1.5708

        self.create_walkmesh(context, file_data, map_root)

        self.create_trigger(context, file_data, map_root)

        return {'FINISHED'}

    def create_scroll_planes(self, context, path, file_data, mesh_root):
        if "scroll_planes" not in file_data:
            return

        scroll_planes = file_data.get("scroll_planes").get("values")
        header = file_data.get("header")
        for scroll_plane_no, scroll_pane in enumerate(scroll_planes):
            material = self.create_material(path, scroll_pane)
            tile_size = 32

            blender_mesh = bpy.data.meshes.new("scroll_plane_{}".format(scroll_plane_no))
            bm = bmesh.new()
            vertices = bm.verts

            v1 = (0 * tile_size, 0, 0 * tile_size)
            v2 = (-64 * tile_size, 0, 0)
            v3 = (-64 * tile_size, 0, -64 * tile_size)
            v4 = (0 * tile_size, 0, -64 * tile_size)
            face_config = (vertices.new(v1), vertices.new(v2), vertices.new(v3), vertices.new(v4))

            bm.verts.ensure_lookup_table()
            uv_layer = bm.loops.layers.uv.verify()

            face = bm.faces.new(face_config)
            uv_coords = [
                (0.0, 0.0),
                (1.0, 0.0),
                (1.0, 1.0),
                (0.0, 1.0)
            ]
            texture_delta = (header.get("scroll_plane_x") / (64*tile_size), header.get("scroll_plane_z") / (64*tile_size))
            for i, loop in enumerate(face.loops):
                uv = loop[uv_layer].uv
                uv[0] = uv_coords[FLIP_MODES[0][i]][0] - texture_delta[0]
                uv[1] = uv_coords[FLIP_MODES[0][i]][1] - texture_delta[1]

            bm.to_mesh(blender_mesh)
            blender_mesh.update()
            mesh_object = object_utils.object_data_add(context, blender_mesh, operator=self)
            mesh_object.data.materials.append(material)
            mesh_object.location = (0.0, header.get("scroll_plane_y"), 0.0)
            mesh_object.parent = mesh_root

    def create_trigger(self, context, file_data, mesh_root):
        surface_block = file_data.get("surface2")

        if "surface2" not in surface_block:
            return

        triggers = surface_block.get("surface2").get("trigger")

        triggers_root = self.create_empty(context, "trigger_root")
        # triggers_root.rotation_euler[1] = math.radians(180)

        for x in range(64):
            for y in range(64):
                row_array = json.loads(triggers[y])
                trigger = row_array[x]
                if trigger > 0:
#                    empty = self.create_object(context, "Cube", "trigger_{}_{}_{}".format(x, y, trigger))
                    bpy.ops.mesh.primitive_cube_add(location=(0.0, 2.0, 1.25))
                    empty = bpy.context.object
                    empty.name = "trigger_{}_{}_{}".format(x, y, trigger)
                    empty.parent = triggers_root
                    empty.location = (-x * 32-16, 0, -y * 32-16)
                    empty.scale = (16, 16, 16)


        triggers_root.parent = mesh_root

    def create_walkmesh(self, context, file_data, mesh_root):
        """Create grid for the stuff which might be a heightmap."""
        surface_block = file_data.get("surface_tiles")

        if "surface" not in surface_block:
            return

        heights = surface_block.get("surface").get("walkmesh_heights")

        grid = self.create_empty(context, "walkmesh")
        grid.rotation_euler[1] = math.radians(180)

        for x in range(16):
            for y in range(16):
                # create vertices
                vertices = []
                for w in range(5):
                    for h in range(5):
                        row = y * 5 + h
                        col = x * 5 + w
                        row_array = json.loads(heights[row])
                        height = row_array[col]
                        vertices.append(((x*4+w) * 32, -height * 2, (y*4+h) * 32))

                # create faces
                faces = []
                for w in range(4):
                    for h in range(4):
                        faces.append((w * 5 + h, (w + 1) * 5 + h, (w + 1) * 5 + h + 1, w * 5 + h + 1))

                # Create Mesh Datablock
                name = "grid_{}_{}".format(x, y)
                mesh = bpy.data.meshes.new(name)
                mesh.from_pydata(vertices, [], faces)

                # Create Object and link to scene
                mesh.update()
                mesh_object = object_utils.object_data_add(context, mesh, operator=self)
                mesh_object.parent = grid

        grid.parent = mesh_root

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
        print("uv-layer: {} . {}".format(uv_layer, uv_layer.name))

        for face_config in faces:
            face = bm.faces.new(face_config["vertices"])
            texture_id = face_config["character"]
            if str(texture_id) in texture_map:
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

        for object_name, object_list in header.get("objects").items():
            object_root = self.create_empty(context, "object_{}".format(object_name))
            object_root.parent = mesh_root
            for mesh_name, object_description in object_list.items():
                mesh = object_description.get("mesh")
                mesh_object = self.create_mesh(context, file_data, mesh, mesh_name)
                mesh_object.location = json.loads(object_description.get("position"))
                mesh_object.rotation_mode = "XYZ"
                mesh_object.rotation_euler = object_description.get("rotation")
                mesh_object.scale = json.loads(object_description.get("scale"))
                mesh_object.parent = object_root
                mesh_object.data.materials.append(texture_material)
#                print("added mesh {}".format(mesh_name))

    def read_texture_animations(self, file_data):
        header = file_data.get("header")
        if "texture_animations" not in header:
            return {}
        texture_animations_block = header.get("texture_animations")

        return {int(group.get("group"), 16): [index.get("textureImageIndex") for index in group.get("indices")]
                for group in texture_animations_block}

    def create_meshes(self, context, file_data, texture_material, mesh_root):
        if "map_objects" not in file_data:
            return []

        objects_chunk = file_data.get("map_objects")
        models = objects_chunk.get("models")
        texture_animations = self.read_texture_animations(file_data)
        print("texture_animations: {}".format(texture_animations))

        objects_root = self.create_empty(context, "objects")
        objects_root.parent = mesh_root

        meshes = []

        for index, model_header in enumerate(models):
            name = "mapobject_{}".format(index)
            mesh = model_header.get("polygonData")[0]
            mesh_object = self.create_mesh(context, file_data, mesh, name, texture_animations)
            mesh_object.location = model_header.get("position")
            mesh_object.rotation_mode = "XYZ"
            mesh_object.rotation_euler = model_header.get("rotation")
            mesh_object.scale = model_header.get("scale")
            mesh_object.parent = objects_root
            mesh_object.data.materials.append(texture_material)
            meshes.append(mesh_object)
#            print("added mesh {}".format(name))

        return meshes

    def create_mesh(self, context, file_data, mesh, name, texture_animations=None):
        if texture_animations is None:
            texture_animations = {}

        texture_uvs = file_data.get("uvs").get("uv_map")
        texture_map = file_data.get("textures")

        blender_mesh = bpy.data.meshes.new(name)
        bm = bmesh.new()
        points = mesh.get("points")
        for point in points:
            bm.verts.new(point)
        bm.verts.ensure_lookup_table()
        uv_layer = bm.loops.layers.uv.verify()
        polygons = mesh.get("polygons")
        attributes = mesh.get("polygonAttributes")
        animations = []
        for polygon, attribute in zip(polygons, attributes):
            vertex_indices = list(OrderedDict.fromkeys(polygon.get("vertexIndices")))
            bm_vertices = [bm.verts[i] for i in vertex_indices]
            if bm.faces.get(bm_vertices) is None:
                face = bm.faces.new(bm_vertices)
                # add uv if the face is textured
                attribute_dir = attribute.get("dir")
                draw_mode = attribute_dir & 0x0f
                flip_mode = (attribute_dir >> 4) & 0x3
                if draw_mode == DRAW_MODE_TEXTURED:
                    texture_id = attribute.get("texno")
                else:
                    color = attribute.get("colno")
#                    col_index = solid_colors.index(color)
#                    texture_id = num_textures + col_index
                    texture_id = -1

                if texture_id >= 0:
                    self.add_uv_to_face(face, flip_mode, texture_id, texture_map, texture_uvs, uv_layer)
                    if texture_id in texture_animations:
                        animations.append((len(bm.faces)-1, texture_id, flip_mode))

        bm.to_mesh(blender_mesh)
        blender_mesh.update()
        mesh_object = object_utils.object_data_add(context, blender_mesh, operator=self)
        # add texture animation for animated faces
        if len(animations) > 0:
            uv_layers = blender_mesh.uv_layers["UVMap"].data
            for face_no, texture_id, flip_mode in animations:
                texture_animation = texture_animations[texture_id]
                polygon = blender_mesh.polygons[face_no]
                print("{}-{}: polygon {} loops: {}".format(face_no, texture_id, polygon.index, polygon.loop_indices))
                for frame_no, animation_frame in enumerate(texture_animation):
                    uv_index = animation_frame
                    uv_box = texture_uvs[uv_index]
                    uv_coords = [
                        (uv_box[0], 1.0 - uv_box[1]),
                        (uv_box[2], 1.0 - uv_box[1]),
                        (uv_box[2], 1.0 - uv_box[3]),
                        (uv_box[0], 1.0 - uv_box[3]),
                    ]

                    for loop_index, loop_no in enumerate(polygon.loop_indices):
                        uv_layer = uv_layers[loop_no]
                        uv = uv_layer.uv
                        uv[0] = uv_coords[FLIP_MODES[flip_mode][loop_index]][0]
                        uv[1] = uv_coords[FLIP_MODES[flip_mode][loop_index]][1]
                        print("frame {} ({}) loop {} uv {}".format(loop_index, animation_frame, loop_no, uv))
                        uv_layer.keyframe_insert(data_path="uv", frame=1+frame_no*4, group="face_{}".format(face_no))

            # set all keyframes to constant interpolation
            for fcurve in blender_mesh.animation_data.action.fcurves:
                for keyframe in fcurve.keyframe_points:
                    keyframe.interpolation = 'CONSTANT'
        # bpy.ops.action.interpolation_type(type='CONSTANT')
        return mesh_object

    def add_uv_to_face(self, face, flip_mode, texture_id, texture_map, texture_uvs, uv_layer):
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

    def create_object(self, context, object_type, name):
        obj = bpy.data.objects.new(object_type, None )
        obj.name = name
        obj.empty_display_size = 1
        obj.empty_display_type = 'CUBE'
        context.collection.objects.link(obj)
        return obj

    def create_empty(self, context, name):
        return self.create_object(context, "empty",  name)

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