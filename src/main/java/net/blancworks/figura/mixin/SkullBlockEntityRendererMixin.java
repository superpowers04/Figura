package net.blancworks.figura.mixin;

import com.mojang.authlib.GameProfile;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockEntityRenderer.class)
public abstract class SkullBlockEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/util/math/Direction;FLnet/minecraft/block/SkullBlock$SkullType;Lcom/mojang/authlib/GameProfile;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "HEAD"), cancellable = true)
    private static void renderSkull(Direction direction, float yaw, SkullBlock.SkullType skullType, GameProfile gameProfile, float animationProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PlayerData data = FiguraMod.currentData;
        if (data == null || data.model == null || data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 0)
            return;

        matrices.push();
        if (direction == null) {
            matrices.translate(0.5D, 0.0D, 0.5D);
        } else {
            matrices.translate((0.5F - (float) direction.getOffsetX() * 0.25F), 0.25D, (0.5F - (float) direction.getOffsetZ() * 0.25F));
        }
        matrices.scale(-1.0F, -1.0F, 1.0F);

        matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(yaw));

        //render skull :3
        if (data.model.renderSkull(data, matrices, FiguraMod.tryGetImmediate(), light))
            ci.cancel();

        matrices.pop();
    }

    @Inject(method = "method_3578", at = @At(value = "HEAD"))
    private static void getRenderLayer(SkullBlock.SkullType type, GameProfile profile, CallbackInfoReturnable<RenderLayer> cir) {
        PlayerEntity player = null;
        if (profile != null && profile.getId() != null && MinecraftClient.getInstance().world != null)
            player = MinecraftClient.getInstance().world.getPlayerByUuid(profile.getId());

        if (player == null) {
            FiguraMod.currentData = null;
            return;
        }

        FiguraMod.currentData = PlayerDataManager.getDataForPlayer(profile.getId());
        if (FiguraMod.currentData != null) FiguraMod.currentData.lastEntity = player;
    }
}
