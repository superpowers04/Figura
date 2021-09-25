package net.blancworks.figura.mixin;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.block.BlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockModelRenderer.class)
public interface BlockModelRendererAccessorMixin {

    @Accessor("colors")
    BlockColors getColors();

}
