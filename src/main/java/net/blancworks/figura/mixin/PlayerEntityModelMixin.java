package net.blancworks.figura.mixin;

import net.blancworks.figura.access.PlayerEntityModelAccess;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin implements PlayerEntityModelAccess {

    @Shadow @Final private ModelPart cloak;
    @Shadow @Final private ModelPart ear;

    @Override
    public ModelPart getCloak() {
        return this.cloak;
    }

    @Override
    public ModelPart getEar() {
        return this.ear;
    }
}
