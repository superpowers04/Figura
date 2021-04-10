package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.models.FiguraTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Function;

@Mixin(PlayerEntityModel.class)
public class PlayerModelRendererMixin<T extends LivingEntity> extends BipedEntityModel<T> {
    public PlayerModelRendererMixin(float scale) {
        super(scale);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        super.render(matrices, vertices, light, overlay, red, green, blue, alpha);

        PlayerData playerData = FiguraMod.currentData;

        if (playerData != null) {
            PlayerDataManager.checkForPlayerDataRefresh(playerData);

            if (playerData.model != null) {
                if (playerData.texture == null || !playerData.texture.ready)
                    return;

                VertexConsumer actualConsumer = FiguraMod.vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(playerData.texture.id));
                matrices.push();

                try {
                    playerData.model.render((PlayerEntityModel<T>) (Object) this, matrices, actualConsumer, light, overlay, 1, 1, 1, 1);
                    for (int i = 0; i < playerData.extraTextures.size(); i++) {
                        FiguraTexture texture = playerData.extraTextures.get(i);

                        if (!texture.ready) {
                            continue;
                        }

                        Function<Identifier, RenderLayer> renderLayerGetter = FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.get(texture.type);

                        if (renderLayerGetter != null) {
                            actualConsumer = FiguraMod.vertexConsumerProvider.getBuffer(renderLayerGetter.apply(texture.id));
                            playerData.model.render((PlayerEntityModel<T>) (Object) this, matrices, actualConsumer, light, overlay, red, green, blue, alpha);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                matrices.pop();
            }
        }
    }
}
