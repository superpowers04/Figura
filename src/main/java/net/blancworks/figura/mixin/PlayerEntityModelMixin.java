package net.blancworks.figura.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.PlayerEntityModelAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin<T extends LivingEntity> extends BipedEntityModel<T> implements PlayerEntityModelAccess {

    @Shadow @Final public ModelPart jacket;
    @Shadow @Final public ModelPart leftPantLeg;
    @Shadow @Final public ModelPart leftSleeve;
    @Shadow @Final public ModelPart rightSleeve;
    @Shadow @Final public ModelPart rightPantLeg;
    @Shadow @Final private ModelPart ears;
    private HashSet<String> disabled_parts = new HashSet<String>();

    public PlayerEntityModelMixin(float scale) {
        super(scale);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        super.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        PlayerData playerData = FiguraMod.getCurrData();

        if (playerData != null) {
            if (playerData.model != null) {
                if (playerData.texture == null || playerData.texture.ready == false) {
                    return;
                }
                //We actually wanna use this custom vertex consumer, not the one provided by the render arguments.
                VertexConsumer actualConsumer = FiguraMod.vertex_consumer_provider.getBuffer(RenderLayer.getEntityCutout(playerData.texture.id));
                playerData.model.render((PlayerEntityModel<?>) (Object) this, matrices, actualConsumer, light, overlay, red, green, blue, alpha);
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "setVisible(Z)V")
    public void setVisible(boolean visible, CallbackInfo ci) {
        PlayerEntityModel mdl = (PlayerEntityModel) (Object) this;

        for (String part : disabled_parts) {
            switch (part) {
                case "HEAD":
                    head.visible = false;
                    ears.visible = false;
                    break;
                case "TORSO":
                    torso.visible = false;
                    jacket.visible = false;
                    break;
                case "LEFT_ARM":
                    leftArm.visible = false;
                    leftSleeve.visible = false;
                    break;
                case "RIGHT_ARM":
                    rightArm.visible = false;
                    rightSleeve.visible = false;
                    break;
                case "LEFT_LEG":
                    leftLeg.visible = false;
                    leftPantLeg.visible = false;
                    break;
                case "RIGHT_LEG":
                    rightLeg.visible = false;
                    rightPantLeg.visible = false;
                    break;
            }
        }
    }

    @Override
    public HashSet<String> getDisabledParts() {
        return disabled_parts;
    }
}
