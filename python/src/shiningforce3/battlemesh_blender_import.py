bl_info = {
    "name": "Import Shining Force Battle Model",
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


class ImportSf3BattleModel(bpy.types.Operator, ImportHelper):
    """Import Shining Force Battle Model"""      # Use this as a tooltip for menu items and buttons.
    bl_idname = "import.sf3_btl_import"        # Unique identifier for buttons and menu items to reference.
    bl_label = "SF3 Battle Model (*.json)"         # Display name in the interface.
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
    # show only the JSON files
    filename_ext = ".json"
    filter_glob = StringProperty(default = "*.json", options = {'HIDDEN'})

    def execute(self, context):
        return self.read_some_data(context, self.filepath)

    def add_box(self, corner_min, corner_max):
        """
        This function takes inputs and returns vertex and face arrays.
        no actual mesh data creation is done here.
        """

        verts = [
            (corner_max[0], corner_max[1], corner_min[2]),
            (corner_max[0], corner_min[1], corner_min[2]),
            (corner_min[0], corner_min[1], corner_min[2]),
            (corner_min[0], corner_max[1], corner_min[2]),
            (corner_max[0], corner_max[1], corner_max[2]),
            (corner_max[0], corner_min[1], corner_max[2]),
            (corner_min[0], corner_min[1], corner_max[2]),
            (corner_min[0], corner_max[1], corner_max[2]),
        ]

        faces = [
            (0, 1, 2, 3),
            (4, 7, 6, 5),
            (0, 4, 5, 1),
            (1, 5, 6, 2),
            (2, 6, 7, 3),
            (4, 0, 3, 7),
        ]

        mesh = bpy.data.meshes.new("Box")

        bm = bmesh.new()

        for v_co in verts:
            bm.verts.new(v_co)

        bm.verts.ensure_lookup_table()
        for f_idx in faces:
            bm.faces.new([bm.verts[i] for i in f_idx])

        bm.to_mesh(mesh)
        mesh.update()
        return mesh

    def create_material(self, context, file_data, path):

        materials = bpy.data.materials

        textures_chunk = file_data.get("textures")
        texture_file_name = textures_chunk.get("textureFileName")
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

    def create_animation_actions(self, context, file_data, meshes):
        animations = file_data.get("animations").get("dictionary")

        print("animations:{}".format(len(animations)))

        for i, mesh in enumerate(meshes):
            print("mesh {} {}".format(i, mesh))
            animation = list(animations.values())[i+4]

            for frame, location in animation.get("translation").items():
                mesh.location = json.loads(location)
                mesh.keyframe_insert(data_path='location', frame=int(frame))

            mesh.rotation_mode = "AXIS_ANGLE"
            for frame, quarternion in animation.get("rotation").items():
                values = json.loads(quarternion)
                mesh.rotation_axis_angle = [values[1], values[2], values[3], values[0]]
                mesh.keyframe_insert(data_path='rotation_quaternion', frame=int(frame))

#            action = bpy.data.actions.new("{}.action".format(mesh.name))
#            fcurve = action.fcurves.new("location")
#            fcurve.keyframe_points.add(count=len(translation_list))

#            fcurve.keyframe_points.foreach_set("co", [x for co in zip(translation_list.keys(), translation_list.values()) for x in co])
            # update
#            fcurve.update()
            # assign to context ob
#            animation_data = mesh.animation_data_create()
#            animation_data.action = action

    def create_skeleton(self, context, file_data, meshes):
        animations = list(file_data.get("animations").get("dictionary").values())
        skeleton = file_data.get("meshes").get("skeleton")
        mesh_root = self.create_empty(context, "mesh_root")
        mesh_root.rotation_euler[0] = -1.5

        tags = {}

        for child in skeleton["childs"]:
            self.process_skeleton_bone(context, child, mesh_root, animations, meshes, tags)
        return tags

    def process_skeleton_bone(self, context, bone_desc, bone, animations, meshes, tags):
        if "index" in bone_desc:
            # create new bone
            bone_index = bone_desc["index"]
            current_bone = self.create_empty(context, "bone[{}]".format(bone_index))
            current_bone.parent = bone
            current_bone.rotation_mode = 'QUATERNION'

            animation = animations[bone_index]
            for frame, location in animation.get("translation").items():
                current_bone.location = json.loads(location)
                current_bone.keyframe_insert(data_path='location', frame=int(frame))

            for frame, rotation in animation.get("rotation").items():
                rotation_values = json.loads(rotation)
                current_bone.rotation_quaternion = [rotation_values[3], rotation_values[0], rotation_values[1], rotation_values[2]]
                current_bone.keyframe_insert(data_path='rotation_quaternion', frame=int(frame))

            for frame, scale in animation.get("scale").items():
                scale_values = json.loads(scale)
                current_bone.scale = scale_values
                current_bone.keyframe_insert(data_path='scale', frame=int(frame))
        else:
            current_bone = bone

        if "meshes" in bone_desc:
            for mesh_id in bone_desc["meshes"]:
                mesh = meshes[mesh_id]
                mesh.parent = current_bone

        if "tags" in bone_desc:
            for child in bone_desc["tags"]:
                type = child.get("type")
                tag = self.create_empty(context, "tag {}".format(type))
                tag.rotation_mode = 'QUATERNION'
                tag.parent = current_bone

                tag.location = json.loads(child.get("translation"))

                if "rotation" in child:
                    rotation_values = json.loads(child.get("rotation"))
                    tag.rotation_quaternion = [rotation_values[3], rotation_values[0], rotation_values[1],
                                               rotation_values[2]]

                if "scale" in child:
                    tag.scale = json.loads(child.get("scale"))

                tags[type] = tag


        if "childs" in bone_desc:
            for child in bone_desc["childs"]:
                self.process_skeleton_bone(context, child, current_bone, animations, meshes, tags)

    def read_some_data(self, context, filepath):
        print("running read_some_data...{}".format(filepath))
        f = open(filepath, 'r', encoding='utf-8')
        data = f.read()
        f.close()

        # would normally load the data here
        file_data = json.loads(data)
        material = self.create_material(context, file_data, os.path.dirname(filepath))
        meshes = self.create_meshes(context, file_data, material)
        # self.create_animation_actions(context, file_data, meshes)
        tags = self.create_skeleton(context, file_data, meshes)
        # put weapon in weapon tag
        # there must be a tag 0x30 and the last mesh shouldn't already have a parent
        last_mesh = meshes[-1]
        if last_mesh.parent is None and 0x30 in tags:
            last_mesh.parent = tags[0x30]

        return {'FINISHED'}

    def create_animation_box(self, context, file_data, mesh_root):
        animations_chunk = file_data.get("animations")
        animations_header = animations_chunk.get("header")
        box_min = json.loads(animations_header.get("bounding_box_min"))
        box_max = json.loads(animations_header.get("bounding_box_max"))
        box_mesh = self.add_box(box_min, box_max)
        box_object = object_utils.object_data_add(context, box_mesh, operator=self)
        box_object.parent = mesh_root

    def create_empty(self, context, name):
        mesh_root = bpy.data.objects.new( "empty", None )
        mesh_root.name = name
        mesh_root.empty_display_size = 1
        mesh_root.empty_display_type = 'CUBE'
        context.collection.objects.link(mesh_root)
        return mesh_root

    def create_meshes(self, context, file_data, texture_material):
        mesh_chunk = file_data.get("meshes")
        # objects = bpy.data.objects
        body_meshes = mesh_chunk.get("body_meshes")
        meshes = []


        for name, mesh in body_meshes.items():
            mesh_object = self.create_mesh(context, file_data, mesh, name)
            # mesh_object.parent = mesh_root
            mesh_object.data.materials.append(texture_material)
            meshes.append(mesh_object)
            print("added mesh {}".format(name))

        weapon_mesh = mesh_chunk.get("weaponMesh")
        if weapon_mesh is not None:
            mesh_object = self.create_mesh(context, file_data, weapon_mesh, "weapon_mesh")
            # mesh_object.parent = mesh_root
            mesh_object.data.materials.append(texture_material)
            meshes.append(mesh_object)
            print("added weapon mesh")

        return meshes

    def create_mesh(self, context, file_data, mesh, name):
        texture_uvs = file_data.get("textures").get("uvs")
        solid_colors = file_data.get("textures").get("solid_colors")
        num_textures = len(file_data.get("textureDefinitions").get("defs"))
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


# some action
# a = bpy.data.actions.new("SomeAction")
# fc = a.fcurves.new("location", 1, "LocY")

# fc.keyframe_points.add(count=len(frames))
# populate points

# fc.keyframe_points.foreach_set("co", [x for co in zip(frames, samples) for x in co])
# update
# fc.update()
# assign to context ob
# ad = bpy.context.object.animation_data_create()
# ad.action = a

# https://blender.stackexchange.com/questions/135759/is-it-possible-to-create-an-animation-in-blender-2-8-using-python-that-its-no
# https://blender.stackexchange.com/a/64450
# http://web.purplefrog.com/~thoth/blender/python-cookbook/animate-random-spin.html

def menu_func(self, context):
    self.layout.operator(ImportSf3BattleModel.bl_idname)

def register():
    bpy.utils.register_class(ImportSf3BattleModel)
    bpy.types.TOPBAR_MT_file_import.append(menu_func)

def unregister():
    bpy.utils.unregister_class(ImportSf3BattleModel)
    bpy.types.TOPBAR_MT_file_import.remove(menu_func)


# This allows you to run the script directly from Blender's Text editor
# to test the add-on without having to install it.
if __name__ == "__main__":
    register()