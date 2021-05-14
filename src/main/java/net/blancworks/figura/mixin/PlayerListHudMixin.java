package net.blancworks.figura.mixin;

import net.blancworks.figura.*;
import net.blancworks.figura.access.FiguraTextAccess;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.UUID;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(at = @At("RETURN"), method = "getPlayerName", cancellable = true)
    private void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text text = cir.getReturnValue();

        if ((boolean) Config.entries.get("listMods").value)
            figura$applyFormattingRecursive((LiteralText) text, entry.getProfile().getId(), entry.getProfile().getName());

        cir.setReturnValue(text);
    }

    public boolean figura$applyFormattingRecursive(LiteralText text, UUID uuid, String playerName) {
        //save siblings
        ArrayList<Text> siblings = new ArrayList<>(text.getSiblings());

        //if contains playername
        if (text.getRawString().contains(playerName) && !playerName.equals("")) {

            //save style
            Style style = text.getStyle();

            //split the text
            String[] textSplit = text.getRawString().split(playerName, 2);

            Text playerNameSplitted = new LiteralText(playerName).setStyle(style);

            //transform the text
            Text transformed = figura$applyFiguraNameplateFormatting(playerNameSplitted, uuid);

            //add badges
            ((LiteralText) transformed).append(FiguraMod.getBadges(uuid));

            //return the text
            if (!textSplit[0].equals("")) {
                ((FiguraTextAccess) text).figura$setText(textSplit[0]);
                text.setStyle(style);
                text.append(transformed);
            }
            else {
                ((FiguraTextAccess) text).figura$setText(((LiteralText) transformed).getRawString());
                text.setStyle(transformed.getStyle());
                transformed.getSiblings().forEach(((LiteralText) text)::append);
            }
            if (!textSplit[1].equals("")) {
                text.append(textSplit[1]).setStyle(style);
            }

            //append siblings back
            for (Text sibling : siblings) {
                if (!((FiguraTextAccess) sibling).figura$getFigura())
                    text.append(sibling);
            }

            return true;
        }
        else {
            //iterate over children
            for (Text sibling : siblings) {
                if (figura$applyFormattingRecursive((LiteralText) sibling, uuid, playerName))
                    return true;
            }
        }

        return false;
    }

    public Text figura$applyFiguraNameplateFormatting(Text text, UUID uuid) {
        LiteralText formattedText = new LiteralText(text.getString());

        PlayerData currentData = PlayerDataManager.getDataForPlayer(uuid);
        if (currentData != null && currentData.script != null && currentData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_NAMEPLATE_MOD_ID)) {
            NamePlateData data = currentData.script.nameplate;
            Style style = text.getStyle();

            String formattedString = data.listText
                    .replace("%n", text.getString())
                    .replace("%u", text.getString());
            if (data.listRGB != -1) {
                style = style.withColor(TextColor.fromRgb(data.listRGB));
            }
            if ((data.listTextProperties & 0b10000000) != 0b10000000) {
                if ((data.listTextProperties & 0b0000001) == 0b0000001 && !style.isBold()) {
                    style = style.withBold(true);
                }
                if ((data.listTextProperties & 0b0000010) == 0b0000010 && !style.isItalic()) {
                    style = style.withItalic(true);
                }
                if ((data.listTextProperties & 0b00000100) == 0b00000100 && !style.isUnderlined()) {
                    style = style.withUnderline(true);
                }
                if ((data.listTextProperties & 0b00001000) == 0b00001000 && !style.isStrikethrough()) {
                    style = style.withFormatting(Formatting.STRIKETHROUGH);
                }
                if ((data.listTextProperties & 0b00010000) == 0b0010000 && !style.isObfuscated()) {
                    style = style.withFormatting(Formatting.OBFUSCATED);
                }
            }
            ((FiguraTextAccess) formattedText).figura$setText(formattedString);
            formattedText.setStyle(style);
        }

        return formattedText;
    }
}
