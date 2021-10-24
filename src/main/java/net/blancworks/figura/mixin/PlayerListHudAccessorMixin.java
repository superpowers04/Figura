package net.blancworks.figura.mixin;

import com.google.common.collect.Ordering;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessorMixin {
    @Accessor("ENTRY_ORDERING")
    static Ordering<PlayerListEntry> getEntryOrdering() {
        throw new AssertionError();
    }
}
