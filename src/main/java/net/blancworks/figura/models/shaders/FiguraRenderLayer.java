package net.blancworks.figura.models.shaders;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;

/**
 * This class only exists because RenderLayer is abstract, and I need to instantiate them.
 * There are (as you can see) no other features.
 */

public class FiguraRenderLayer extends RenderLayer {

    public FiguraRenderLayer(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedSize, boolean crumbling, boolean translucent, Runnable preDraw, Runnable postDraw) {
        super(name, vertexFormat, drawMode, expectedSize, crumbling, translucent, preDraw, postDraw);
    }

}
