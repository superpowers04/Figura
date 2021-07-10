package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(at = @At("HEAD"), method = "damage")
    public void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerData data = PlayerDataManager.getDataForPlayer(((Entity) (Object) this).getUuid());

        if (data != null && data.script != null) {
            data.script.lastDamageSource = source;
        }
    }

    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    public void playSound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        SoundEvent override = SoundUtils.getSoundOverride(self, sound);
        if (override != null) {
            self.world.playSound((PlayerEntity) self, self.getX(), self.getY(), self.getZ(), override, self.getSoundCategory(), volume, pitch);
            ci.cancel();
        }
    }

    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V", at = @At("HEAD"), cancellable = true)
    public void playSound(SoundEvent event, SoundCategory category, float volume, float pitch, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        SoundEvent override = SoundUtils.getSoundOverride(self, event);
        if (override != null) {
            self.world.playSound((PlayerEntity) self, self.getX(), self.getY(), self.getZ(), override, category, volume, pitch);
            ci.cancel();
        }
    }

}
