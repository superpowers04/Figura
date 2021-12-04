package net.blancworks.figura.lua.api.sound;

import net.minecraft.client.sound.StaticSound;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public final class FiguraSound {
    private final StaticSound sound;
    private final String name;
    private final byte[] sample;
    private final boolean local;

    FiguraSound(StaticSound sound, String name, byte[] sample, boolean local) {
        this.sound = sound;
        this.name = name;
        this.sample = sample;
        this.local = local;
    }

    public StaticSound sound() {
        return sound;
    }

    public String name() {
        return name;
    }

    public byte[] sample() {
        return sample;
    }

    public boolean local() {
        return local;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        FiguraSound that = (FiguraSound) obj;
        return Objects.equals(this.sound, that.sound) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.sample, that.sample) &&
                this.local == that.local;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sound, name, sample, local);
    }

    @Override
    public String toString() {
        return "FiguraSound[" +
                "sound=" + sound + ", " +
                "name=" + name + ", " +
                "sample=" + sample + ", " +
                "local=" + local + ']';
    }

    public void writeNbt(CompoundTag soundsNbt) {
        if (local) return;

        if (sample == null) throw new RuntimeException("Cannot write nbt with empty sample!");
        soundsNbt.put(name, new ByteArrayTag(sample));
    }

    public void close() {
        sound.close();
    }
}
