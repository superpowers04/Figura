package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.ActionWheel;
import net.blancworks.figura.gui.PlayerPopup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Final public Mouse mouse;
    @Shadow @Final public InGameHud inGameHud;
    @Shadow @Final public GameOptions options;
    @Shadow @Final private RenderTickCounter renderTickCounter;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public Entity cameraEntity;

    @Unique public boolean actionWheelActive = false;
    @Unique public boolean playerPopupActive = false;

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
        } else if (actionWheelActive) {
            ActionWheel.play();
            this.mouse.lockCursor();
            actionWheelActive = false;
        }

        if (FiguraMod.playerPopup.isPressed()) {
            if (PlayerPopup.data == null) {
                Entity target = getTargetedEntity();
                if (((PlayerListHudAccessorMixin) this.inGameHud.getPlayerListHud()).isVisible()) {
                    playerPopupActive = true;
                    PlayerPopup.miniEnabled = true;
                } else if (target instanceof PlayerEntity player && !target.isInvisibleTo(this.player)) {
                    playerPopupActive = true;
                    PlayerPopup.data = PlayerDataManager.getDataForPlayer(player.getUuid());
                } else if (!this.options.getPerspective().isFirstPerson()) {
                    playerPopupActive = true;
                    PlayerPopup.data = PlayerDataManager.localPlayer;
                }
            }
        } else if (playerPopupActive) {
            PlayerPopup.execute();
            playerPopupActive = false;
        }

        for (int i = 0; i < 4; ++i) {
            if (this.options.keysHotbar[i].isPressed()) {
                PlayerPopup.hotbarKeyPressed(i);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "setScreen")
    public void setScreen(Screen screen, CallbackInfo ci) {
        if (actionWheelActive) {
            ActionWheel.play();
            actionWheelActive = false;
        }
        else if (playerPopupActive) {
            PlayerPopup.execute();
            playerPopupActive = false;
        }
    }

    @Unique
    private Entity getTargetedEntity() {
        if (cameraEntity == null) return null;

        float maxDistance = 16f;
        float tickDelta = this.renderTickCounter.tickDelta;
        Vec3d vec3d = cameraEntity.getCameraPosVec(tickDelta);

        double raycastDistance = cameraEntity.raycast(maxDistance, tickDelta, false).getPos().squaredDistanceTo(vec3d);

        Vec3d vec3d2 = cameraEntity.getRotationVec(1f);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
        Box box = cameraEntity.getBoundingBox().stretch(vec3d2.multiply(maxDistance)).expand(1f, 1f, 1f);

        EntityHitResult entityHitResult = ProjectileUtil.raycast(cameraEntity, vec3d, vec3d3, box, (entity) -> !entity.isSpectator() && entity.collides(), raycastDistance);

        if (entityHitResult != null) {
            Entity entity = entityHitResult.getEntity();
            Vec3d vec3d4 = entityHitResult.getPos();
            double g = vec3d.squaredDistanceTo(vec3d4);

            if (g < raycastDistance && entity instanceof LivingEntity)
                return entity;
        }

        return null;
    }
}
