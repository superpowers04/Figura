package net.blancworks.figura.models.shaders;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.mixin.RenderPhaseInvokerMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.Program;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.resource.Resource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Pair;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.zip.ZipFile;

public class FiguraVertexConsumerProvider extends VertexConsumerProvider.Immediate {

    private static final ArrayList<String> defaultTextures;
    private final Map<String, FiguraRenderLayer> stringLayerMap = new HashMap<>();
    private final NbtList nbtData = new NbtList();

    protected FiguraVertexConsumerProvider(BufferBuilder fallbackBuffer, Map<RenderLayer, BufferBuilder> layerBuilderMap) {
        super(fallbackBuffer, layerBuilderMap);
    }

    /**
     * Gets the vertex buffer for a certain layer, if it's a custom layer.
     * If it's a default (vanilla) layer, then it defers to the backup immediate
     * The backup immediate will generally be BufferBuilderStorage.getEntityVertexConsumers(), the same one passed in to render().
     * @param renderLayer The RenderLayer which we are getting a buffer for
     * @param backup An immediate which will be queried if this does not have a buffer for the renderLayer.
     * @return The vertex buffer corresponding to that render layer, or the default framebuffer if it doesn't exist.
     */
    public VertexConsumer getBuffer(RenderLayer renderLayer, VertexConsumerProvider backup) {
        BufferBuilder buffer = (BufferBuilder) super.getBuffer(renderLayer); //Attempt to find the buffer builder, if custom.
        if (buffer == this.fallbackBuffer) { //We failed to find it here, so it was probably a default vanilla buffer
            //Access the backup immediate, the vanilla one, to find the buffer instead.
            return backup.getBuffer(renderLayer);
        }
        return buffer; //If we did successfully find the custom buffer, return it.
    }

