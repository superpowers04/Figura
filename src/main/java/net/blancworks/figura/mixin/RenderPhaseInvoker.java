package net.blancworks.figura.mixin;

import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPhase.class)
public interface RenderPhaseInvoker {
    @Invoker("setupGlintTexturing")
    public static void setupGlintTexturing(float f) {
        throw new AssertionError();
    }
}
