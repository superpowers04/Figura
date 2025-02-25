package net.blancworks.figura.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntityModel.class)
public interface PlayerEntityModelAccessorMixin {

    @Accessor("cloak")
    ModelPart getCloak();

    @Accessor("ear")
    ModelPart getEar();
}
