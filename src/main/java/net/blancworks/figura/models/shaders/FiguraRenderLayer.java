package net.blancworks.figura.models.shaders;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * This class only exists because RenderLayer is abstract, and I need to instantiate them.
 * There are practically no other features.
 */

public class FiguraRenderLayer extends RenderLayer implements Comparable<FiguraRenderLayer> {

    //Low priority happens earlier
    public int priority = 0;

    public FiguraRenderLayer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedSize, boolean crumbling, boolean translucent, Runnable preDraw, Runnable postDraw) {
        super(name, vertexFormat, drawMode, expectedSize, crumbling, translucent, preDraw, postDraw);
    }

    public int compareTo(FiguraRenderLayer other) {
        return priority - other.priority;
    }
}
