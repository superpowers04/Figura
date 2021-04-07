package net.blancworks.figura.access;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.client.model.ModelPart;

import java.util.Set;

public interface PlayerEntityModelAccess {
    Set<ModelPart> figura$getDisabledParts();

    void figura$setupCustomValuesFromScript(CustomScript script);
    void figura$applyCustomValueForPart(CustomScript script, String accessor, ModelPart part);
}
