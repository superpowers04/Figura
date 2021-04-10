package net.blancworks.figura.access;

import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;

public interface ModelPartAccess {
    VanillaModelPartCustomization figura$getPartCustomization();
    void figura$setPartCustomization(VanillaModelPartCustomization toSet);
}
