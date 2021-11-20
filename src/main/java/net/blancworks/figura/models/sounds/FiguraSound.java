package net.blancworks.figura.models.sounds;

import net.minecraft.client.sound.StaticSound;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;

public record FiguraSound(StaticSound sound, String name, byte[] sample, boolean local) {

    public void writeNbt(NbtCompound soundsNbt) {
        if (local) return;

        if (sample == null) throw new RuntimeException("Cannot write nbt with empty sample!");
        soundsNbt.put(name, new NbtByteArray(sample));
    }

    public void close() {
        sound.close();
    }
}
