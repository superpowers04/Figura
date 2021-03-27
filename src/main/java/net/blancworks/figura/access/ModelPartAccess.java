package net.blancworks.figura.access;

import net.minecraft.util.math.Vec3f;

public interface ModelPartAccess {
    void setAdditionalPos(Vec3f v);

    void setAdditionalRot(Vec3f v);

    Vec3f getAdditionalPos();

    Vec3f getAdditionalRot();
}
