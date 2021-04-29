package net.blancworks.figura.mixin;

import net.blancworks.figura.*;
import net.blancworks.figura.gui.SetText;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.UUID;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "addChatMessage", at = @At("HEAD"))
    private void addChatMessage(MessageType type, Text message, UUID senderUuid, CallbackInfo ci) {
        if (senderUuid != Util.NIL_UUID && message instanceof TranslatableText && ((TranslatableText) message).getKey().equals("chat.type.text")) {
            Object[] args = ((TranslatableText) message).getArgs();
            if (args.length > 0 && args[0] instanceof MutableText) {
                MutableText playerName = ((MutableText) args[0]);
                PlayerData playerData = PlayerDataManager.getDataForPlayer(senderUuid);
                if (playerData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_NAMEPLATE_MOD_ID)
                        && playerData.script != null && playerData.script.nameplate != null) {
                    NamePlateData data = playerData.script.nameplate;
                    Style style = playerName.getStyle();
                    if (style == null) {
                        style = Style.EMPTY;
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
                    if (style.getColor() == null) {
                        style = style.withColor(TextColor.fromRgb(data.chatRGB));
                    }
                    ((SetText) playerName).figura$setText(data.chatText.replace("%n", playerName.getString())
                            .replace("%u", playerName.getString()));
                    playerName.setStyle(style);
                }
                if (playerData.model != null && Config.chatMark.value)
                    playerName.append(" ").append(new TranslatableText("figura.mark").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

                if (FiguraMod.special.contains(senderUuid) && Config.chatMark.value)
                    playerName.append(" ").append(new TranslatableText("figura.star").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

            }
        }
    }
}
