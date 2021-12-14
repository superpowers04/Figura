package net.blancworks.figura.models.animations;

import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.CustomModelPartGroup;
import net.minecraft.util.math.Vec3f;

public class KeyFrame {
    public final Vec3f data;
    public final float time;
    public final AnimationType rotation;
    public final Interpolation interpolation;
    public final float ownerID;
    public final CustomModelPart modelPart;

    public KeyFrame(Vec3f data, float time, AnimationType rotation, Interpolation interpolation, float ownerID, CustomModelPartGroup modelPart) {
        this.data = data;
        this.time = time;
        this.rotation = rotation;
        this.interpolation = interpolation;
        this.ownerID = ownerID;
        this.modelPart = modelPart;
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
