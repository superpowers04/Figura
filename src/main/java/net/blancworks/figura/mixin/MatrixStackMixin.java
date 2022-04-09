package net.blancworks.figura.mixin;

import net.blancworks.figura.access.MatrixStackAccess;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Deque;

@Mixin(MatrixStack.class)
public class MatrixStackMixin implements MatrixStackAccess {
    @Final
    @Shadow
    private Deque<MatrixStack.Entry> stack;
    
    public void pushEntry(MatrixStack.Entry entry){
        stack.addLast(entry);
    }
}
