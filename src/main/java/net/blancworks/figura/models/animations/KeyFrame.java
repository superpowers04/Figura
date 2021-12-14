package net.blancworks.figura.models.animations;

import net.minecraft.util.math.Vec3f;

public class KeyFrame {
    public final Vec3f data;
    public final float time;
    public final AnimationType rotation;
    public final Interpolation interpolation;
    public final float ownerID;

    public KeyFrame(Vec3f data, float time, AnimationType rotation, Interpolation interpolation, float ownerID) {
        this.data = data;
        this.time = time;
        this.rotation = rotation;
        this.interpolation = interpolation;
        this.ownerID = ownerID;
    }

    public enum AnimationType {
        rotation,
        scale,
        position
    }

    public enum Interpolation {
        linear,
        catmullrom
    }
}
