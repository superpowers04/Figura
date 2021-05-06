package net.blancworks.figura.mixin;

import net.blancworks.figura.*;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
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

        PlayerData currentData = PlayerDataManager.getDataForPlayer(entry.getProfile().getId());
        if (currentData != null && currentData.script != null && currentData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_NAMEPLATE_MOD_ID)) {
            NamePlateData data = currentData.script.nameplate;

            String formattedText = data.listText
                    .replace("%n", text.getString())
                    .replace("%u", text.getString());
            Style style = text.getStyle();
            if (style.getColor() == null) {
                style = style.withColor(TextColor.fromRgb(data.listRGB));
            }
            if ((data.listTextProperties & 0b10000000) != 0b10000000) {
                style = style.withBold((data.listTextProperties & 0b00000001) == 0b0000001)
                        .withItalic((data.listTextProperties & 0b00000010) == 0b0000010)
                        .withUnderline((data.listTextProperties & 0b00000100) == 0b0000100);
                if ((data.listTextProperties & 0b00001000) == 0b00001000) {
                    style = style.withFormatting(Formatting.STRIKETHROUGH);
                }
                if ((data.listTextProperties & 0b00010000) == 0b0010000) {
                    style = style.withFormatting(Formatting.OBFUSCATED);
                }
            }

            text = new LiteralText("");
            ((LiteralText) text).append(new LiteralText(formattedText).setStyle(style));
        }

        Identifier font;
        if ((boolean) Config.entries.get("nameTagIcon").value)
            font = FiguraMod.FIGURA_FONT;
        else
            font = Style.DEFAULT_FONT_ID;

        if ((boolean) Config.entries.get("nameTagMark").value) {
            LiteralText badges = new LiteralText("");

            if (currentData != null && currentData.model != null) {
                if (currentData.model.getRenderComplexity() < currentData.getTrustContainer().getFloatSetting(PlayerTrustManager.MAX_COMPLEXITY_ID)) {
                    badges.append(new LiteralText("△").setStyle(Style.EMPTY.withFont(font).withColor(TextColor.parse("white"))));
                } else {
                    badges.append(new LiteralText("▲").setStyle(Style.EMPTY.withFont(font).withColor(TextColor.parse("white"))));
                }
            }

            if (FiguraMod.special.contains(entry.getProfile().getId()) && (boolean) Config.entries.get("nameTagMark").value)
                badges.append(new LiteralText("✭").setStyle(Style.EMPTY.withFont(font).withColor(TextColor.parse("white"))));

            //apply badges
            if (!badges.getString().equals(""))
                ((LiteralText) text).append(new LiteralText(" ").append(badges));
        }

        cir.setReturnValue(text);
    }
}
