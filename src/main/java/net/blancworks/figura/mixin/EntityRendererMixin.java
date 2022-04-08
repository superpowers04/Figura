package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        AvatarData data = entity instanceof PlayerEntity ? AvatarDataManager.getDataForPlayer(entity.getUuid()) : AvatarDataManager.getDataForEntity(entity);
        if (data != null && data.getTrustContainer().getTrust(TrustContainer.Trust.OFFSCREEN_RENDERING) == 1)
            cir.setReturnValue(true);
    }
}
