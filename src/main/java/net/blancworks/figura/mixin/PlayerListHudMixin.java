package net.blancworks.figura.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.gui.PlayerPopup;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.UUID;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Unique private PlayerEntity playerEntity;

    @Inject(at = @At("RETURN"), method = "getPlayerName", cancellable = true)
    private void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text text = cir.getReturnValue();

        if ((boolean) Config.PLAYERLIST_MODIFICATIONS.value) {
            UUID uuid = entry.getProfile().getId();
            String playerName = entry.getProfile().getName();

            PlayerData currentData = PlayerDataManager.getDataForPlayer(uuid);
            if (currentData != null && !playerName.equals("")) {
                NamePlateCustomization nameplateData = currentData.script == null ? null : currentData.script.nameplateCustomizations.get(NamePlateAPI.TABLIST);

                try {
                    if (text instanceof TranslatableText) {
                        Object[] args = ((TranslatableText) text).getArgs();

                        for (Object arg : args) {
                            if (arg instanceof TranslatableText || !(arg instanceof Text))
                                continue;

                            if (NamePlateAPI.applyFormattingRecursive((LiteralText) arg, playerName, nameplateData, currentData))
                                break;
                        }
                    } else if (text instanceof LiteralText) {
                        NamePlateAPI.applyFormattingRecursive((LiteralText) text, playerName, nameplateData, currentData);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        cir.setReturnValue(text);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawableHelper;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIFFIIII)V", shift = At.Shift.BEFORE), method = "render", locals = LocalCapture.CAPTURE_FAILHARD)
    private void render(MatrixStack matrices, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci, ClientPlayNetworkHandler clientPlayNetworkHandler, List<?> list, int i, int j, int l, int m, int n, boolean bl, int q, int r, int s, int t, int u, List<?> list2, List<?> list3, int w, int x, int y, int z, int aa, int ab, PlayerListEntry playerListEntry2, GameProfile gameProfile, PlayerEntity playerEntity, boolean bl2, int ae, int af) {
        this.playerEntity = playerEntity;

        if (PlayerPopup.miniEnabled && x == PlayerPopup.miniSelected) {
            PlayerPopup.data = PlayerDataManager.getDataForPlayer(playerListEntry2.getProfile().getId());

            PlayerPopup.miniSize = list.size();

            matrices.push();
            matrices.translate(aa, ab, 0f);
            PlayerPopup.renderMini(matrices);
            matrices.pop();

            RenderSystem.setShaderTexture(0, playerListEntry2.getSkinTexture());
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawableHelper;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIFFIIII)V"), method = "render")
    private void render(MatrixStack matrices, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        PlayerData data = playerEntity == null ? null : PlayerDataManager.getDataForPlayer(playerEntity.getUuid());

        if (!(boolean) Config.CUSTOM_PLAYER_HEADS.value || data == null || data.model == null || data.getTrustContainer().getTrust(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 0) {
            DrawableHelper.drawTexture(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
            return;
        }

        //draw figura head
        FiguraMod.currentData = data;
        FiguraMod.currentPlayer = (AbstractClientPlayerEntity) playerEntity;

        Window w = MinecraftClient.getInstance().getWindow();
        final double guiScale = w.getScaleFactor();

        RenderSystem.enableScissor((int) (x * guiScale), w.getHeight() - (int) ((regionHeight + y) * guiScale), (int) (regionWidth * guiScale), (int) (regionHeight * guiScale));
        DiffuseLighting.disableGuiDepthLighting();

        MatrixStack stack = new MatrixStack();
        stack.push();

        stack.translate(x + 4, y + 8, 0);
        stack.scale(-16, 16, 16);
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));

        if (!data.model.renderSkull(data, stack, FiguraMod.tryGetImmediate(), 0xF000F0)) {
            matrices.push();
            matrices.translate(0f, 0f, 4f);

            RenderSystem.enableDepthTest();
            RenderSystem.setShaderTexture(0, MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(playerEntity.getUuid()).getSkinTexture());
            DrawableHelper.drawTexture(matrices, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);

            matrices.pop();
        }

        stack.pop();

        RenderSystem.disableScissor();
        DiffuseLighting.enableGuiDepthLighting();
    }
}
