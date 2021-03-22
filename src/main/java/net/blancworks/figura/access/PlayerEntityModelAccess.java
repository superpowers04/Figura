package net.blancworks.figura.access;

import net.minecraft.client.model.ModelPart;

import java.util.HashSet;

public interface PlayerEntityModelAccess {
    HashSet<ModelPart> getDisabledParts();
}
