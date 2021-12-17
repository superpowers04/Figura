package net.blancworks.figura.mixin;

import com.mojang.authlib.GameProfile;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockEntityRenderer.class)
public abstract class SkullBlockEntityRendererMixin {

    @Unique private static AvatarData data;

    @Inject(method = "renderSkull", at = @At(value = "HEAD"), cancellable = true)
    private static void renderSkull(Direction direction, float yaw, float animationProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, SkullBlockEntityModel model, RenderLayer renderLayer, CallbackInfo ci) {
        if (!(boolean) Config.CUSTOM_PLAYER_HEADS.value || data == null || data.model == null || data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 0)
            return;

        matrices.push();
        if (direction == null) {
            matrices.translate(0.5D, 0.0D, 0.5D);
        } else {
            matrices.translate((0.5F - (float) direction.getOffsetX() * 0.25F), 0.25D, (0.5F - (float) direction.getOffsetZ() * 0.25F));
        }
        matrices.scale(-1.0F, -1.0F, 1.0F);

        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(yaw));

        //render skull :3
        if (data.model.renderSkull(matrices, data.tryGetImmediate(), light))
            ci.cancel();

        matrices.pop();
    }

    @Inject(method = "getRenderLayer", at = @At(value = "HEAD"))
    private static void getRenderLayer(SkullBlock.SkullType type, GameProfile profile, CallbackInfoReturnable<RenderLayer> cir) {
        data = null;

        if (profile != null && profile.getId() != null)
            data = AvatarDataManager.getDataForPlayer(profile.getId());
    }
}
