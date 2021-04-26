package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(at = @At("RETURN"), method = "getPlayerName", cancellable = true)
    private void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text text = cir.getReturnValue();

        if (PlayerDataManager.getDataForPlayer(entry.getProfile().getId()).model != null && Config.listMark.value)
            ((LiteralText) text).append(" ").append(new TranslatableText("figura.mark"));

        if (FiguraMod.special.contains(entry.getProfile().getId()) && Config.listMark.value)
            ((LiteralText) text).append(" ").append(new TranslatableText("figura.star"));

        cir.setReturnValue(text);
    }
}
