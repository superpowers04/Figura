package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudListener;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.regex.Pattern;

@Mixin(ChatHudListener.class)
public class ChatHudListenerMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onChatMessage(MessageType type, Text message, UUID senderUuid, CallbackInfo ci) {
        if (senderUuid != Util.NIL_UUID && type == MessageType.CHAT && (boolean) Config.entries.get("chatMods").value) {

            //get player name
            String playerName = "";

            for (PlayerListEntry player : this.client.player.networkHandler.getPlayerList()) {
                UUID entryUUID = player.getProfile().getId();

                if (senderUuid.compareTo(entryUUID) == 0) {
                    playerName = player.getProfile().getName();
                    break;
                }
            }

            //get player data
            PlayerData currentData = PlayerDataManager.getDataForPlayer(senderUuid);

            //player not found or no data
            if (playerName.equals("") || currentData == null)
                return;

            //apply formatting
            NamePlateCustomization nameplateData = currentData.script == null ? null : currentData.script.nameplateCustomizations.get(NamePlateAPI.CHAT);

            if (message instanceof TranslatableText) {
                Object[] args = ((TranslatableText) message).getArgs();

                for (Object arg : args) {
                    if (NamePlateAPI.applyFormattingRecursive((LiteralText) arg, senderUuid, playerName, nameplateData, currentData))
                        break;
                }
            }
            else {
                NamePlateAPI.applyFormattingRecursive((LiteralText) message, senderUuid, playerName, nameplateData, currentData);
            }
        }
    }
}
