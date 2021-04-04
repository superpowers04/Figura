package net.blancworks.figura.models;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class FiguraTexture extends ResourceTexture {
    public static final Map<TextureType, Function<Identifier, RenderLayer>> EXTRA_TEXTURE_TO_RENDER_LAYER =
            new ImmutableMap.Builder<TextureType, Function<Identifier, RenderLayer>>()
                    .put(TextureType._emission, RenderLayer::getEyes)
                    .build();

    public byte[] data;
    public Path filePath;
    public InputStream inputStream;
    public Identifier id;
    public TextureType type = TextureType.color;

    public boolean isLoading = false;
    public boolean ready = false;

    public FiguraTexture() {
        super(new Identifier("minecraft", "textures/entity/steve.png"));
    }

    public static FiguraTexture load(Path target_path, Identifier id) {
        FiguraTexture tex = new FiguraTexture();
        tex.load(target_path);
        tex.id = id;
        return tex;
    }

    public void load(Path targetPath) {
        MinecraftClient.getInstance().execute(() -> {
            try {
                InputStream stream;
                if (targetPath == null || !Files.exists(targetPath))
                    stream = this.inputStream;
                else
                    stream = Files.newInputStream(targetPath);
                data = IOUtils.toByteArray(stream);
                stream.close();
                ByteBuffer wrapper = MemoryUtil.memAlloc(data.length);
                wrapper.put(data);
                wrapper.rewind();
                NativeImage image = NativeImage.read(wrapper);

                if (!RenderSystem.isOnRenderThread()) {
                    RenderSystem.recordRenderCall(() -> {
                        this.uploadTexture(image);
                    });
                } else {
                    this.uploadTexture(image);
                }
                this.filePath = targetPath;
                this.ready = true;
            } catch (Exception e) {
                FiguraMod.LOGGER.error("Failed to load texture " + targetPath);
                FiguraMod.LOGGER.debug(e.toString());
                PlayerDataManager.clearLocalPlayer();
            }
        });
    }

    private void uploadTexture(NativeImage image) {
        TextureUtil.allocate(this.getGlId(), image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, true);
        this.ready = true;
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
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            data = nbt.getByteArray("img2");
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
                            e.printStackTrace();
                        }
                    },
                    Util.getMainWorkerExecutor()
            );
        } else if (nbt.contains("img")) { //legacy bloat
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            String dataString = nbt.getString("img");
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
                            e.printStackTrace();
                        }
                    },
                    Util.getMainWorkerExecutor()
            );
        }

        if (nbt.contains("type"))
            type = TextureType.valueOf(nbt.get("type").asString());
    }

    public enum TextureType {
        color,
        _emission
    }
}
