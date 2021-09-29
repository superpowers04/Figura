package net.blancworks.figura.models.shaders;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.Map;

public class FiguraVertexConsumerProvider extends VertexConsumerProvider.Immediate {

    protected FiguraVertexConsumerProvider(BufferBuilder fallbackBuffer, Map<RenderLayer, BufferBuilder> layerBuilderMap) {
        super(fallbackBuffer, layerBuilderMap);
    }
}
