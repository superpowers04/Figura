package net.blancworks.figura.access;

import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.minecraft.client.util.math.Vector3f;

public interface ModelPartAccess {
    VanillaModelPartCustomization figura$getPartCustomization();
    void figura$setPartCustomization(VanillaModelPartCustomization toSet);
}
