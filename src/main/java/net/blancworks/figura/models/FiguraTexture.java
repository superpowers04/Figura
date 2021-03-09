package net.blancworks.figura.models;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class FiguraTexture extends ResourceTexture {

    public Path filePath;
    public Identifier id;

    public boolean ready = false;

    public FiguraTexture() {
        super(new Identifier("minecraft", "textures/entity/steve.png"));
    }


    public static FiguraTexture load(Path target_path, Identifier id) throws IOException {
        FiguraTexture tex = new FiguraTexture();
        tex.load(target_path);
        tex.id = id;
        return tex;
    }

    public static FiguraTexture load(InputStream stream, Identifier id) throws IOException {
        FiguraTexture tex = new FiguraTexture();
        tex.load(stream);
        tex.id = id;
        return null;
    }


    public void load(Path target_path) {
        MinecraftClient.getInstance().execute(() -> {
            try {
                InputStream stream = new FileInputStream(new File(target_path.toString()));

                NativeImage image = NativeImage.read(stream);
                image.writeFile(new File(FabricLoader.getInstance().getGameDir().resolve("OUTPUT_TEXTURE.png").toString()));

                if (!RenderSystem.isOnRenderThread()) {
                    RenderSystem.recordRenderCall(() -> {
                        uploadTexture(image);
                    });
                } else {
                    uploadTexture(image);
                }
                filePath = target_path;
                ready = true;
            } catch (Exception e) {
                FiguraMod.LOGGER.log(Level.ERROR, e);
            }
        });
    }

    public void load(InputStream inputStream) throws IOException {
        NativeImage image = NativeImage.read(inputStream);

        image.writeFile(new File(FabricLoader.getInstance().getGameDir().resolve("OUTPUT_TEXTURE.png").toString()));

        MinecraftClient.getInstance().execute(() -> {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(() -> {
                    uploadTexture(image);
                });
            } else {
                uploadTexture(image);
            }
        });
    }

    private void uploadTexture(NativeImage image) {
        TextureUtil.allocate(this.getGlId(), image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, true);
        ready = true;
    }


    public void toNBT(CompoundTag tag) throws Exception {

        if (filePath == null) {
            tag.putString("note", "Texture was not loaded from disk, unable to save locally. Identifier is " + id);
            return;
        }

        try {

            InputStream stream = new FileInputStream(new File(filePath.toString()));
            String result = null;

            ByteBuffer byteBuffer = TextureUtil.readAllToByteBuffer(stream);
            byteBuffer.rewind();
            byte[] arr = new byte[byteBuffer.remaining()];
            byteBuffer.get(arr);
            result = Base64.getEncoder().encodeToString(arr);

            tag.putString("img", result);
        } catch (Exception e) {
            FiguraMod.LOGGER.log(Level.ERROR,e);
        }
    }

    public void fromNBT(CompoundTag tag) throws Exception {
        if (!tag.contains("img"))
            return;
        
        CompletableFuture.runAsync(
                () -> {
                    try {
                        Thread.sleep(250);
                        String dataString = tag.getString("img");
                        byte[] result = Base64.getDecoder().decode(dataString);
                        ByteBuffer wrapper = MemoryUtil.memAlloc(result.length);
                        wrapper.put(result);
                        wrapper.rewind();
                        NativeImage image = NativeImage.read(wrapper);

                        MinecraftClient.getInstance().execute(() -> {
                            if (!RenderSystem.isOnRenderThread()) {
                                RenderSystem.recordRenderCall(() -> {
                                    uploadTexture(image);
                                });
                            } else {
                                uploadTexture(image);
                            }
                        });
                    } catch (Exception e) {
                        FiguraMod.LOGGER.log(Level.ERROR, e);
                    }
                },
                Util.getMainWorkerExecutor()
        );

    }
}
