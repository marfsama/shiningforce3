bl_info = {
    "name": "Import Shining Force Battle Model Animations",
    "blender": (2, 80, 0),
    "category": "Import",
}

import json

import bpy

from bpy_extras.io_utils import ImportHelper
from bpy_extras.object_utils import AddObjectHelper
from bpy.props import (
    BoolProperty,
    BoolVectorProperty,
    EnumProperty,
    FloatProperty,
    FloatVectorProperty,
    StringProperty,
)

ANIMATION_TYPES = {
    0x0:  "idle",
    0x10: "hit",
    0x20: "block",
    0x70: "attack"
}

class ImportSf3BattleAnimation(bpy.types.Operator, ImportHelper):
    """Import Shining Force Battle Animations"""      # Use this as a tooltip for menu items and buttons.
    bl_idname = "import.sf3_btl_anim_import"        # Unique identifier for buttons and menu items to reference.
    bl_label = "SF3 Battle Animation (*.json)"         # Display name in the interface.
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
        return self.read_animations(context, self.filepath)

    def filter_frames(self, animations, min_frame, max_frame):
        return {anim[0]: anim[1] for anim in animations.items() if min_frame <= int(anim[0]) <= max_frame}

    def create_animation_action(self, animations, armature_object, action_name, min_frame, max_frame):
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
                bone_index = bone["bone_index"]
                # find bone in animation bones
                animation = animations[bone_index]
                print(animation)
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

    def read_animations(self, context, filepath):
        print("running read_animations...{}".format(filepath))
        f = open(filepath, 'r', encoding='utf-8')
        data = f.read()
        f.close()

        file_data = json.loads(data)
        armature_object = bpy.context.active_object
        for name, animations in file_data.items():
            type_name = name
            start_frame = 1
            num_frames = 1000
            end_frame = start_frame + num_frames - 1
            self.create_animation_action(list(animations.values()), armature_object, type_name, start_frame, end_frame)

        self.report({'INFO'}, "Imported {} segments".format(len(file_data)))
        return {'FINISHED'}

def menu_func(self, context):
    self.layout.operator(ImportSf3BattleAnimation.bl_idname)

def register():
    bpy.utils.register_class(ImportSf3BattleAnimation)
    bpy.types.TOPBAR_MT_file_import.append(menu_func)

def unregister():
    bpy.utils.unregister_class(ImportSf3BattleAnimation)
    bpy.types.TOPBAR_MT_file_import.remove(menu_func)


# This allows you to run the script directly from Blender's Text editor
# to test the add-on without having to install it.
if __name__ == "__main__":
    register()