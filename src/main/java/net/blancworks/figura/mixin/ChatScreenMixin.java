package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected TextFieldWidget chatField;

    @Inject(at = @At("RETURN"), method = "onChatFieldUpdate")
    private void onChatFieldUpdate(String chatText, CallbackInfo ci) {
        AvatarData data = AvatarDataManager.localPlayer;

        if (data == null || data.script == null)
            return;

        this.chatField.setEditableColor(chatText.startsWith(data.script.commandPrefix) ? (int) Config.ACCENT_COLOR.value : 0xFFFFFF);
    }
}
