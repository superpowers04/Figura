package net.blancworks.figura.mixin;

import net.blancworks.figura.gui.SetText;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.*;

@Mixin(LiteralText.class)
public abstract class SetTextImplMixin implements SetText {
    @Mutable
    @Shadow @Final private String string;

    @Override
    public void figura$setText(String text) {
        this.string = text;
        ((BaseText) (Object) this).getSiblings().clear();
    }
}
