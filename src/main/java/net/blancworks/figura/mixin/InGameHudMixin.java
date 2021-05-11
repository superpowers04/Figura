package net.blancworks.figura.mixin;

import net.blancworks.figura.*;
import net.blancworks.figura.gui.SetText;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.network.MessageType;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "addChatMessage", at = @At("HEAD"))
    private void addChatMessage(MessageType type, Text message, UUID senderUuid, CallbackInfo ci) {
        if (senderUuid != Util.NIL_UUID && message instanceof TranslatableText && ((TranslatableText) message).getKey().equals("chat.type.text")) {
            Object[] args = ((TranslatableText) message).getArgs();
            if (args.length > 0 && args[0] instanceof MutableText) {

                Identifier font;
                if ((boolean) Config.entries.get("nameTagIcon").value)
                    font = FiguraMod.FIGURA_FONT;
                else
                    font = Style.DEFAULT_FONT_ID;

                MutableText playerName = ((MutableText) args[0]);
                PlayerData currentData = PlayerDataManager.getDataForPlayer(senderUuid);
                if (currentData != null && currentData.script != null && currentData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_NAMEPLATE_MOD_ID)) {
                    NamePlateData data = currentData.script.nameplate;

                    String formattedText = data.chatText
                            .replace("%n", playerName.getString())
                            .replace("%u", playerName.getString());
                    Style style = playerName.getStyle();
                    if (style.getColor() == null) {
                        style = style.withColor(TextColor.fromRgb(data.chatRGB));
                    }
                    if ((data.chatTextProperties & 0b10000000) != 0b10000000) {
                        style = style.withBold((data.chatTextProperties & 0b00000001) == 0b0000001)
                                .withItalic((data.chatTextProperties & 0b00000010) == 0b0000010)
                                .withUnderline((data.chatTextProperties & 0b00000100) == 0b0000100);
                        if ((data.chatTextProperties & 0b00001000) == 0b00001000) {
                            style = style.withFormatting(Formatting.STRIKETHROUGH);
                        }
                        if ((data.chatTextProperties & 0b00010000) == 0b0010000) {
                            style = style.withFormatting(Formatting.OBFUSCATED);
                        }
                    }

                    ((SetText) playerName).figura$setText(formattedText);
                    playerName.setStyle(style);
                }

                if ((boolean) Config.entries.get("chatMark").value) {
                    LiteralText badges = new LiteralText("");

                    if (currentData != null && currentData.model != null) {
                        if (PlayerDataManager.getDataForPlayer(senderUuid).model.getRenderComplexity() < currentData.getTrustContainer().getFloatSetting(PlayerTrustManager.MAX_COMPLEXITY_ID)) {
                            badges.append(new LiteralText("△").setStyle(Style.EMPTY.withFont(font).withColor(TextColor.parse("white"))));
                        }
                        else {
                            badges.append(new LiteralText("▲").setStyle(Style.EMPTY.withFont(font).withColor(TextColor.parse("white"))));
                        }
                    }

                    if (FiguraMod.special.contains(senderUuid) && (boolean) Config.entries.get("chatMark").value)
                        badges.append(new LiteralText("✭").setStyle(Style.EMPTY.withFont(font).withColor(TextColor.parse("white"))));

                    //apply badges
                    if (!badges.getString().equals(""))
                        playerName.append(new LiteralText(" ").append(badges));
                }
            }
        }
    }
}
