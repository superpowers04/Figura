package net.blancworks.figura.models.animations;

import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Vec3f;

public class KeyFrame {
    public final Vec3f data;
    public final float time;
    public final AnimationType type;
    public final Interpolation interpolation;

    public KeyFrame(Vec3f data, float time, AnimationType type, Interpolation interpolation) {
        this.data = data;
        this.time = time;
        this.type = type;
        this.interpolation = interpolation;
    }

    public enum AnimationType {
        position,
        rotation,
        scale
    }

    public enum Interpolation {
        linear,
        catmullrom
    }

    public static KeyFrame fromNbt(NbtCompound tag) {
        Vec3f data = CustomModelPart.vec3fFromNbt(tag.getList("data", NbtElement.FLOAT_TYPE));
        float time = tag.getFloat("time");
        AnimationType type = AnimationType.valueOf(tag.getString("type"));
        Interpolation interpolation = Interpolation.valueOf(tag.getString("int"));

        return new KeyFrame(data, time, type, interpolation);
    }
}
