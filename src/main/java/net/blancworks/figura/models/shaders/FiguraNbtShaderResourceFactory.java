package net.blancworks.figura.models.shaders;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FiguraNbtShaderResourceFactory implements ResourceFactory {

    private String jsonString;
    private String vertexString;
    private String fragmentString;

    public FiguraNbtShaderResourceFactory(String json, String vert, String frag) {
        jsonString = json;
        vertexString = vert;
        fragmentString = frag;
    }

    @Override
    public Resource getResource(Identifier identifier) throws IOException {
        return new Resource() {

            InputStream stream;

            @Override
            public Identifier getId() {
                return identifier;
            }

            @Override
            public InputStream getInputStream() {
                if (identifier.getPath().endsWith(".json")) {
                    stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
                } else if (identifier.getPath().endsWith(".vsh")) {
                    stream = new ByteArrayInputStream(vertexString.getBytes(StandardCharsets.UTF_8));
                } else if (identifier.getPath().endsWith(".fsh")) {
                    stream = new ByteArrayInputStream(fragmentString.getBytes(StandardCharsets.UTF_8));
                } else {
                    System.out.println("Issue with NBT handling. FiguraNbtShaderResourceFactory.java");
                    stream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
                }
                return stream;
            }

            //Method is unused in shader constructor, so no need to do anything with it
            @Override
            public boolean hasMetadata() {
                return false;
            }

            //Method is unused in shader constructor, so no need to do anything with it
            @Nullable
            @Override
            public <T> T getMetadata(ResourceMetadataReader<T> resourceMetadataReader) {
                return null;
            }

            //This method's result only matters in the event of an exception occurring
            @Override
            public String getResourcePackName() {
                return "Custom Figura Render Layers";
            }

            @Override
            public void close() throws IOException {
                stream.close();
            }
        };
    }
}
