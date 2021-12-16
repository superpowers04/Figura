package net.blancworks.figura.models.animations;

import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Vec3f;

public class KeyFrame implements Comparable<KeyFrame> {
    public final float time;

    public Translation pos;
    public Translation rot;
    public Translation scale;

    public KeyFrame head = this;
    public KeyFrame previousKeyFrame;
    public KeyFrame nextKeyFrame;

    public KeyFrame(float time) {
        this.time = time;
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

    public enum DataType {
        position,
        rotation,
        scale
    }

    public KeyFrame getNext(DataType data) {
        if (nextKeyFrame == this) return this;

        switch (data) {
            case position -> {
                if (nextKeyFrame.pos == null) return nextKeyFrame.getNext(data);
                else return nextKeyFrame;
            }
            case rotation -> {
                if (nextKeyFrame.rot == null) return nextKeyFrame.getNext(data);
                else return nextKeyFrame;
            }
            case scale -> {
                if (nextKeyFrame.scale == null) return nextKeyFrame.getNext(data);
                else return nextKeyFrame;
            }
        }

        return this;
    }

    public void merge(KeyFrame kf) {
        if (kf.pos != null) this.pos = kf.pos;
        if (kf.rot != null) this.rot = kf.rot;
        if (kf.scale != null) this.scale = kf.scale;
    }

    public static KeyFrame fromNbt(NbtCompound tag) {
        float time = tag.getFloat("time");
        KeyFrame ret = new KeyFrame(time);

        Vec3f data = CustomModelPart.vec3fFromNbt(tag.getList("data", NbtElement.FLOAT_TYPE));
        AnimationType type = AnimationType.valueOf(tag.getString("type"));
        Interpolation interpolation = Interpolation.valueOf(tag.getString("int"));

        Translation ts = new Translation(data, interpolation);
        switch (type) {
            case position -> ret.pos = ts;
            case rotation -> ret.rot = ts;
            case scale -> ret.scale = ts;
        }

        return ret;
    }

    @Override
    public int compareTo(KeyFrame arg) {
        return Float.compare(this.time, arg.time);
    }

    public static record Translation(Vec3f offset, Interpolation lerpMode) {}
}
