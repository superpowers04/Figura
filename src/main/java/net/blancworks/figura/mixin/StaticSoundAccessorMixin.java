package net.blancworks.figura.mixin;

import net.minecraft.client.sound.StaticSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.ByteBuffer;

@Mixin(StaticSound.class)
public interface StaticSoundAccessorMixin {
    @Accessor("sample")
    ByteBuffer getSample();
}
