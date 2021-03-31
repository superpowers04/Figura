package net.blancworks.figura.access;

import net.minecraft.client.util.math.Vector3f;

public interface ModelPartAccess {
    void setAdditionalPos(Vector3f v);

    void setAdditionalRot(Vector3f v);

    Vector3f getAdditionalPos();

    Vector3f getAdditionalRot();
}
