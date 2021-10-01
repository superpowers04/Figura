package net.blancworks.figura.mixin;

import com.mojang.authlib.GameProfile;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkullBlockEntityRenderer.class)
public abstract class SkullBlockEntityRendererMixin {

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void render(SkullBlockEntity skullBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int j, CallbackInfo ci) {
        if (skullBlockEntity != null) {
            GameProfile owner = skullBlockEntity.getOwner();
            if (owner != null && owner.getId() != null) {
                PlayerEntity player = MinecraftClient.getInstance().world.getPlayerByUuid(owner.getId());

                if (player == null)
                    return;

                PlayerData data = PlayerDataManager.getDataForPlayer(owner.getId());
                if (data != null && data.model != null && data.playerId != null) {
                    FiguraMod.currentData = data;
                    data.lastEntity = player;

                    BlockState state = skullBlockEntity.getCachedState();
                    Direction direction = state.getBlock() instanceof WallSkullBlock ? state.get(WallSkullBlock.FACING) : null;

                    if (direction == null) {
                        matrixStack.translate(0.5D, 0.0D, 0.5D);
                    } else {
                        matrixStack.translate((0.5F - (float) direction.getOffsetX() * 0.25F), 0.25D, (0.5F - (float) direction.getOffsetZ() * 0.25F));
                    }

                    matrixStack.scale(-1.0F, -1.0F, 1.0F);

                    float rotation = direction == null ? state.get(SkullBlock.ROTATION) : (2 + direction.getHorizontal()) * 4;
                    matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(22.5F * rotation));

                    ModelTransform headTransform = data.vanillaModel.head.getTransform();
                    data.vanillaModel.head.setAngles(0f, 0f, 0f);
                    data.model.renderSkull(data, matrixStack, vertexConsumerProvider, light, 1f);
                    data.vanillaModel.head.setTransform(headTransform);

                    ci.cancel();
                }
            }
        }
    }
}
