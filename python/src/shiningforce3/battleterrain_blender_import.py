bl_info = {
    "name": "Import Shining Force Battle Terrain",
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


class ImportSf3BattleTerrain(bpy.types.Operator, ImportHelper):
    """Import Shining Force Battle Terrain"""      # Use this as a tooltip for menu items and buttons.
    bl_idname = "import.sf3_btlterrain_import"        # Unique identifier for buttons and menu items to reference.
    bl_label = "SF3 Battle Terrain (*.json)"         # Display name in the interface.
    bl_options = {'REGISTER', 'UNDO'}  # Enable undo for the operator.

    # ImportHelper mixin class uses this
    filename_ext = ".json"
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
    filter_glob = StringProperty(default = "*.json", options = {'HIDDEN'})

    def execute(self, context):
        return self.read_terrain(context, self.filepath)

    def create_material(self, path, texture_file_name):
        print("creating material from {}/{}".format(path, texture_file_name))
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

    def read_terrain(self, context, filepath):
        print("running read_terrain...{}".format(filepath))
        f = open(filepath, 'r', encoding='utf-8')
        data = f.read()
        f.close()

        map_root = self.create_empty(context, "map_root")
        objects_root = self.create_empty(context, "objects_root")
        mirrored_root = self.create_empty(context, "mirrored_root")

        # would normally load the data here
        file_data = json.loads(data)
        texture_material = self.create_material(os.path.dirname(filepath), file_data.get("textures").get("texture_file"))
        ground_material = self.create_material(os.path.dirname(filepath), file_data.get("ground_file"))
        meshes = self.create_meshes(context, file_data, texture_material, map_root)

        self.create_instances(context, file_data, meshes, objects_root)
        self.create_instances(context, file_data, meshes, mirrored_root)

        # delete original meshes
        self.delete_meshes(context, meshes)

        self.add_ground(context, ground_material, objects_root)
        self.add_ground(context, ground_material, mirrored_root)

        mirrored_root.parent = map_root
        objects_root.parent = map_root

        mirrored_root.scale = [1, 1, -1]
        mirrored_root.location[2] = -257

        map_root.scale = [0.1, 0.1, 0.1]
        map_root.rotation_euler[0] = -1.5708

        return {'FINISHED'}

    def add_ground(self, context, texture_material, parent):
        blender_mesh = bpy.data.meshes.new("ground")
        bm = bmesh.new()
        v1 = bm.verts.new([256, 0, 128.5])
        v2 = bm.verts.new([-256, 0, 128.5])
        v3 = bm.verts.new([-256, 0, -128.5])
        v4 = bm.verts.new([256, 0, -128.5])
        bm.verts.ensure_lookup_table()
        uv_layer = bm.loops.layers.uv.verify()
        face = bm.faces.new([v1, v2, v3, v4])
        uv_coords = [
            (0.0, 0.0),
            (1.0, 0.0),
            (1.0, 1.0),
            (0.0, 1.0)
        ]
        for i, loop in enumerate(face.loops):
            uv = loop[uv_layer].uv
            uv[0] = uv_coords[i][0]
            uv[1] = uv_coords[i][1]
        bm.to_mesh(blender_mesh)
        blender_mesh.update()
        mesh_object = object_utils.object_data_add(context, blender_mesh, operator=self)
        mesh_object.data.materials.append(texture_material)
        mesh_object.parent = parent

    def delete_meshes(self, context, meshes):
        for mesh in meshes:
            context.collection.objects.unlink(mesh)

    def create_instances(self, context, file_data, meshes, parent):
        mesh_chunk = file_data.get("mesh_chunk")
        instances = mesh_chunk.get("instances")
        mesh_instances = []
        for index, instance in enumerate(instances):
            mesh_id = instance.get("meshId")
            rotation = instance.get("rotation")
            position = instance.get("position")
            scale = instance.get("scale")

            mesh = meshes[mesh_id]
            instanced_object = bpy.data.objects.new("instance_{}".format(index), mesh.data)
            instanced_object.rotation_euler = rotation
            instanced_object.scale = scale
            instanced_object.location = position
            instanced_object.parent = parent
            mesh_instances.append(instanced_object)
            context.collection.objects.link(instanced_object)
        return mesh_instances

    def create_meshes(self, context, file_data, texture_material, parent):
        mesh_chunk = file_data.get("mesh_chunk")
        body_meshes = mesh_chunk.get("meshes")
        meshes = []

        for index, mesh in enumerate(body_meshes):
            mesh_object = self.create_mesh(context, file_data, mesh, "mesh_{}".format(index))
            if texture_material is not None:
                mesh_object.data.materials.append(texture_material)
            mesh_object.parent = parent
            meshes.append(mesh_object)
            print("added mesh {}".format(mesh_object.name))

        return meshes

    def create_mesh(self, context, file_data, mesh, name):
        texture_uvs = file_data.get("textures").get("uvs")
        solid_colors = file_data.get("textures").get("solid_colors")
        num_textures = len(file_data.get("textures").get("texture_list"))
        blender_mesh = bpy.data.meshes.new(name)
        bm = bmesh.new()
        points = mesh.get("points")
        for point in points:
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
                attribute_dir = attribute.get("dir")
                draw_mode = attribute_dir & 0x0f
                flip_mode = (attribute_dir >> 4) & 0x3
                if draw_mode == DRAW_MODE_TEXTURED:
                    texture_id = attribute.get("texno")
                else:
                    color = attribute.get("colno")
                    col_index = solid_colors.index(color)
                    texture_id = num_textures + col_index

                uv_box = texture_uvs[texture_id]
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
    self.layout.operator(ImportSf3BattleTerrain.bl_idname)


def register():
    bpy.utils.register_class(ImportSf3BattleTerrain)
    bpy.types.TOPBAR_MT_file_import.append(menu_func)


def unregister():
    bpy.utils.unregister_class(ImportSf3BattleTerrain)
    bpy.types.TOPBAR_MT_file_import.remove(menu_func)


if __name__ == "__main__":
    register()
