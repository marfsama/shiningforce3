bl_info = {
    "name": "Import Shining Force Battle Model",
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

ANIMATION_TYPES = {
    0x0:  "idle",
    0x10: "hit",
    0x20: "block",
    0x70: "attack"
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
        animations = file_data.get("animations").get("bone_key_frames")

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
        animations = list(file_data.get("animations").get("bone_key_frames").values())
        skeleton = file_data.get("meshes").get("skeleton")

        # can only create armature in object mode
        if (bpy.context.mode != 'OBJECT'):
            bpy.ops.object.mode_set(mode='OBJECT')
        # create armature and find root node
        bpy.ops.object.armature_add()
        armature_object = bpy.context.active_object
        armature = armature_object.data

        # enter edit mode to modify bones
        bpy.ops.object.mode_set(mode='EDIT',toggle=True)
        root = [k for k in armature.edit_bones if k.parent == None][0]
        root.tail[2] = 0.1
        armature.edit_bones[root.name].select = True

        tags = {}

        for child in skeleton["childs"]:
            self.process_skeleton_bone_with_armature(context, armature_object, child, root, animations, meshes, tags)

        # switch to object mode to save the edit_bones
        bpy.ops.object.mode_set(mode='OBJECT')

        # rig meshes to bones
        for mesh in meshes:
            if "bone_name" in mesh:
                bone_name = mesh["bone_name"]
                mesh.parent = armature_object
                mesh.parent_type = "BONE"
                mesh.parent_bone = bone_name

        # put weapon in weapon tag
        # there must be a tag 0x30 and the last mesh shouldn't already have a parent
        last_mesh = meshes[-1]
        if last_mesh.parent is None and 0x30 in tags:
            mesh.parent = armature_object
            mesh.parent_type = "BONE"
            last_mesh.parent_bone = tags[0x30].name
            print(tags[0x30].parent_recursive)
            parents = ".".join([parent.name for parent in tags[0x30].parent_recursive].reverse())
            print("added weapon to {}.{}".format(parents, tags[0x30].name))

        # in pose mode animations can be created
        bpy.ops.object.mode_set(mode='POSE')

        pose_bones = armature_object.pose.bones

        root_pose_bone = [k for k in pose_bones if k.parent is None][0]
        root_pose_bone.rotation_mode = 'XYZ'
        root_pose_bone.rotation_euler[0] = math.pi

        self.add_tag_transforms(armature_object, bone_name, pose_bones)

        for animation_description in file_data.get("animations").get("animations").values():
            type = int(animation_description.get("type"), 16)
            if type in ANIMATION_TYPES:
                type_name = ANIMATION_TYPES.get(type)
                start_frame = animation_description.get("start_frame")
                num_frames = animation_description.get("numberOfFrames")
                end_frame = start_frame + num_frames - 1
                self.create_animation_action(animations, armature_object, pose_bones, type_name, start_frame, end_frame)

        self.create_animation_action(animations, armature_object, pose_bones, "all", 1, 1000)

        # switch back to object mode to save the pose
        bpy.ops.object.mode_set(mode='OBJECT')
        return tags

    def filter_frames(self, animations, min_frame, max_frame):
        return {anim[0]: anim[1] for anim in animations.items() if min_frame <= int(anim[0]) <= max_frame}

    def create_animation_action(self, animations, armature_object, pose_bones, action_name, min_frame, max_frame):
        actions = bpy.data.actions
        if action_name in actions:
            actions.remove(actions[action_name])

        action = actions.new(action_name)
        action.use_fake_user = True
        # loop over "normal" bones
        for bone in armature_object.data.bones:
            if "bone_index" in bone:
                # get the pose bone
                bone_name = bone.name
                pose_bone = pose_bones[bone_name]
                bone_index = bone["bone_index"]
                # find bone in animation bones
                animation = animations[bone_index]
                translations = self.filter_frames(animation.get("translation"), min_frame, max_frame)
                for channel in range(0, 3):
                    fcurve = action.fcurves.new(data_path='pose.bones["{}"].location'.format(bone_name), index=channel)
                    for frame, translation in translations.items():
                        location = json.loads(translation)
                        fcurve.keyframe_points.insert(int(frame) - min_frame + 1, location[channel])

                rotations = self.filter_frames(animation.get("rotation"), min_frame, max_frame)
                for channel in range(0, 4):
                    fcurve = action.fcurves.new(data_path='pose.bones["{}"].rotation_quaternion'.format(bone_name),
                                                index=channel)
                    for frame, rotation in rotations.items():
                        rotation_values = json.loads(rotation)
                        fcurve.keyframe_points.insert(int(frame) - min_frame + 1, rotation_values[(channel + 3) % 4])

                scales = self.filter_frames(animation.get("scale"), min_frame, max_frame)
                for channel in range(0, 3):
                    fcurve = action.fcurves.new(data_path='pose.bones["{}"].scale'.format(bone_name), index=channel)
                    for frame, scale in scales.items():
                        scale_values = json.loads(scale)
                        fcurve.keyframe_points.insert(int(frame) - min_frame + 1, scale_values[channel])

    def add_tag_transforms(self, armature_object, bone_name, pose_bones):
        # loop over "normal" bones
        for bone in armature_object.data.bones:
            # get the pose bone
            bone_name = bone.name
            pose_bone = pose_bones[bone_name]

            if "location" in bone:
                pose_bone.location = bone["location"]

            if "scale" in bone:
                pose_bone.scale = bone["scale"]

            if "rotation_quaternion" in bone:
                pose_bone.rotation_quaternion = bone["rotation_quaternion"]
        return bone_name, pose_bone

    def create_bone(self, context, armature, parent, name):
        edit_bone = armature.data.edit_bones.new(name)
        edit_bone.parent = parent
        edit_bone.head = parent.tail
        edit_bone.tail = (0,0,edit_bone.head[2]+0.01)
        edit_bone.use_connect = False
        return edit_bone

    def process_skeleton_bone_with_armature(self, context, armature, bone_desc, bone, animations, meshes, tags):
        if "index" in bone_desc:
            # create new bone
            bone_index = bone_desc["index"]
            current_bone = self.create_bone(context, armature, bone, "bone[{}]".format(bone_index))
            current_bone["bone_index"] = bone_index
        else:
            current_bone = bone

        # add meshes
        if "meshes" in bone_desc:
            for mesh_id in bone_desc["meshes"]:
                mesh = meshes[mesh_id]
                # remember name so we can rig the seleton afterwards in pose mode
                mesh["bone_name"] = current_bone.name

        # add tags to skeleton
        if "tags" in bone_desc:
            for child in bone_desc["tags"]:
                type = child.get("type")
                tag = self.create_bone(context, armature, current_bone, "tag {}".format(type))
                tags[type] = tag
                tag["location"] = json.loads(child.get("translation"))
                if "rotation" in child:
                    rotation_values = json.loads(child.get("rotation"))
                    tag["rotation_quaternion"] = [rotation_values[3], rotation_values[0], rotation_values[1],
                                                  rotation_values[2]]
                if "scale" in child:
                    tag["scale"] = json.loads(child.get("scale"))


        if "childs" in bone_desc:
            for child in bone_desc["childs"]:
                self.process_skeleton_bone_with_armature(context, armature, child, current_bone, animations, meshes, tags)

    def create_skeleton_with_empty(self, context, file_data, meshes):
        animations = list(file_data.get("animations").get("bone_key_frames").values())
        skeleton = file_data.get("meshes").get("skeleton")
        mesh_root = self.create_empty(context, "mesh_root")
        # rotate the model so up is in positive z (blender) instead of negative y (saturn)
        mesh_root.rotation_euler[0] = -math.pi / 2
        # scale to 10%
        mesh_root.scale = [0.1, 0.1, 0.1]

        tags = {}

        for child in skeleton["childs"]:
            self.process_skeleton_bone_with_empty(context, child, mesh_root, animations, meshes, tags)
        return tags

    def process_skeleton_bone_with_empty(self, context, bone_desc, bone, animations, meshes, tags):
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
        #last_mesh = meshes[-1]
        #if last_mesh.parent is None and 0x30 in tags:
        #    last_mesh.parent = tags[0x30]

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