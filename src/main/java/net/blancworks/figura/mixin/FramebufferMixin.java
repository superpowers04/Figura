package net.blancworks.figura.mixin;

import net.blancworks.figura.lua.api.RenderLayerAPI;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

//https://gist.github.com/burgerguy/8233170683ad93eea6aa27ee02a5c4d1
//thank you!

@Mixin (Framebuffer.class)
public class FramebufferMixin {
    @ModifyArgs (method = "initFbo",
            at = @At (value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V",
                    ordinal = 0))
    public void init(Args args) {
        if (!RenderLayerAPI.areIrisShadersEnabled()) {
            args.set(2, GL30.GL_DEPTH32F_STENCIL8);
            args.set(6, GL30.GL_DEPTH_STENCIL);
            args.set(7, GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV);
        }
    }

    @ModifyArgs (method = "initFbo",
            at = @At (value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glFramebufferTexture2D(IIIII)V"),
            slice = @Slice (from = @At (value = "FIELD", target = "Lnet/minecraft/client/gl/Framebuffer;useDepthAttachment:Z", ordinal = 1)))
    public void init2(Args args) {
        if (!RenderLayerAPI.areIrisShadersEnabled()) {
            args.set(1, GL30.GL_DEPTH_STENCIL_ATTACHMENT);
        }
    }
}
