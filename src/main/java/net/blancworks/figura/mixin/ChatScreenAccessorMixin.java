package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ChatScreenAccess;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatScreen.class)
public class ChatScreenAccessorMixin implements ChatScreenAccess {

    @Shadow protected TextFieldWidget chatField;

    public TextFieldWidget getChatField() {
        return this.chatField;
    }
}
