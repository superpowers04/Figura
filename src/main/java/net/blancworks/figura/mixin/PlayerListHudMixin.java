package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(at = @At("RETURN"), method = "getPlayerName", cancellable = true)
    private void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text text = cir.getReturnValue();

        Identifier font;
        if ((boolean) Config.entries.get("nameTagIcon").value)
            font = FiguraMod.FIGURA_FONT;
        else
            font = Style.DEFAULT_FONT_ID;

        if (PlayerDataManager.getDataForPlayer(entry.getProfile().getId()).model != null && (boolean) Config.entries.get("listMark").value)
            ((LiteralText) text).append(" ").append(new LiteralText("△").setStyle(Style.EMPTY.withFont(font)));

        if (FiguraMod.special.contains(entry.getProfile().getId()) && (boolean) Config.entries.get("listMark").value)
            ((LiteralText) text).append(" ").append(new LiteralText("✭").setStyle(Style.EMPTY.withFont(font)));

        cir.setReturnValue(text);
    }
}
