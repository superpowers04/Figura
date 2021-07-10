package net.blancworks.figura.mixin;

import net.blancworks.figura.utils.SoundUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {


    @Shadow private ClientWorld world;

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onPlaySound", at = @At("HEAD"), cancellable = true)
    public void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (this.world == null) return;
        PlayerEntity player = this.world.getClosestPlayer(packet.getX(), packet.getY(), packet.getZ(), 5.0D, false);
        if (player == null) return;

        SoundEvent override = SoundUtils.getSoundOverride(player, packet.getSound());
        if (override != null) {
            this.client.world.playSound(player.getBlockPos(), override, packet.getCategory(), packet.getVolume(), packet.getPitch(), true);
            ci.cancel();
        }
    }

}
