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

    public KeyFrame previousKeyFrame;
    public KeyFrame nextKeyFrame;

    public KeyFrame(float time) {
        this.time = time;
    }

    public enum AnimationType {
        POSITION,
        ROTATION,
        SCALE
    }

    public enum Interpolation {
        LINEAR,
        CATMULLROM
    }

    public KeyFrame getNext(AnimationType type) {
        if (nextKeyFrame == this) return this;

        switch (type) {
            case POSITION -> {
                if (nextKeyFrame.pos == null) return nextKeyFrame.getNext(type);
                else return nextKeyFrame;
            }
            case ROTATION -> {
                if (nextKeyFrame.rot == null) return nextKeyFrame.getNext(type);
                else return nextKeyFrame;
            }
            case SCALE -> {
                if (nextKeyFrame.scale == null) return nextKeyFrame.getNext(type);
                else return nextKeyFrame;
            }
        }

        return this;
    }

    public KeyFrame getPrevious(AnimationType type) {
        if (previousKeyFrame == this) return this;

        switch (type) {
            case POSITION -> {
                if (previousKeyFrame.pos == null) return previousKeyFrame.getPrevious(type);
                else return previousKeyFrame;
            }
            case ROTATION -> {
                if (previousKeyFrame.rot == null) return previousKeyFrame.getPrevious(type);
                else return previousKeyFrame;
            }
            case SCALE -> {
                if (previousKeyFrame.scale == null) return previousKeyFrame.getPrevious(type);
                else return previousKeyFrame;
            }
        }

        return this;
    }

    public KeyFrame getFirst() {
        if (previousKeyFrame == this) return this;
        else if (previousKeyFrame.time <= this.time) return previousKeyFrame.getFirst();
        else return this;
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
        AnimationType type = AnimationType.valueOf(tag.getString("type").toUpperCase());
        Interpolation interpolation = Interpolation.valueOf(tag.getString("int").toUpperCase());

        Translation ts = new Translation(data, interpolation);
        switch (type) {
            case POSITION -> ret.pos = ts;
            case ROTATION -> ret.rot = ts;
            case SCALE -> ret.scale = ts;
        }

        return ret;
    }

    @Override
    public int compareTo(KeyFrame arg) {
        return Float.compare(this.time, arg.time);
    }

    public static record Translation(Vec3f offset, Interpolation lerpMode) {}
}
