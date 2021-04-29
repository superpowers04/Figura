package net.blancworks.figura.mixin;

import net.blancworks.figura.*;
import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private ArrayList<ModelPart> figura$customizedParts = new ArrayList<>();

    PlayerEntityRendererMixin(EntityRenderDispatcher dispatcher, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(dispatcher, model, shadowRadius);
    }

    @Inject(at = @At("HEAD"), method = "render")
    public void onRender(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        FiguraMod.setRenderingData(abstractClientPlayerEntity, vertexConsumerProvider, this.getModel(), MinecraftClient.getInstance().getTickDelta());

        if (FiguraMod.currentData != null) {
            if (FiguraMod.currentData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_VANILLA_MOD_ID)) {
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_HEAD, this.getModel().head);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_TORSO, this.getModel().torso);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_ARM, this.getModel().leftArm);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_ARM, this.getModel().rightArm);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_LEG, this.getModel().leftLeg);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_LEG, this.getModel().rightLeg);

                figura$applyPartCustomization(VanillaModelAPI.VANILLA_HAT, this.getModel().helmet);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_JACKET, this.getModel().jacket);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_SLEEVE, this.getModel().leftSleeve);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_SLEEVE, this.getModel().rightSleeve);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_PANTS, this.getModel().leftPantLeg);
                figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_PANTS, this.getModel().rightPantLeg);
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void postRender(AbstractClientPlayerEntity abstractClientPlayerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (FiguraMod.currentData != null) {
            PlayerData currData = FiguraMod.currentData;

            if (currData.script != null && currData.script.isDone) {
                for (VanillaModelAPI.ModelPartTable partTable : currData.script.vanillaModelPartTables) {
                    partTable.updateFromPart();
                }
            }
        }
        
        FiguraMod.clearRenderingData();
        figura$clearAllPartCustomizations();
    }

    @Inject(at = @At("HEAD"), method = "renderArm")
    private void onRenderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        FiguraMod.setRenderingData(player, vertexConsumers, this.getModel(), MinecraftClient.getInstance().getTickDelta());

        figura$applyPartCustomization(VanillaModelAPI.VANILLA_HEAD, this.getModel().head);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_TORSO, this.getModel().torso);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_ARM, this.getModel().leftArm);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_ARM, this.getModel().rightArm);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_LEG, this.getModel().leftLeg);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_LEG, this.getModel().rightLeg);

        figura$applyPartCustomization(VanillaModelAPI.VANILLA_HAT, this.getModel().helmet);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_JACKET, this.getModel().jacket);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_SLEEVE, this.getModel().leftSleeve);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_SLEEVE, this.getModel().rightSleeve);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_LEFT_PANTS, this.getModel().leftPantLeg);
        figura$applyPartCustomization(VanillaModelAPI.VANILLA_RIGHT_PANTS, this.getModel().rightPantLeg);
    }

    @Redirect(method = "renderLabelIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", ordinal = 1))
    private<T extends Entity> void renderFiguraLabelIfPresent(LivingEntityRenderer livingEntityRenderer, T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!FiguraMod.currentData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_NAMEPLATE_MOD_ID)) {
            super.renderLabelIfPresent((AbstractClientPlayerEntity) entity, text, matrices, vertexConsumers, light);
            return;
        }
        PlayerData playerData = PlayerDataManager.getDataForPlayer(entity.getUuid());
        if (playerData != null) {
            NamePlateData data = playerData.nameplate;
            if (!data.enabled) return;
            String formattedText = data.text
                    .replace("%n", text.getString())
                    .replace("%h", String.valueOf(((LivingEntity) entity).getHealth()))
                    .replace("%u", entity.getName().getString());
            Style style = text.getStyle();
            if (style.getColor() == null) {
                style = style.withColor(TextColor.fromRgb(playerData.nameplate.RGB));
            }
            if ((data.textProperties & 0b10000000) != 0b10000000) {
                style = style.withBold((data.textProperties & 0b00000001) == 0b0000001)
                        .withItalic((data.textProperties & 0b00000010) == 0b0000010)
                        .withUnderline((data.textProperties & 0b00000100) == 0b0000100);
                if ((data.textProperties & 0b00001000) == 0b00001000) {
                    style = style.withFormatting(Formatting.STRIKETHROUGH);
                }
                if ((data.textProperties & 0b00001000) == 0b0001000) {
                    style = style.withFormatting(Formatting.OBFUSCATED);
                }
            }
            text = new LiteralText(formattedText).setStyle(style);
        }
        double d = this.dispatcher.getSquaredDistanceToCamera(entity);
        if (!(d > 4096.0D)) {
            boolean bl = !entity.isSneaky();
            float f = entity.getHeight() + 0.5F;
            matrices.push();
            if (playerData == null) {
                matrices.translate(0.0D, f, 0.0D);
            } else {
                matrices.translate(playerData.nameplate.position.getX(), playerData.nameplate.position.getY(), playerData.nameplate.position.getZ());
            }
            matrices.multiply(this.dispatcher.getRotation());
            matrices.scale(-0.025F, -0.025F, 0.025F);
            Matrix4f matrix4f = matrices.peek().getModel();
            float g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
            int j = (int)(g * 255.0F) << 24;
            TextRenderer textRenderer = this.getFontRenderer();
            float h = (float)(-textRenderer.getWidth(text) / 2);
            textRenderer.draw(text, h, 0, 553648127, false, matrix4f, vertexConsumers, bl, j, light);
            if (bl) {
                textRenderer.draw((Text)text, h, 0, -1, false, matrix4f, vertexConsumers, false, 0, light);
            }

            matrices.pop();
        }
    }

    @Inject(at = @At("RETURN"), method = "renderArm")
    private void postRenderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        PlayerEntityRenderer realRenderer = (PlayerEntityRenderer) (Object) this;
        PlayerEntityModel model = realRenderer.getModel();
        PlayerData playerData = FiguraMod.currentData;

        if (playerData != null && playerData.model != null) {
            //Only render if texture is ready
            if (playerData.texture == null || !playerData.texture.isDone) {
                FiguraMod.clearRenderingData();
                figura$clearAllPartCustomizations();
                return;
            }

            arm.pitch = 0;

            playerData.model.renderArm(playerData, matrices, vertexConsumers, light, player, arm, sleeve, model);
        }

        FiguraMod.clearRenderingData();
        figura$clearAllPartCustomizations();
    }

    @Inject(at = @At("HEAD"), method = "renderLabelIfPresent")
    protected void renderLabelIfPresent(AbstractClientPlayerEntity abstractClientPlayerEntity, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo inf) {
        if (PlayerDataManager.getDataForPlayer(abstractClientPlayerEntity.getUuid()).model != null && Config.nameTagMark.value)
            ((LiteralText) text).append(" ").append(new TranslatableText("figura.mark").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

        if (FiguraMod.special.contains(abstractClientPlayerEntity.getUuid()) && Config.nameTagMark.value)
            ((LiteralText) text).append(" ").append(new TranslatableText("figura.star").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
    }

    public void figura$applyPartCustomization(String id, ModelPart part) {
        PlayerData data = FiguraMod.currentData;

        if (data != null && data.script != null && data.script.allCustomizations != null) {
            VanillaModelPartCustomization customization = data.script.allCustomizations.get(id);

            if (customization != null) {
                ((ModelPartAccess) part).figura$setPartCustomization(customization);
                figura$customizedParts.add(part);
            }
        }
    }

    public void figura$clearAllPartCustomizations() {
        for (ModelPart part : figura$customizedParts) {
            ((ModelPartAccess) part).figura$setPartCustomization(null);
        }
        figura$customizedParts.clear();
    }

    @Shadow
    @Override
    public Identifier getTexture(AbstractClientPlayerEntity entity) {
        return null;
    }
}