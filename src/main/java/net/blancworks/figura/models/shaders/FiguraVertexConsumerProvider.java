package net.blancworks.figura.models.shaders;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.mixin.RenderPhaseInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.Program;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FiguraVertexConsumerProvider extends VertexConsumerProvider.Immediate {

    private static final ArrayList<String> defaultTextures;
    private final Map<String, FiguraRenderLayer> stringLayerMap = new HashMap<>();

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
     * @param rootPath The path to the folder containing model.bbmodel
     */

    public static void parse(PlayerData playerData, InputStream inputStream, Path rootPath) {
        try {
            Map<RenderLayer, BufferBuilder> layerBufferBuilderMap = new HashMap<>();

            playerData.customVCP = new FiguraVertexConsumerProvider(new BufferBuilder(256), layerBufferBuilderMap);

//            Reader reader = Files.newBufferedReader(renderLayersFile.toPath());
//            Map jsonData = FiguraMod.GSON.fromJson(reader, Map.class);

            Reader reader = new InputStreamReader(inputStream);
            Map jsonData = FiguraMod.GSON.fromJson(reader, Map.class);

            reader.close();

            //Get arraylist of renderLayers, iterate over them all
            ArrayList<Map> renderLayers = (ArrayList<Map>) jsonData.get("render_layers");
            for (Map layer : renderLayers) {
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

                //Create the shader
                CompletableFuture<Shader> shader = new CompletableFuture<>();
                boolean isVanillaShader = vanillaShaderMap.containsKey(shaderStr);
                if (isVanillaShader) {
                    //The chosen shader is a vanilla shader, so get it from this map.
                    shader.complete(vanillaShaderMap.get(shaderStr).get());
                } else {
                    //This shader is not a vanilla shader, so create a new one.
                    FiguraShaderResourceFactory shaderFactory = new FiguraShaderResourceFactory(rootPath);
                    RenderSystem.recordRenderCall(() -> {
                        try {
                            //Remove shader from the cache, so it can be changed
                            //Without this, problems arise when:
                            //Saving the file, since the cached version remains
                            //If you load two avatars that have a shader with the same name, the two will use
                            //The same shader.
                            //These following two lines are to prevent this behavior.
                            Program.Type.VERTEX.getProgramCache().put(shaderStr, null);
                            Program.Type.FRAGMENT.getProgramCache().put(shaderStr, null);
                            Shader customShader = new FiguraShader(shaderFactory, shaderStr, vertexFormat);
                            shader.complete(customShader);
                        } catch (IOException e) {
                            e.printStackTrace();
                            shader.complete(vanillaShaderMap.get("RENDERTYPE_ENTITY_TRANSLUCENT_SHADER").get());
                        }
                    });
                }

                //preDraw: setup all the render system environment variables for this layer.
                Runnable preDraw = () -> {
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

                //post draw: Reset environment variables back to defaults.
                Runnable postDraw = () -> {

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

                //Now we have everything we need to actually construct the RenderLayer.
                FiguraRenderLayer renderLayer;
                if (!isVanillaShader) //If not a vanilla shader, store the custom shader in the render layer
                    renderLayer = new FiguraRenderLayer(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, preDraw, postDraw, shader);
                else
                    renderLayer = new FiguraRenderLayer(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, preDraw, postDraw, null);

                BufferBuilder bufferBuilder = new BufferBuilder(renderLayer.getExpectedBufferSize());

                //Put the render layer into the map
                layerBufferBuilderMap.put(renderLayer, bufferBuilder);
                //Keep a reference to the render layer by name
                playerData.customVCP.stringLayerMap.put(name, renderLayer);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

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
        texturingModesMap.put("GLINT_TEXTURING", () -> RenderPhaseInvoker.setupGlintTexturing(8.0F));
        texturingModesMap.put("ENTITY_GLINT_TEXTURING", () -> RenderPhaseInvoker.setupGlintTexturing(0.16F));

    }
}
