package net.blancworks.figura.models.shaders;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * This class only exists because RenderLayer is abstract, and I need to instantiate them.
 * There are practically no other features.
 */

public class FiguraRenderLayer extends RenderLayer {

    //Set at the end of the parse() method in FiguraVertexConsumerProvider
    private CompletableFuture<net.minecraft.client.render.Shader> customShader;

    public FiguraRenderLayer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedSize, boolean crumbling, boolean translucent, Runnable preDraw, Runnable postDraw, CompletableFuture<net.minecraft.client.render.Shader> customShader) {
        super(name, vertexFormat, drawMode, expectedSize, crumbling, translucent, preDraw, postDraw);
        this.customShader = customShader;
    }

    @Nullable
    public net.minecraft.client.render.Shader getShader() {
        if (customShader == null) return null;
        return customShader.getNow(null);
    }
}
