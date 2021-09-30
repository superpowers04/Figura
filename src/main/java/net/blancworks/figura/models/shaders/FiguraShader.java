package net.blancworks.figura.models.shaders;

import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;

import java.io.IOException;

public class FiguraShader extends Shader {

    public FiguraShader(ResourceFactory factory, String name, VertexFormat format) throws IOException {
        super(factory, name, format);
    }
}
