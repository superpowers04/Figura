package net.blancworks.figura.mixin;

import net.blancworks.figura.access.FiguraTextAccess;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LiteralText.class)
public abstract class FiguraTextImplMixin implements FiguraTextAccess {
    @Mutable
    @Shadow @Final private String string;

    public boolean figuraText = false;

    @Override
    public void figura$setText(String text) {
        this.string = text;
        ((BaseText) (Object) this).getSiblings().clear();
    }

    @Override
    public void figura$setFigura(boolean bool) {
        this.figuraText = bool;
    }

    @Override
    public boolean figura$getFigura() {
        return this.figuraText;
    }
}