    //Default backup is the vanilla EntityVertexConsumers
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
        VertexConsumerProvider backup = FiguraMod.vertexConsumerProvider;
        return getBuffer(renderLayer, backup);
    }

    public FiguraRenderLayer getRenderLayer(String name) {
        return stringLayerMap.get(name);
    }

    /**
     * Loads all renderLayers for this avatar and attaches a new FiguraVertexConsumerProvider to the avatar.
     * @param playerData The figura player data this VCP is attached to
     * @param inputStream An input stream containing data for all custom render layers.
     * @param root Can be either a Path or a ZipFile. Represents the file containing the avatar.
     */

    public static void parseLocal(PlayerData playerData, InputStream inputStream, Object root) {
        try {

            Map<RenderLayer, BufferBuilder> layerBufferBuilderMap = new HashMap<>();
            FiguraVertexConsumerProvider newVcp = new FiguraVertexConsumerProvider(new BufferBuilder(256), layerBufferBuilderMap);

            Reader reader = new InputStreamReader(inputStream);
            Map jsonData = FiguraMod.GSON.fromJson(reader, Map.class);

            reader.close();

            //Get arraylist of renderLayers, iterate over them all
            ArrayList<Map> renderLayers = (ArrayList<Map>) jsonData.get("render_layers");
            for (Map layer : renderLayers) {

                NbtCompound layerCompound = new NbtCompound();

                //Get basic information about the renderLayer
                String name = layer.getOrDefault("name", "layerName").toString();
                VertexFormat vertexFormat = vertexFormatMap.get(layer.getOrDefault("vertexFormat", "POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL").toString());
                VertexFormat.DrawMode drawMode = drawModeMap.get(layer.getOrDefault("drawMode", "QUADS").toString());
                int expectedBufferSize = ((Number) layer.getOrDefault("expectedBufferSize", 256)).intValue();
                boolean hasCrumbling = (Boolean) layer.getOrDefault("hasCrumbling", true);
                boolean translucent = (Boolean) layer.getOrDefault("translucent", true);

                //Create the functions that run before and after the renderLayer draws, using parameters
                Map parameters = (Map) layer.getOrDefault("parameters", new HashMap<>());

                //Get values from json for each parameter
                String shaderStr = parameters.getOrDefault("shader", "RENDERTYPE_ENTITY_TRANSLUCENT_SHADER").toString();

                //If the texture value is "MY_TEXTURE", then the chosen texture is the texture.png file of your avatar.
                //If "SKIP", the texture slot is skipped. This is useful if you want to use slots 1 and 2 for overlay or lightmap,
                //As the game will overwrite slots 1 and 2 with overlay and lighting if you enable them.
                //In other cases, the strings are treated as Identifiers.
                ArrayList<String> textures = new ArrayList<>();
                ((List<Object>) parameters.getOrDefault("textures", defaultTextures)).forEach((textureStr) -> textures.add(textureStr.toString()));

                //For the rest, the default value is treated the same as in the normal RenderLayer.MultiPhaseParameters.Builder class.
                Boolean enableLightmap = (Boolean) parameters.getOrDefault("lightmap", false);
                Boolean enableOverlay = (Boolean) parameters.getOrDefault("overlay", false);
                Boolean enableCull = (Boolean) parameters.getOrDefault("cull", true);
                String depthTestStr = parameters.getOrDefault("depthTest", "LEQUAL_DEPTH_TEST").toString();
                String writeMaskStateStr = parameters.getOrDefault("writeMaskState", "ALL_MASK").toString();
                Double lineWidth = (Double) parameters.getOrDefault("lineWidth", 1.0);
                String layeringModeStr = parameters.getOrDefault("layering", "NO_LAYERING").toString();
                String targetStr = parameters.getOrDefault("target", "MAIN_TARGET").toString();
                String transparencyStr = parameters.getOrDefault("transparency", "NO_TRANSPARENCY").toString();
                String texturingModeStr = parameters.getOrDefault("texturing", "DEFAULT_TEXTURING").toString();

                //Put the information we just gathered from json into the nbt tag:
                layerCompound.putString("name", name);
                layerCompound.putString("vertexFormat", layer.getOrDefault("vertexFormat", "POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL").toString());
                layerCompound.putString("drawMode", layer.getOrDefault("drawMode", "QUADS").toString());
                layerCompound.putInt("expectedBufferSize", expectedBufferSize);
                layerCompound.putBoolean("hasCrumbling", hasCrumbling);
                layerCompound.putBoolean("translucent", translucent);
                NbtCompound nbtParams = new NbtCompound();
                NbtList nbtTextures = new NbtList();
                for (String s : textures)
                    nbtTextures.add(nbtTextures.size(), NbtString.of(s));
                nbtParams.put("textures", nbtTextures);
                nbtParams.putBoolean("lightmap", enableLightmap);
                nbtParams.putBoolean("overlay", enableOverlay);
                nbtParams.putBoolean("cull", enableCull);
                nbtParams.putString("depthTest", depthTestStr);
                nbtParams.putString("writeMaskState", writeMaskStateStr);
                nbtParams.putDouble("lineWidth", lineWidth);
                nbtParams.putString("layering", layeringModeStr);
                nbtParams.putString("target", targetStr);
                nbtParams.putString("transparency", transparencyStr);
                nbtParams.putString("texturing", texturingModeStr);

                //Create the shader
                CompletableFuture<Shader> shader = new CompletableFuture<>();
                boolean isVanillaShader = vanillaShaderMap.containsKey(shaderStr);
                FiguraLocalShaderResourceFactory localShaderFactory = null;
                if (isVanillaShader) {
                    //The chosen shader is a vanilla shader, so get it from this map.
                    shader.complete(vanillaShaderMap.get(shaderStr).get());
                } else {
                    //This shader is not a vanilla shader, so create a new one.
                    if (root instanceof ZipFile)
                        localShaderFactory = new FiguraLocalShaderResourceFactory((ZipFile) root);
                    else if (root instanceof Path)
                        localShaderFactory = new FiguraLocalShaderResourceFactory((Path) root);
                    else
                        throw new IllegalArgumentException();
                    FiguraLocalShaderResourceFactory finalShaderFactory = localShaderFactory; //For lambda
                    RenderSystem.recordRenderCall(() -> {
                        try {
                            //Append the UUID of the player to the front of the shader string,
                            //so that two players using shaders with the same name can have different shaders.
                            String playerShaderStr = playerData.playerId.toString()+"-"+shaderStr;

                            //Remove shader from the cache, so it can be changed
                            //Without this, problems arise when:
                            //Saving the file, since the cached version remains
                            //If you load two avatars that have a shader with the same name, the two will use
                            //The same shader.
                            //These following two lines are to prevent this behavior.
                            Program.Type.VERTEX.getProgramCache().put(playerShaderStr, null);
                            Program.Type.FRAGMENT.getProgramCache().put(playerShaderStr, null);

                            Shader customShader = new FiguraShader(finalShaderFactory, playerShaderStr, vertexFormat);
                            shader.complete(customShader);
                            if (root instanceof ZipFile)
                                ((ZipFile) root).close();
                        } catch (IOException e) {
                            if (playerData.isLocalAvatar)
                                CustomScript.sendChatMessage(new LiteralText(e.getMessage()).setStyle(Style.EMPTY.withColor(TextColor.parse("red"))));
                            shader.complete(vanillaShaderMap.get("RENDERTYPE_ENTITY_TRANSLUCENT_SHADER").get());
                        }
                    });
                }

                //Put the shader information into the parameters nbt tag
                if (isVanillaShader) {
                    nbtParams.putString("vanillaShader", shaderStr);
                } else {
                    NbtCompound shaderNbt = new NbtCompound();
                    //Get the shader's json file
                    Identifier jsonIdentifier = new Identifier("shaders/core/uuiduuid-uuid-uuid-uuid-uuiduuiduuid-" + shaderStr + ".json");
                    Resource jsonResource = localShaderFactory.getResource(jsonIdentifier);
                    JsonObject jsonObject = JsonHelper.deserialize(new InputStreamReader(jsonResource.getInputStream(), StandardCharsets.UTF_8));
                    //Get paths to the vertex and fragment shaders
                    String vshPath = "shaders/core/uuiduuid-uuid-uuid-uuid-uuiduuiduuid-" + JsonHelper.getString(jsonObject, "vertex") + ".vsh";
                    String fshPath = "shaders/core/uuiduuid-uuid-uuid-uuid-uuiduuiduuid-" + JsonHelper.getString(jsonObject, "fragment") + ".fsh";
                    //Get input streams for each of the 3 files
                    Resource vshResource = localShaderFactory.getResource(new Identifier(vshPath));
                    Resource fshResource = localShaderFactory.getResource(new Identifier(fshPath));
                    InputStream jsonStream = jsonResource.getInputStream();
                    InputStream vshStream = vshResource.getInputStream();
                    InputStream fshStream = fshResource.getInputStream();
                    //Get strings for each of the 3 files
                    String jsonString = new String(jsonStream.readAllBytes(), StandardCharsets.UTF_8);
                    String vshString = new String(vshStream.readAllBytes(), StandardCharsets.UTF_8);
                    String fshString = new String(fshStream.readAllBytes(), StandardCharsets.UTF_8);

                    //Close resources
                    IOUtils.closeQuietly(jsonResource);
                    IOUtils.closeQuietly(vshResource);
                    IOUtils.closeQuietly(fshResource);

                    //Put nbt
                    shaderNbt.putString("name", shaderStr);
                    shaderNbt.putString("json", jsonString);
                    shaderNbt.putString("vertex", vshString);
                    shaderNbt.putString("fragment", fshString);
                    nbtParams.put("shader", shaderNbt);
                }

                //Put the finalized parameters into the nbt layer
                layerCompound.put("parameters", nbtParams);
                //Insert this new layer into the nbt data
                newVcp.nbtData.addElement(newVcp.nbtData.size(), layerCompound);

                //preDraw: setup all the render system environment variables for this layer.
                Runnable preDraw = getPreDraw(playerData, shader, textures, enableLightmap, enableOverlay, enableCull,
                        depthTestStr, writeMaskStateStr, lineWidth, layeringModeStr, targetStr, transparencyStr, texturingModeStr);

                //post draw: Reset environment variables back to defaults.
                Runnable postDraw = getPostDraw(enableLightmap, enableOverlay, enableCull, depthTestStr, writeMaskStateStr,
                        lineWidth, layeringModeStr, targetStr);

                //Now we have everything we need to actually construct the RenderLayer.
                FiguraRenderLayer renderLayer = new FiguraRenderLayer(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, preDraw, postDraw, shader);

                BufferBuilder bufferBuilder = new BufferBuilder(renderLayer.getExpectedBufferSize());

                //Put the render layer into the map
                layerBufferBuilderMap.put(renderLayer, bufferBuilder);
                //Keep a reference to the render layer by name
                newVcp.stringLayerMap.put(name, renderLayer);
            }

            playerData.customVCP = newVcp;
        } catch(Exception e) {
            e.printStackTrace();
        }
        FiguraMod.LOGGER.info("Local Renderlayer Loading Finished");
    }

    //Split these into their own methods, so we can call them when we want to load from nbt
    private static Runnable getPreDraw(PlayerData playerData, CompletableFuture<Shader> shader, ArrayList<String> textures,
                                       boolean enableLightmap, boolean enableOverlay, boolean enableCull, String depthTestStr,
                                       String writeMaskStateStr, Double lineWidth, String layeringModeStr, String targetStr,
                                       String transparencyStr, String texturingModeStr) {
        return () -> {
            //Set the shader
            try {
                Shader shaderToSet = shader.getNow(null);
                if (shaderToSet != null)
                    RenderSystem.setShader(() -> shaderToSet);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Set textures using the provided strings
            RenderSystem.enableTexture();
            for (int i = 0; i < textures.size(); i++) {
                String textureStr = textures.get(i);
                if (textureStr.equals("MY_TEXTURE")) {
                    RenderSystem.setShaderTexture(i, playerData.texture.id);
                } else if (!textureStr.equals("SKIP")) { //If there is no texture, then don't set the shader texture
                    RenderSystem.setShaderTexture(i, new Identifier(textureStr));
                }
            }

            //Enable lightmap if requested
            if (enableLightmap) {
                MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
            }
            //Enable overlay if requested
            if (enableOverlay) {
                MinecraftClient.getInstance().gameRenderer.getOverlayTexture().setupOverlayColor();
            }
            //Disable culling if requested
            if (!enableCull) {
                RenderSystem.disableCull();
            }
            //Set depth test flags using chosen depth test mode
            int depthTestFlags = depthTestsMap.get(depthTestStr);
            if (depthTestFlags != 519) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(depthTestFlags);
            }
            //Setup write mask using chosen mode
            boolean color = writeMaskStatesMap.get(writeMaskStateStr).getLeft();
            boolean depth = writeMaskStatesMap.get(writeMaskStateStr).getRight();
            if (!depth) {
                RenderSystem.depthMask(false);
            }
            if (!color) {
                RenderSystem.colorMask(false, false, false,false);
            }
            //Setup line width
            if (lineWidth != 1.0) {
                if (lineWidth < 0) {
                    RenderSystem.lineWidth(Math.max(2.5F, (float)MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 1920.0F * 2.5F));
                } else {
                    RenderSystem.lineWidth(lineWidth.floatValue());
                }
            }
            //Setup layering mode
            layeringModesMap.get(layeringModeStr).getLeft().run();
            //Setup target
            if (!targetStr.equals("MAIN_TARGET")) {
                Pair<Boolean, Supplier<Framebuffer>> targetEntry = targetsMap.get(targetStr);
                if (!targetEntry.getLeft() || MinecraftClient.isFabulousGraphicsOrBetter()) {
                    targetEntry.getRight().get().beginWrite(false);
                }
            }

            //Setup transparency mode
            transparencyModesMap.get(transparencyStr).run();
            //Setup texturing mode
            texturingModesMap.get(texturingModeStr).run();
        };
    }

    private static Runnable getPostDraw(boolean enableLightmap, boolean enableOverlay, boolean enableCull, String depthTestStr,
                                        String writeMaskStateStr, Double lineWidth, String layeringModeStr, String targetStr) {
        return () -> {

            //Disable lightmap if it was enabled
            if (enableLightmap) {
                MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
            }

            //Disable overlay if it was enabled
            if (enableOverlay) {
                MinecraftClient.getInstance().gameRenderer.getOverlayTexture().teardownOverlayColor();
            }

            //Re-enable culling if it was disabled
            if (!enableCull) {
                RenderSystem.enableCull();
            }

            //Reset depth test
            int depthTestFlags = depthTestsMap.get(depthTestStr);
            if (depthTestFlags != 519) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthFunc(515);
            }

            //Reset Write mask
            boolean color = writeMaskStatesMap.get(writeMaskStateStr).getLeft();
            boolean depth = writeMaskStatesMap.get(writeMaskStateStr).getRight();
            if (!depth) {
                RenderSystem.depthMask(true);
            }
            if (!color) {
                RenderSystem.colorMask(true, true, true,true);
            }

            //Reset line width
            if (lineWidth != 1.0) {
                RenderSystem.lineWidth(1.0F);
            }

            //Reset layering mode
            layeringModesMap.get(layeringModeStr).getRight().run();

            //Reset target
            if (!targetStr.equals("MAIN_TARGET")) {
                boolean requiresFabulous = targetsMap.get(targetStr).getLeft();
                if (!requiresFabulous || MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
                }
            }

            //Reset transparency
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();

            //Reset Texturing mode (glint)
            RenderSystem.resetTextureMatrix();
        };
    }

    /**
     * Writes the data for the render layers into the provided tag
     */
    public void writeNbt(NbtCompound tag) {
        tag.put("layers", nbtData);
    }

    public static void parseFromNbt(PlayerData playerData, NbtCompound vcpNbt) {
        NbtList layers = vcpNbt.getList("layers", NbtElement.COMPOUND_TYPE);

        Map<RenderLayer, BufferBuilder> layerBufferBuilderMap = new HashMap<>();
        FiguraVertexConsumerProvider newVcp = new FiguraVertexConsumerProvider(new BufferBuilder(256), layerBufferBuilderMap);
        layers.forEach((layer) -> {
            NbtCompound layerCompound = (NbtCompound) layer;
            String name = layerCompound.getString("name");
            VertexFormat vertexFormat = vertexFormatMap.get(layerCompound.getString("vertexFormat"));
            VertexFormat.DrawMode drawMode = drawModeMap.get(layerCompound.getString("drawMode"));
            int expectedBufferSize = layerCompound.getInt("expectedBufferSize");
            boolean hasCrumbling = layerCompound.getBoolean("hasCrumbling");
            boolean translucent = layerCompound.getBoolean("translucent");
            NbtCompound nbtParams = layerCompound.getCompound("parameters");

            NbtList nbtTextures = nbtParams.getList("textures", NbtElement.STRING_TYPE);
            ArrayList<String> textures = new ArrayList<>();
            for(NbtElement nbtStr : nbtTextures)
                textures.add(((NbtString) nbtStr).asString());
            boolean enableLightmap = nbtParams.getBoolean("lightmap");
            boolean enableOverlay = nbtParams.getBoolean("overlay");
            boolean enableCull = nbtParams.getBoolean("cull");
            String depthTestStr = nbtParams.getString("depthTest");
            String writeMaskStateStr = nbtParams.getString("writeMaskState");
            Double lineWidth = nbtParams.getDouble("lineWidth");
            String layeringModeStr = nbtParams.getString("layering");
            String targetStr = nbtParams.getString("target");
            String transparencyStr = nbtParams.getString("transparency");
            String texturingModeStr = nbtParams.getString("texturing");

            CompletableFuture<Shader> shader = new CompletableFuture<>();
            if (nbtParams.contains("vanillaShader")) {
                shader.complete(vanillaShaderMap.get(nbtParams.getString("vanillaShader")).get());
            } else {
                NbtCompound shaderNbt = nbtParams.getCompound("shader");
                String shaderJson = shaderNbt.getString("json");
                String shaderVert = shaderNbt.getString("vertex");
                String shaderFrag = shaderNbt.getString("fragment");
                String shaderStr = shaderNbt.getString("name");
                FiguraNbtShaderResourceFactory nbtShaderFactory = new FiguraNbtShaderResourceFactory(shaderJson, shaderVert, shaderFrag);
                RenderSystem.recordRenderCall(() -> {
                    try {
                        String playerShaderStr = playerData.playerId.toString()+"-"+shaderStr;
                        Program.Type.VERTEX.getProgramCache().put(playerShaderStr, null);
                        Program.Type.FRAGMENT.getProgramCache().put(playerShaderStr, null);
                        Shader customShader = new FiguraShader(nbtShaderFactory, playerShaderStr, vertexFormat);
                        shader.complete(customShader);
                    } catch (IOException e) {
                        shader.complete(vanillaShaderMap.get("RENDERTYPE_ENTITY_TRANSLUCENT_SHADER").get());
                    }
                });
            }

            //preDraw: setup all the render system environment variables for this layer.
            Runnable preDraw = getPreDraw(playerData, shader, textures, enableLightmap, enableOverlay, enableCull,
                    depthTestStr, writeMaskStateStr, lineWidth, layeringModeStr, targetStr, transparencyStr, texturingModeStr);
            //post draw: Reset environment variables back to defaults.
            Runnable postDraw = getPostDraw(enableLightmap, enableOverlay, enableCull, depthTestStr, writeMaskStateStr,
                    lineWidth, layeringModeStr, targetStr);

            FiguraRenderLayer renderLayer = new FiguraRenderLayer(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, preDraw, postDraw, shader);
            BufferBuilder bufferBuilder = new BufferBuilder(renderLayer.getExpectedBufferSize());
            layerBufferBuilderMap.put(renderLayer, bufferBuilder);
            newVcp.stringLayerMap.put(name, renderLayer);
        });

        playerData.customVCP = newVcp;
        FiguraMod.LOGGER.info("External Renderlayer Loading Finished");
    }

    /**
     * To convert strings from the json into actual objects, use these maps
     * There's probably some fancy way to do this with reflection or something,
     * but I don't understand that and it probably wouldn't help much anyway.
     * This section is specially for maps of vanilla classes, in contrast to
     * the above section which has a map to access custom shaders.
     */

    public static final Map<String, VertexFormat> vertexFormatMap;
    public static final Map<String, VertexFormat.DrawMode> drawModeMap;
    public static final Map<String, Supplier<Shader>> vanillaShaderMap;
    public static final Map<String, Integer> depthTestsMap;
    public static final Map<String, Pair<Boolean, Boolean>> writeMaskStatesMap;
    public static final Map<String, Pair<Runnable, Runnable>> layeringModesMap;
    public static final Map<String, Pair<Boolean, Supplier<Framebuffer>>> targetsMap;
    public static final Map<String, Runnable> transparencyModesMap;
    public static final Map<String, Runnable> texturingModesMap;

    static {

        defaultTextures = new ArrayList<>();
        defaultTextures.add("MY_TEXTURE");

        //Vertex formats
        vertexFormatMap = new HashMap<>();
        vertexFormatMap.put("BLIT_SCREEN", VertexFormats.BLIT_SCREEN);
        vertexFormatMap.put("POSITION_COLOR_TEXTURE_LIGHT_NORMAL", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
        vertexFormatMap.put("POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL); //Default
        vertexFormatMap.put("POSITION_TEXTURE_COLOR_LIGHT", VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        vertexFormatMap.put("POSITION", VertexFormats.POSITION);
        vertexFormatMap.put("POSITION_COLOR", VertexFormats.POSITION_COLOR);
        vertexFormatMap.put("LINES", VertexFormats.LINES);
        vertexFormatMap.put("POSITION_COLOR_LIGHT", VertexFormats.POSITION_COLOR_LIGHT);
        vertexFormatMap.put("POSITION_TEXTURE", VertexFormats.POSITION_TEXTURE);
        vertexFormatMap.put("POSITION_COLOR_TEXTURE", VertexFormats.POSITION_COLOR_TEXTURE);
        vertexFormatMap.put("POSITION_TEXTURE_COLOR", VertexFormats.POSITION_TEXTURE_COLOR);
        vertexFormatMap.put("POSITION_COLOR_TEXTURE_LIGHT", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
        vertexFormatMap.put("POSITION_TEXTURE_LIGHT_COLOR", VertexFormats.POSITION_TEXTURE_LIGHT_COLOR);
        vertexFormatMap.put("POSITION_TEXTURE_COLOR_NORMAL", VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);

        //Draw modes
        drawModeMap = new HashMap<>();
        drawModeMap.put("LINES", VertexFormat.DrawMode.LINES);
        drawModeMap.put("LINE_STRIP", VertexFormat.DrawMode.LINE_STRIP);
        drawModeMap.put("DEBUG_LINES", VertexFormat.DrawMode.DEBUG_LINES);
        drawModeMap.put("DEBUG_LINE_STRIP", VertexFormat.DrawMode.DEBUG_LINE_STRIP);
        drawModeMap.put("TRIANGLES", VertexFormat.DrawMode.TRIANGLES);
        drawModeMap.put("TRIANGLE_STRIP", VertexFormat.DrawMode.TRIANGLE_STRIP);
        drawModeMap.put("TRIANGLE_FAN", VertexFormat.DrawMode.TRIANGLE_FAN);
        drawModeMap.put("QUADS", VertexFormat.DrawMode.QUADS); //Default

        //Shaders
        vanillaShaderMap = new HashMap<>();
        vanillaShaderMap.put("POSITION_SHADER", GameRenderer::getPositionShader);
        vanillaShaderMap.put("POSITION_COLOR_SHADER", GameRenderer::getPositionColorShader);
        vanillaShaderMap.put("POSITION_COLOR_TEX_SHADER", GameRenderer::getPositionColorTexShader);
        vanillaShaderMap.put("POSITION_TEX_SHADER", GameRenderer::getPositionTexShader);
        vanillaShaderMap.put("POSITION_TEX_COLOR_SHADER", GameRenderer::getPositionTexColorShader);
        vanillaShaderMap.put("BLOCK_SHADER", GameRenderer::getBlockShader);
        vanillaShaderMap.put("NEW_ENTITY_SHADER", GameRenderer::getNewEntityShader);
        vanillaShaderMap.put("PARTICLE_SHADER", GameRenderer::getParticleShader);
        vanillaShaderMap.put("POSITION_COLOR_LIGHTMAP_SHADER", GameRenderer::getPositionColorLightmapShader);
        vanillaShaderMap.put("POSITION_COLOR_TEX_LIGHTMAP_SHADER", GameRenderer::getPositionColorTexLightmapShader);
        vanillaShaderMap.put("POSITION_TEX_COLOR_NORMAL_SHADER", GameRenderer::getPositionTexColorNormalShader);
        vanillaShaderMap.put("POSITION_TEX_LIGHTMAP_COLOR_SHADER", GameRenderer::getPositionTexLightmapColorShader);
        vanillaShaderMap.put("RENDERTYPE_SOLID_SHADER", GameRenderer::getRenderTypeSolidShader);
        vanillaShaderMap.put("RENDERTYPE_CUTOUT_MIPPED_SHADER", GameRenderer::getRenderTypeCutoutMippedShader);
        vanillaShaderMap.put("RENDERTYPE_CUTOUT_SHADER", GameRenderer::getRenderTypeCutoutShader);
        vanillaShaderMap.put("RENDERTYPE_TRANSLUCENT_SHADER", GameRenderer::getRenderTypeTranslucentShader);
        vanillaShaderMap.put("RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER", GameRenderer::getRenderTypeTranslucentMovingBlockShader);
        vanillaShaderMap.put("RENDERTYPE_TRANSLUCENT_NO_CRUMBLING_SHADER", GameRenderer::getRenderTypeTranslucentNoCrumblingShader);
        vanillaShaderMap.put("RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER", GameRenderer::getRenderTypeArmorCutoutNoCullShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_SOLID_SHADER", GameRenderer::getRenderTypeEntitySolidShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_CUTOUT_SHADER", GameRenderer::getRenderTypeEntityCutoutShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_CUTOUT_NO_NULL_SHADER", GameRenderer::getRenderTypeEntityCutoutNoNullShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_CUTOUT_NO_NULL_Z_OFFSET_SHADER", GameRenderer::getRenderTypeEntityCutoutNoNullZOffsetShader);
        vanillaShaderMap.put("RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER", GameRenderer::getRenderTypeItemEntityTranslucentCullShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER", GameRenderer::getRenderTypeEntityTranslucentCullShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_TRANSLUCENT_SHADER", GameRenderer::getRenderTypeEntityTranslucentShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER", GameRenderer::getRenderTypeEntitySmoothCutoutShader);
        vanillaShaderMap.put("RENDERTYPE_BEACON_BEAM_SHADER", GameRenderer::getRenderTypeBeaconBeamShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_DECAL_SHADER", GameRenderer::getRenderTypeEntityDecalShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_NO_OUTLINE_SHADER", GameRenderer::getRenderTypeEntityNoOutlineShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_SHADOW_SHADER", GameRenderer::getRenderTypeEntityShadowShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_ALPHA_SHADER", GameRenderer::getRenderTypeEntityAlphaShader);
        vanillaShaderMap.put("RENDERTYPE_EYES_SHADER", GameRenderer::getRenderTypeEyesShader);
        vanillaShaderMap.put("RENDERTYPE_ENERGY_SWIRL_SHADER", GameRenderer::getRenderTypeEnergySwirlShader);
        vanillaShaderMap.put("RENDERTYPE_LEASH_SHADER", GameRenderer::getRenderTypeLeashShader);
        vanillaShaderMap.put("RENDERTYPE_WATER_MASK_SHADER", GameRenderer::getRenderTypeWaterMaskShader);
        vanillaShaderMap.put("RENDERTYPE_OUTLINE_SHADER", GameRenderer::getRenderTypeOutlineShader);
        vanillaShaderMap.put("RENDERTYPE_ARMOR_GLINT_SHADER", GameRenderer::getRenderTypeArmorGlintShader);
        vanillaShaderMap.put("RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER", GameRenderer::getRenderTypeArmorEntityGlintShader);
        vanillaShaderMap.put("RENDERTYPE_GLINT_TRANSLUCENT_SHADER", GameRenderer::getRenderTypeGlintTranslucentShader);
        vanillaShaderMap.put("RENDERTYPE_GLINT_SHADER", GameRenderer::getRenderTypeGlintShader);
        vanillaShaderMap.put("RENDERTYPE_GLINT_DIRECT_SHADER", GameRenderer::getRenderTypeGlintDirectShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_GLINT_SHADER", GameRenderer::getRenderTypeEntityGlintShader);
        vanillaShaderMap.put("RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER", GameRenderer::getRenderTypeEntityGlintDirectShader);
        vanillaShaderMap.put("RENDERTYPE_TEXT_SHADER", GameRenderer::getRenderTypeTextShader);
        vanillaShaderMap.put("RENDERTYPE_TEXT_INTENSITY_SHADER", GameRenderer::getRenderTypeTextIntensityShader);
        vanillaShaderMap.put("RENDERTYPE_TEXT_SEE_THROUGH_SHADER", GameRenderer::getRenderTypeTextSeeThroughShader);
        vanillaShaderMap.put("RENDERTYPE_LIGHTNING_SHADER", GameRenderer::getRenderTypeLightningShader);
        vanillaShaderMap.put("RENDERTYPE_TRIPWIRE_SHADER", GameRenderer::getRenderTypeTripwireShader);
        vanillaShaderMap.put("RENDERTYPE_END_PORTAL_SHADER", GameRenderer::getRenderTypeEndPortalShader);
        vanillaShaderMap.put("RENDERTYPE_END_GATEWAY_SHADER", GameRenderer::getRenderTypeEndGatewayShader);
        vanillaShaderMap.put("RENDERTYPE_LINES_SHADER", GameRenderer::getRenderTypeLinesShader);
        vanillaShaderMap.put("RENDERTYPE_CRUMBLING_SHADER", GameRenderer::getRenderTypeCrumblingShader);

        //Depth tests
        depthTestsMap = new HashMap<>();
        depthTestsMap.put("LEQUAL_DEPTH_TEST", 515);
        depthTestsMap.put("EQUAL_DEPTH_TEST", 514);
        depthTestsMap.put("ALWAYS_DEPTH_TEST", 519);

        //Write mask states
        writeMaskStatesMap = new HashMap<>();
        writeMaskStatesMap.put("ALL_MASK", new Pair<>(true, true));
        writeMaskStatesMap.put("COLOR_MASK", new Pair<>(true, false));
        writeMaskStatesMap.put("DEPTH_MASK", new Pair<>(false, true));

        //Layering modes
        layeringModesMap = new HashMap<>();
        layeringModesMap.put("NO_LAYERING", new Pair<>(()->{},()->{})); //Do nothing.
        layeringModesMap.put("POLYGON_OFFSET_LAYERING", new Pair<>(() -> {
            RenderSystem.polygonOffset(-1.0F, -10.0F);
            RenderSystem.enablePolygonOffset();
        }, () -> {
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
        }));
        layeringModesMap.put("VIEW_OFFSET_Z_LAYERING", new Pair<>(() -> {
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.push();
            matrixStack.scale(0.99975586F, 0.99975586F, 0.99975586F);
            RenderSystem.applyModelViewMatrix();
        }, () -> {
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();
        }));

        //Targets
        targetsMap = new HashMap<>(); //The boolean refers to whether this requires fabulous graphics.
        targetsMap.put("OUTLINE_TARGET", new Pair<>(false, () -> MinecraftClient.getInstance().worldRenderer.getEntityOutlinesFramebuffer()));
        targetsMap.put("TRANSLUCENT_TARGET", new Pair<>(true, () -> MinecraftClient.getInstance().worldRenderer.getTranslucentFramebuffer()));
        targetsMap.put("PARTICLES_TARGET", new Pair<>(true, () -> MinecraftClient.getInstance().worldRenderer.getParticlesFramebuffer()));
        targetsMap.put("WEATHER_TARGET", new Pair<>(true, () -> MinecraftClient.getInstance().worldRenderer.getWeatherFramebuffer()));
        targetsMap.put("CLOUDS_TARGET", new Pair<>(true, () -> MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer()));
        targetsMap.put("ITEM_TARGET", new Pair<>(true, () -> MinecraftClient.getInstance().worldRenderer.getEntityFramebuffer()));

        //Transparency modes
        transparencyModesMap = new HashMap<>();
        transparencyModesMap.put("NO_TRANSPARENCY", ()->{
            RenderSystem.disableBlend();
        });
        transparencyModesMap.put("ADDITIVE_TRANSPARENCY", ()->{
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        });
        transparencyModesMap.put("LIGHTNING_TRANSPARENCY", ()->{
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        });
        transparencyModesMap.put("GLINT_TRANSPARENCY", ()->{
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_COLOR, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        });
        transparencyModesMap.put("CRUMBLING_TRANSPARENCY", ()->{
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.DST_COLOR, GlStateManager.DstFactor.SRC_COLOR, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        });
        transparencyModesMap.put("TRANSLUCENT_TRANSPARENCY", ()->{
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        });

        //Texturing modes
        texturingModesMap = new HashMap<>();
        texturingModesMap.put("DEFAULT_TEXTURING", () -> {}); //Do nothing
        texturingModesMap.put("GLINT_TEXTURING", () -> RenderPhaseInvokerMixin.setupGlintTexturing(8.0F));
        texturingModesMap.put("ENTITY_GLINT_TEXTURING", () -> RenderPhaseInvokerMixin.setupGlintTexturing(0.16F));

    }
}
