package net.blancworks.figura.models.animations;

import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Vec3f;

public class KeyFrame {
    public final float time;
    public final Vec3f data;
    public final AnimationType type;
    public final Interpolation interpolation;

    public KeyFrame(float time, Vec3f data, AnimationType type, Interpolation interpolation) {
        this.time = time;
        this.data = data;
        this.type = type;
        this.interpolation = interpolation;
    }

    public enum AnimationType {
        POSITION,
        ROTATION,
        SCALE
    }

    public enum Interpolation {
        LINEAR,
        CATMULLROM,
        STEP
    }

    public static KeyFrame fromNbt(NbtCompound tag) {
        float time = tag.getFloat("time");
        Vec3f data = CustomModelPart.vec3fFromNbt(tag.getList("data", NbtElement.FLOAT_TYPE));
        AnimationType type = AnimationType.valueOf(tag.getString("type").toUpperCase());
        Interpolation interpolation = Interpolation.valueOf(tag.getString("int").toUpperCase());

        return new KeyFrame(time, data, type, interpolation);
    }
}
