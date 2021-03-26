package net.blancworks.figura.models;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class FiguraTexture extends ResourceTexture {

    public byte[] data;
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

    public void load(Path target_path) {
        MinecraftClient.getInstance().execute(() -> {
            try {
                InputStream stream = Files.newInputStream(target_path);
                data = IOUtils.toByteArray(stream);
                stream.close();
                ByteBuffer wrapper = MemoryUtil.memAlloc(data.length);
                wrapper.put(data);
                wrapper.rewind();
                NativeImage image = NativeImage.read(wrapper);

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

    private void uploadTexture(NativeImage image) {
        TextureUtil.allocate(this.getGlId(), image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, true);
        ready = true;
    }


    public void toNBT(CompoundTag tag) {
        try {
            if (data == null) {
                tag.putString("note", "Texture has no data, cannot save : " + id);
                return;
            }
            tag.putByteArray("img2", data);
        } catch (Exception e) {
            FiguraMod.LOGGER.log(Level.ERROR,e);
        }
    }

    public void fromNBT(CompoundTag tag) {
        if (tag.contains("img2")) {
            CompletableFuture.runAsync(
                () -> {
                    try {
                        ByteBuffer wrapper = MemoryUtil.memAlloc(data.length);
                        wrapper.put(tag.getByteArray("img2"));
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
        } else if (tag.contains("img")) { //legacy bloat
            CompletableFuture.runAsync(
                () -> {
                    try {
                        String dataString = tag.getString("img");
                        data = Base64.getDecoder().decode(dataString);
                        ByteBuffer wrapper = MemoryUtil.memAlloc(data.length);
                        wrapper.put(data);
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
}
