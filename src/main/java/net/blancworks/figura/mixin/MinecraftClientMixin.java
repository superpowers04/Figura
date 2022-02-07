package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.gui.ActionWheel;
import net.blancworks.figura.gui.PlayerPopup;
import net.blancworks.figura.gui.NewActionWheel;
import net.blancworks.figura.lua.api.RenderLayerAPI;
import net.blancworks.figura.lua.api.keybind.FiguraKeybind;
import net.blancworks.figura.lua.api.sound.FiguraSoundManager;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.animations.Animation;
import net.blancworks.figura.models.shaders.FiguraVertexConsumerProvider;
import net.blancworks.figura.trust.TrustContainer;
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
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Final public Mouse mouse;
    @Shadow @Final public InGameHud inGameHud;
    @Shadow @Final public GameOptions options;
    @Shadow @Final private RenderTickCounter renderTickCounter;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public Entity cameraEntity;

    @Unique private boolean wasMouseScriptUnlocked = false;
    @Unique private boolean wereIrisShadersEnabled = false;

    @Inject(at = @At("HEAD"), method = "render")
    public void preRender(boolean tick, CallbackInfo ci) {
        //process animations
        synchronized (AvatarDataManager.LOADED_PLAYER_DATA) {
            for (AvatarData data : AvatarDataManager.LOADED_PLAYER_DATA.values()) {
                if (data == null || data.model == null || !data.model.isDone)
                    continue;

                CustomModel model = data.model;
                model.animRendered = 0;
                model.animMaxRender = data.getTrustContainer().getTrust(TrustContainer.Trust.BB_ANIMATIONS);
                if (model.animMaxRender <= 0)
                    break;

                for (Animation anim : model.animations.values()) {
                    if (anim.playState != Animation.PlayState.STOPPED)
                        model.animRendered = anim.render(model.animRendered, model.animMaxRender);
                }
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "render")
    public void afterRender(boolean tick, CallbackInfo ci) {
        //refresh framebuffers if necessary
        if (RenderLayerAPI.areIrisShadersEnabled() != wereIrisShadersEnabled) {
            //"resize" the framebuffers, causing a full framebuffer reset
            //this happens whenever Iris shaders are toggled on or off
            MinecraftClient.getInstance().onResolutionChanged();
        }
        wereIrisShadersEnabled = RenderLayerAPI.areIrisShadersEnabled();

        //save last framebuffer
        if (FiguraVertexConsumerProvider.isUsingLastFramebuffer) {
            RenderLayerAPI.blitMainFramebuffer(RenderLayerAPI.lastFramebufferCopy);
            FiguraVertexConsumerProvider.isUsingLastFramebuffer = false;
        }

        //clear animations
        synchronized (AvatarDataManager.LOADED_PLAYER_DATA) {
            for (AvatarData data : AvatarDataManager.LOADED_PLAYER_DATA.values()) {
                if (data == null || data.model == null || !data.model.isDone)
                    continue;

                for (Animation anim : data.model.animations.values())
                    anim.clearAnimData();
            }
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"), index = 0)
    public int clearMask(int defaultMask) {
        return defaultMask | GL30.GL_DEPTH_BUFFER_BIT | GL30.GL_COLOR_BUFFER_BIT | GL30.GL_STENCIL_BUFFER_BIT;
    }

    @Inject(at = @At("HEAD"), method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V")
    public void disconnect(Screen screen, CallbackInfo ci) {
        try {
            FiguraSoundManager.getChannel().stopAllSounds();
            AvatarDataManager.clearCache();
        } catch (Exception ignored) {}
    }

    @Inject(at = @At("RETURN"), method = "handleInputEvents")
    public void handleInputEvents(CallbackInfo ci) {
        if (FiguraMod.PANIC_BUTTON.wasPressed()) {
            RenderLayerAPI.restoreDefaults();
            FiguraSoundManager.getChannel().stopAllSounds();
            AvatarData.currentRenderingData = null;
            AvatarDataManager.panic = !AvatarDataManager.panic;
            return;
        }

        if (AvatarDataManager.panic) return;

        if (FiguraMod.ACTION_WHEEL_BUTTON.isPressed()) {
            this.mouse.unlockCursor();
        } else if (ActionWheel.enabled || NewActionWheel.enabled) {
            if ((boolean) Config.ACTION_WHEEL_EXECUTE_ON_CLOSE.value) {
                ActionWheel.play();
                NewActionWheel.play();
            }

            ActionWheel.enabled = false;
            NewActionWheel.enabled = false;

            this.mouse.lockCursor();
        }

        if (FiguraMod.PLAYER_POPUP_BUTTON.isPressed()) {
            if (PlayerPopup.data == null) {
                Entity target = getTargetedEntity();
                if (((PlayerListHudAccessorMixin) this.inGameHud.getPlayerListHud()).isVisible()) {
                    PlayerPopup.miniEnabled = true;
                } else if (target instanceof PlayerEntity player && !target.isInvisibleTo(this.player)) {
                    PlayerPopup.data = AvatarDataManager.getDataForPlayer(player.getUuid());
                } else if (!this.options.getPerspective().isFirstPerson()) {
                    PlayerPopup.data = AvatarDataManager.localPlayer;
                }
            }
        } else if (PlayerPopup.enabled || PlayerPopup.miniEnabled) {
            PlayerPopup.execute();
        }

        for (int i = 0; i < PlayerPopup.BUTTONS.size(); ++i) {
            if (this.options.keysHotbar[i].isPressed()) {
                PlayerPopup.hotbarKeyPressed(i);
            }
        }

        AvatarData data = AvatarDataManager.localPlayer;
        if (data != null && data.script != null && data.script.unlockCursor) {
            this.mouse.unlockCursor();
            wasMouseScriptUnlocked = true;
        } else if (wasMouseScriptUnlocked) {
            this.mouse.lockCursor();
            wasMouseScriptUnlocked = false;
        }
    }

    @Inject(at = @At("HEAD"), method = "setScreen")
    public void setScreen(Screen screen, CallbackInfo ci) {
        if (ActionWheel.enabled || NewActionWheel.enabled) {
            if ((boolean) Config.ACTION_WHEEL_EXECUTE_ON_CLOSE.value) {
                ActionWheel.play();
                NewActionWheel.play();
            }

            ActionWheel.enabled = false;
            NewActionWheel.enabled = false;
        }

        FiguraKeybind.unpressAll();
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
