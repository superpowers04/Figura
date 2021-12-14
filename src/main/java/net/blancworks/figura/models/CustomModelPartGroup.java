package net.blancworks.figura.models;

import net.blancworks.figura.models.animations.KeyFrame;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;

public class CustomModelPartGroup extends CustomModelPart {

    public ArrayList<CustomModelPart> children = new ArrayList<>();
    public ArrayList<KeyFrame> keyFrames;

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
            keyFrames = new ArrayList<>();

            //todo
        }

        if (partNbt.contains("chld")) {
            NbtList childrenNbt = (NbtList) partNbt.get("chld");
            if (childrenNbt == null || childrenNbt.getHeldType() != NbtType.COMPOUND)
                return;

            for (NbtElement child : childrenNbt) {
                NbtCompound childNbt = (NbtCompound) child;
                CustomModelPart part = fromNbt(childNbt);
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
