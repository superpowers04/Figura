package net.blancworks.figura.models;

import net.blancworks.figura.models.animations.KeyFrame;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;

import java.util.ArrayList;

public class CustomModelPartGroup extends CustomModelPart {

    public ArrayList<CustomModelPart> children = new ArrayList<>();
    public Vec3f animRot = Vec3f.ZERO.copy();
    public Vec3f animPos = Vec3f.ZERO.copy();
    public Vec3f animScale = new Vec3f(1f, 1f, 1f);

    public void clearAnimData() {
        animRot = Vec3f.ZERO.copy();
        animPos = Vec3f.ZERO.copy();
        animScale = new Vec3f(1f, 1f, 1f);
    }

    @Override
    public void clearExtraRendering() {
        super.clearExtraRendering();

        this.children.forEach(CustomModelPart::clearExtraRendering);
    }

    @Override
    public int getComplexity() {
        if (!this.visible) return 0;

        int complexity = 0;

        //iterate over children
        for (CustomModelPart child : this.children)
            complexity += child.getComplexity();

        return complexity;
    }

    @Override
    public void rebuild(Vec2f newTexSize) {
        super.rebuild(newTexSize);

        this.children.forEach(child -> child.rebuild(texSize));
    }

    @Override
    public void readNbt(NbtCompound partNbt) {
        super.readNbt(partNbt);

        if (partNbt.contains("ptype")) {
            try {
                this.parentType = ParentType.valueOf(partNbt.getString("ptype"));
            } catch (Exception ignored) {
                this.parentType = ParentType.Model;
            }
        }

        if (partNbt.contains("mmc")) {
            this.isMimicMode = partNbt.getByte("mmc") == 1;
        }

        if (partNbt.contains("anims")) {
            NbtList animList = partNbt.getList("anims", NbtElement.COMPOUND_TYPE);
            if (animList != null) {
                for (NbtElement nbtElement : animList) {
                    NbtCompound animTag = (NbtCompound) nbtElement;

                    String animationID = animTag.getString("id");

                    ArrayList<KeyFrame> keyFrames = new ArrayList<>();
                    NbtList keyFrameList = animTag.getList("keyf", NbtElement.COMPOUND_TYPE);
                    if (keyFrameList != null) {
                        for (NbtElement nbtElement2 : keyFrameList) {
                            NbtCompound keyFrameTag = (NbtCompound) nbtElement2;
                            keyFrames.add(KeyFrame.fromNbt(keyFrameTag));
                        }
                    }

                    this.model.animations.get(animationID).keyFrames.put(this, keyFrames);
                }
            }
        }

        if (partNbt.contains("chld")) {
            NbtList childrenNbt = (NbtList) partNbt.get("chld");
            if (childrenNbt == null || childrenNbt.getHeldType() != NbtType.COMPOUND)
                return;

            for (NbtElement child : childrenNbt) {
                NbtCompound childNbt = (NbtCompound) child;
                CustomModelPart part = fromNbt(childNbt, this.model);
                if (part != null) this.children.add(part);
            }
        }
    }

    @Override
    public PartType getPartType() {
        return PartType.GROUP;
    }

    @Override
    public void applyUVMods(Vec2f v) {
        super.applyUVMods(v);

        children.forEach(child -> {
            child.UVCustomizations = UVCustomizations;
            child.applyUVMods(v);
        });
    }
}
