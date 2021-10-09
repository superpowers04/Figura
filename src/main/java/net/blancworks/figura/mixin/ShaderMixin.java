package net.blancworks.figura.mixin;

import net.blancworks.figura.models.shaders.FiguraShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(net.minecraft.client.render.Shader.class)
public class ShaderMixin {
    @Shadow
    private String name;

    @ModifyArg(
            method = "<init>()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Shader;loadProgram(Lnet/minecraft/resource/ResourceFactory;Lnet/minecraft/client/gl/Program$Type;Ljava/lang/String;)Lnet/minecraft/client/gl/Program;"),
            index = 2
    )
    private String modifiedName(String name) {
        if ((Object) this instanceof FiguraShader)
            return this.name.substring(0, "UUIDUUID-UUID-UUID-UUID-UUIDUUIDUUID-".length()) + name;
        return name;
    }
}
