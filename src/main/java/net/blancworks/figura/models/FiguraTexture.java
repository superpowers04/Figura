package net.blancworks.figura.models;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

public class FiguraTexture extends ResourceTexture {
    public static final Map<TextureType, Function<Identifier, RenderLayer>> EXTRA_TEXTURE_TO_RENDER_LAYER =
            new ImmutableMap.Builder<TextureType, Function<Identifier, RenderLayer>>()
                    .put(TextureType._emission, RenderLayer::getEyes)
                    .build();

    public byte[] data;
    public Path filePath;
    public Identifier id;
    public TextureType type = TextureType.color;

    public boolean isDone = false;

    public FiguraTexture() {
        super(new Identifier("minecraft", "textures/entity/steve.png"));
    }

    public void loadFromDisk(Path targetPath) {
        try {
            this.filePath = targetPath;
            
            loadFromStream(Files.newInputStream(targetPath));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    public void loadFromStream(InputStream stream){
        try {
            data = IOUtils.toByteArray(stream);
            stream.close();
            
            uploadUsingData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadTexture(NativeImage image) {
        TextureUtil.allocate(this.getGlId(), image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, true);
        this.isDone = true;
    }

    public void writeNbt(CompoundTag nbt) {
        try {
            if (this.data == null) {
                nbt.putString("note", "Texture has no data, cannot save : " + id);
                return;
            }
            nbt.putByteArray("img2", this.data);
            nbt.put("type", StringTag.of(this.type.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readNbt(CompoundTag nbt) {
        if (nbt.contains("img2")) {
            try {
                //Pull data out of NBT tag.
                data = nbt.getByteArray("img2");
                
                //Load using that data
                uploadUsingData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (nbt.contains("img")) { //legacy bloat
            
            try {
                //Pull data out of base64 tag.
                String dataString = nbt.getString("img");
                //Put into array.
                data = Base64.getDecoder().decode(dataString);

                //Load using that data
                uploadUsingData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Grab texture type, if it exists.
        if (nbt.contains("type"))
            type = TextureType.valueOf(nbt.get("type").asString());
    }

    //Uploads the data for the texture to the render system, using the current data array.
    public void uploadUsingData() {
        FiguraMod.doTask(() -> {
            try {
                ByteBuffer wrapper = MemoryUtil.memAlloc(data.length);
                wrapper.put(data);
                wrapper.rewind();
                NativeImage image = NativeImage.read(wrapper);

                //Let the RenderSystem know to upload this texture when it's ready.
                RenderSystem.recordRenderCall(() -> {
                    uploadTexture(image);
                    
                    //IsDone = true whenever we've finished.
                    //Note that we don't need to revert this at any point.
                    //If the texture is reloaded, this entire class is nuked anyway.
                    isDone = true;
                    FiguraMod.LOGGER.warn("Texture Loading Finished");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public enum TextureType {
        color,
        _emission
    }
}