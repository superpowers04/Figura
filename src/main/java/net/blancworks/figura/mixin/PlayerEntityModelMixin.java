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

    @Shadow @Final private ModelPart cape;
    @Shadow @Final private ModelPart ears;

    @Override
    public ModelPart getCloak() {
        return this.cape;
    }

    @Override
    public ModelPart getEar() {
        return this.ears;
    }
}
