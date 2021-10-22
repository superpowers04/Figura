package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.LocalPlayerData;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.ActionWheel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Final public Mouse mouse;
    @Shadow @Nullable public Entity targetedEntity;
    @Shadow @Nullable public ClientPlayerEntity player;
    public boolean actionWheelActive = false;

    @Inject(at = @At("INVOKE"), method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V")
    public void disconnect(Screen screen, CallbackInfo ci) {
        try {
            PlayerDataManager.clearCache();
        } catch (Exception ignored) {}
    }

    @Inject(at = @At("RETURN"), method = "handleInputEvents")
    public void handleInputEvents(CallbackInfo ci) {
        if (FiguraMod.actionWheel.isPressed()) {
            if (ActionWheel.enabled) {
                this.mouse.unlockCursor();
                actionWheelActive = true;
            }
        }
        else if (actionWheelActive) {
            ActionWheel.play();
            this.mouse.lockCursor();
            actionWheelActive = false;
        }

        if (FiguraMod.reloadAvatar.wasPressed()) {
            LocalPlayerData localPlayer = PlayerDataManager.localPlayer;
            if (this.targetedEntity instanceof PlayerEntity player) {
                PlayerData data = PlayerDataManager.getDataForPlayer(player.getUuid());

                if (data != null && data.hasAvatar() && data.isAvatarLoaded()) {
                    PlayerDataManager.clearPlayer(player.getUuid());
                    FiguraMod.sendToast("Figura:", "gui.figura.toast.avatar.reload.title");
                }
            }
            else if (localPlayer != null && localPlayer.hasAvatar() && localPlayer.isAvatarLoaded()) {
                if (!localPlayer.isLocalAvatar)
                    PlayerDataManager.clearLocalPlayer();
                else if (localPlayer.loadedName != null)
                    localPlayer.reloadAvatar();

                FiguraMod.sendToast("Figura:", "gui.figura.toast.avatar.reload.title");
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "setScreen")
    public void setScreen(Screen screen, CallbackInfo ci) {
        if (actionWheelActive) {
            ActionWheel.play();
            actionWheelActive = false;
        }
    }
}
