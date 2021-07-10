package net.blancworks.figura.mixin;

import net.blancworks.figura.utils.SoundUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

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
