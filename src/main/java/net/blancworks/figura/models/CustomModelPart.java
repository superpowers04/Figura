package net.blancworks.figura.models;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.lua.api.model.*;
import net.blancworks.figura.models.tasks.RenderTask;
import net.blancworks.figura.utils.MathUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomModelPart {
    public String name = "NULL";

    //Transform data
    public Vector3f pivot = new Vector3f(0f, 0f, 0f);
    public Vector3f pos = new Vector3f(0f, 0f, 0f);
    public Vector3f rot = new Vector3f(0f, 0f, 0f);
    public Vector3f scale = new Vector3f(1f, 1f, 1f);
    public Vector3f color = new Vector3f(1f, 1f, 1f);

    //uv stuff
    public Map<UV, uvData> UVCustomizations = new HashMap<>();
    public Vec2f texSize;
    public Vec2f uvOffset = new Vec2f(0f, 0f);

    //model properties
    public boolean visible = true;

    public ParentType parentType = ParentType.None;
    public boolean isMimicMode = false;

    public RotationType rotationType = RotationType.BlockBench;

    public ArrayList<CustomModelPart> children = new ArrayList<>();

    public ShaderType shaderType = ShaderType.None;
    private static final Map<Integer, Vector3f> END_SHADER_COLORS = new HashMap<Integer, Vector3f>(){{
        final Random RANDOM = new Random(31100L);
        //first layer
        float r = (RANDOM.nextFloat() * 0.5F + 0.1F) * 0.15F;
        float g = (RANDOM.nextFloat() * 0.5F + 0.4F) * 0.15F;
        float b = (RANDOM.nextFloat() * 0.5F + 0.5F) * 0.15F;
        put(0, new Vector3f(r, g, b));
        //render other layers
        for (int i = 2; i < 17; ++i) {
            float tempColor = 2.0F / (float) (18 - i);
            r = (RANDOM.nextFloat() * 0.5F + 0.1F) * tempColor;
            g = (RANDOM.nextFloat() * 0.5F + 0.4F) * tempColor;
            b = (RANDOM.nextFloat() * 0.5F + 0.5F) * tempColor;
            put(i, new Vector3f(r, g, b));
        }
    }};

    public TextureType textureType = TextureType.Custom;
    public Identifier textureVanilla = FiguraTexture.DEFAULT_ID;

    public boolean extraTex = true;
    public boolean cull = false;

    public float alpha = 1f;

    //All the vertex data is stored here! :D
    public FloatList vertexData = new FloatArrayList();
    public int vertexCount = 0;

    public Matrix4f lastModelMatrix = new Matrix4f();
    public Matrix3f lastNormalMatrix = new Matrix3f();

    public Matrix4f lastModelMatrixInverse = new Matrix4f();
    public Matrix3f lastNormalMatrixInverse = new Matrix3f();

    //Extra special rendering for this part
    public final ArrayList<RenderTask> renderTasks = new ArrayList<>();

    public static boolean canRenderHitBox = false;

    //Renders a model part (and all sub-parts) using the textures provided by a PlayerData instance.
    public int render(PlayerData data, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        //no model to render
        if (data.model == null || data.playerListEntry == null || vcp == null || !data.isAvatarLoaded())
            return 0;

        //lets render boys!!
        boolean applyHiddenTransforms = data.model.applyHiddenTransforms;
        ParentType renderOnly = data.model.renderOnly;
        int ret = data.model.leftToRender;
        data.model.renderOnly = null;

        //main texture
        Function<Identifier, RenderLayer> layerFunction = RenderLayer::getEntityTranslucent;
        ret = renderTextures(ret, matrices, transformStack, vcp, null, light, overlay, 0, 0, new Vector3f(1f, 1f, 1f), alpha, false, getTexture(), layerFunction, false, applyHiddenTransforms, renderOnly);

        //extra textures
        for (FiguraTexture figuraTexture : FiguraMod.currentData.extraTextures) {
            Function<Identifier, RenderLayer> renderLayerGetter = FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.get(figuraTexture.type);

            if (renderLayerGetter != null) {
                renderTextures(ret, matrices, transformStack, vcp, null, light, overlay, 0,0, new Vector3f(1f, 1f, 1f), alpha, false, figuraTexture.id, renderLayerGetter, true, applyHiddenTransforms, renderOnly);
            }
        }

        draw(vcp);

        //shaders
        ret = renderShaders(ret, matrices, vcp, light, overlay, 0, 0, new Vector3f(1f, 1f, 1f), alpha, false, (byte) 0, applyHiddenTransforms, renderOnly);
        draw(vcp);

        //extra stuff and hitboxes
        ret = renderExtraParts(ret, matrices, vcp, light, false, applyHiddenTransforms, renderOnly);
        draw(vcp);

        return ret;
    }

    //Renders this custom model part and all its children.
    //Returns the cuboids left to render after this one, and only renders until leftToRender is zero.
    public int renderTextures(int leftToRender, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, RenderLayer layer, int light, int overlay, float u, float v, Vector3f prevColor, float alpha, boolean canRender, Identifier texture, Function<Identifier, RenderLayer> layerFunction, boolean isExtraTex, boolean applyHiddenTransforms, ParentType renderOnly) {
        //do not render invisible parts
        if (!this.visible || (isExtraTex && !this.extraTex))
            return leftToRender;

        matrices.push();
        transformStack.push();

        if (applyHiddenTransforms) {
            applyVanillaTransforms(matrices, transformStack);

            applyTransforms(matrices);
            applyTransforms(transformStack);

            updateModelMatrices(transformStack);
        } else if (canRender) {
            applyTransforms(matrices);
            applyTransforms(transformStack);
        }

        if (renderOnly == null || this.parentType == renderOnly)
            canRender = true;

        //uv -> color -> alpha -> cull
        u += this.uvOffset.x;
        v += this.uvOffset.y;

        Vector3f color = this.color.copy();
        color.multiplyComponentwise(prevColor.getX(), prevColor.getY(), prevColor.getZ());

        alpha = this.alpha * alpha;

        if (!isExtraTex && this.cull)
            layerFunction = RenderLayer::getEntityTranslucentCull;

        //texture
        if (this.textureType != TextureType.Custom)
            texture = getTexture();

        //render!
        if (canRender) {
            //get vertex consumer
            VertexConsumer consumer = vcp.getBuffer(layerFunction.apply(texture));

            //render
            leftToRender = renderCube(leftToRender, matrices, consumer, light, overlay, u, v, color, alpha);
        }

        for (CustomModelPart child : this.children) {
            if (leftToRender <= 0)
                break;

            //Don't render special parts.
            if (child.isSpecial())
                continue;

            //render part
            leftToRender = child.renderTextures(leftToRender, matrices, transformStack, vcp, layer, light, overlay, u, v, color, alpha, canRender, texture, layerFunction, isExtraTex, applyHiddenTransforms, renderOnly);
        }

        matrices.pop();
        transformStack.pop();

        return leftToRender;
    }

    public int renderShaders(int leftToRender, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay, float u, float v, Vector3f prevColor, float alpha, boolean canRender, byte shadersToRender, boolean applyHiddenTransforms, ParentType renderOnly) {
        //do not render invisible parts
        if (!this.visible)
            return leftToRender;

        matrices.push();

        if (applyHiddenTransforms) {
            applyVanillaTransforms(matrices, new MatrixStack());
            applyTransforms(matrices);
        } else if (canRender) {
            applyTransforms(matrices);
        }

        if (renderOnly == null || this.parentType == renderOnly)
            canRender = true;

        //uv -> color -> alpha -> shaders
        u += this.uvOffset.x;
        v += this.uvOffset.y;

        Vector3f color = this.color.copy();
        color.multiplyComponentwise(prevColor.getX(), prevColor.getY(), prevColor.getZ());

        alpha = this.alpha * alpha;

        byte shaders = shadersToRender;
        if (this.shaderType != ShaderType.None)
            shaders = (byte) (shaders | this.shaderType.id);

        //render!
        if (canRender) {
            if (ShaderType.EndPortal.isShader(shaders)) {
                for (int i = 0; i < 18; i++) {
                    Vector3f rgb = END_SHADER_COLORS.get(i);
                    if (rgb == null) continue;
                    leftToRender = renderCube(leftToRender, matrices, vcp.getBuffer(RenderLayer.getEndPortal(i)), light, overlay, u, v, rgb, alpha);
                }
            }
            if (ShaderType.Glint.isShader(shaders))
                leftToRender = renderCube(leftToRender, matrices, vcp.getBuffer(RenderLayer.getDirectEntityGlint()), light, overlay, u, v, color, alpha);
        }

        for (CustomModelPart child : this.children) {
            if (leftToRender <= 0)
                break;

            //Don't render special parts.
            if (child.isSpecial())
                continue;

            //render part
            leftToRender = child.renderShaders(leftToRender, matrices, vcp, light, overlay, u, v, color, alpha, canRender, shaders, applyHiddenTransforms, renderOnly);
        }

        matrices.pop();

        return leftToRender;
    }

    public int renderExtraParts(int leftToRender, MatrixStack matrices, VertexConsumerProvider vcp, int light, boolean canRender, boolean applyHiddenTransforms, ParentType renderOnly) {
        //do not render invisible parts
        if (!this.visible)
            return leftToRender;

        matrices.push();

        if (applyHiddenTransforms) {
            applyVanillaTransforms(matrices, new MatrixStack());
            applyTransforms(matrices);
        } else if (canRender) {
            applyTransforms(matrices);
        }

        if (renderOnly == null || this.parentType == renderOnly)
            canRender = true;

        //render!
        if (canRender)
            leftToRender = renderExtras(leftToRender, matrices, vcp, light);

        for (CustomModelPart child : this.children) {
            if (leftToRender <= 0)
                break;

            //Don't render special parts.
            if (child.isSpecial())
                continue;

            //render part
            leftToRender = child.renderExtraParts(leftToRender, matrices, vcp, light, canRender, applyHiddenTransforms, renderOnly);
        }

        matrices.pop();

        return leftToRender;
    }

    public void draw(VertexConsumerProvider vcp) {
        if (vcp instanceof VertexConsumerProvider.Immediate) ((VertexConsumerProvider.Immediate) vcp).draw();
        else if (vcp instanceof OutlineVertexConsumerProvider) ((OutlineVertexConsumerProvider) vcp).draw();
    }

    public Identifier getTexture() {
        PlayerData data = FiguraMod.currentData;

        Identifier textureId;
        if (data.texture == null || this.textureType != TextureType.Custom) {
            switch (this.textureType) {
                case Cape:
                    textureId = data.playerListEntry.getCapeTexture();
                    if (textureId == null)
                        textureId = FiguraTexture.DEFAULT_ID;
                    break;
                case Elytra:
                    textureId = data.playerListEntry.getElytraTexture();
                    if (textureId == null)
                        textureId = new Identifier("minecraft", "textures/entity/elytra.png");
                    break;
                case Resource:
                    textureId = MinecraftClient.getInstance().getResourceManager().containsResource(textureVanilla) ? textureVanilla : MissingSprite.getMissingSpriteId();
                    break;
                default:
                    textureId = data.playerListEntry.getSkinTexture();
                    break;
            }
        } else {
            textureId = data.texture.id;
        }

        return textureId;
    }

    public int renderCube(int leftToRender, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float u, float v, Vector3f color, float alpha) {
        Matrix4f modelMatrix = matrices.peek().getModel();
        Matrix3f normalMatrix = matrices.peek().getNormal();

        for (int i = 1; i <= this.vertexCount; i++) {
            int startIndex = (i - 1) * 8;

            //Get vertex.
            Vector4f fullVert = new Vector4f(
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex++),
                    1
            );

            float vertU = this.vertexData.getFloat(startIndex++);
            float vertV = this.vertexData.getFloat(startIndex++);

            Vector3f normal = new Vector3f(
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex)
            );

            fullVert.transform(modelMatrix);
            normal.transform(normalMatrix);

            //Push vertex.
            vertices.vertex(
                    fullVert.getX(), fullVert.getY(), fullVert.getZ(),
                    color.getX(), color.getY(), color.getZ(), alpha,
                    vertU + u, vertV + v,
                    overlay, light,
                    normal.getX(), normal.getY(), normal.getZ()
            );

            //Every 4 verts (1 face)
            if (i % 4 == 0) {
                leftToRender -= 4;

                if (leftToRender <= 0)
                    break;
            }
        }

        return leftToRender;
    }

    public int renderExtras(int leftToRender, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        //Render extra parts
        synchronized (this.renderTasks) {
            for (RenderTask task : this.renderTasks) {
                leftToRender -= task.render(matrices, vcp, light);
                if (leftToRender <= 0) break;
            }
        }

        //Render hit box
        if (canRenderHitBox) renderHitBox(matrices, vcp.getBuffer(RenderLayer.LINES));

        return leftToRender;
    }

    public void renderHitBox(MatrixStack matrices, VertexConsumer vertices) {
        Vector3f color;
        float boxSize;
        if (this.getPartType() == PartType.CUBE) {
            color = new Vector3f(1f, 0.45f, 0.72f); //0xff72b7 aka fran_pink
            boxSize = 1 / 48f;
        }
        else {
            color = new Vector3f(0.69f, 0.95f, 1f); //0xaff2ff aka ace_blue
            boxSize = 1 / 24f;
        }

        //render the box
        WorldRenderer.drawBox(matrices, vertices, -boxSize, -boxSize, -boxSize, boxSize, boxSize, boxSize, color.getX(), color.getY(), color.getZ(), 1f);
    }

    //clear all extra render tasks
    public static void clearExtraRendering(CustomModelPart part) {
        part.renderTasks.clear();
        part.children.forEach(CustomModelPart::clearExtraRendering);
    }

    public int getComplexity() {
        //don't render invisible parts
        if (!this.visible)
            return 0;

        int complexity = this.vertexCount;

        //iterate over children
        for (CustomModelPart child : this.children) {
            complexity += child.getComplexity();
        }

        return complexity;
    }

    public void updateModelMatrices(MatrixStack stack) {
        lastModelMatrix = stack.peek().getModel().copy();
        lastNormalMatrix = stack.peek().getNormal().copy();

        lastModelMatrixInverse = lastModelMatrix.copy();
        lastModelMatrixInverse.invert();
        lastNormalMatrixInverse = lastNormalMatrix.copy();
        lastNormalMatrixInverse.invert();
    }

    public void applyVanillaTransforms(MatrixStack matrices, MatrixStack transformStack) {
        if (parentType != ParentType.None && parentType != ParentType.Model) {
            try {
                PlayerEntityModel<?> model = FiguraMod.currentData.vanillaModel;

                //mimic rotations
                if (this.isMimicMode) {
                    switch (this.parentType) {
                        case Head:
                            this.rot = new Vector3f(model.head.pitch, model.head.yaw, model.head.roll);
                            break;
                        case Torso:
                            this.rot = new Vector3f(model.torso.pitch, model.torso.yaw, model.torso.roll);
                            break;
                        case LeftArm:
                            this.rot = new Vector3f(model.leftArm.pitch, model.leftArm.yaw, model.leftArm.roll);
                            break;
                        case LeftLeg:
                            this.rot = new Vector3f(model.leftLeg.pitch, model.leftLeg.yaw, model.leftLeg.roll);
                            break;
                        case RightArm:
                            this.rot = new Vector3f(model.rightArm.pitch, model.rightArm.yaw, model.rightArm.roll);
                            break;
                        case RightLeg:
                            this.rot = new Vector3f(model.rightLeg.pitch, model.rightLeg.yaw, model.rightLeg.roll);
                            break;
                    }

                    float multiply = 57.2958f;
                    this.rot.multiplyComponentwise(multiply, multiply, multiply);
                }
                //custom rotations
                else {
                    switch (this.parentType) {
                        case Head:
                            model.head.rotate(matrices);
                            model.head.rotate(transformStack);
                            break;
                        case Torso:
                            model.torso.rotate(matrices);
                            model.torso.rotate(transformStack);
                            break;
                        case LeftArm:
                            model.leftArm.rotate(matrices);
                            model.leftArm.rotate(transformStack);
                            break;
                        case LeftLeg:
                            model.leftLeg.rotate(matrices);
                            model.leftLeg.rotate(transformStack);
                            break;
                        case RightArm:
                            model.rightArm.rotate(matrices);
                            model.rightArm.rotate(transformStack);
                            break;
                        case RightLeg:
                            model.rightLeg.rotate(matrices);
                            model.rightLeg.rotate(transformStack);
                            break;
                        case LeftItemOrigin:
                            FiguraMod.currentData.model.originModifications.put(ItemModelAPI.VANILLA_LEFT_HAND_ID, new VanillaModelPartCustomization() {{
                                matrices.push();
                                applyTransformsAsItem(matrices);
                                applyTransformsAsItem(transformStack);
                                stackReference = matrices.peek();
                                part = CustomModelPart.this;
                                visible = true;
                                matrices.pop();
                            }});
                            break;
                        case RightItemOrigin:
                            FiguraMod.currentData.model.originModifications.put(ItemModelAPI.VANILLA_RIGHT_HAND_ID, new VanillaModelPartCustomization() {{
                                matrices.push();
                                applyTransformsAsItem(matrices);
                                applyTransformsAsItem(transformStack);
                                stackReference = matrices.peek();
                                part = CustomModelPart.this;
                                visible = true;
                                matrices.pop();
                            }});
                            break;
                        case LeftElytraOrigin:
                            FiguraMod.currentData.model.originModifications.put(ElytraModelAPI.VANILLA_LEFT_WING_ID, new VanillaModelPartCustomization() {{
                                matrices.push();
                                applyTransformsAsElytraOrParrot(matrices);
                                applyTransformsAsElytraOrParrot(transformStack);
                                stackReference = matrices.peek();
                                part = CustomModelPart.this;
                                visible = true;
                                matrices.pop();
                            }});
                            break;
                        case RightElytraOrigin:
                            FiguraMod.currentData.model.originModifications.put(ElytraModelAPI.VANILLA_RIGHT_WING_ID, new VanillaModelPartCustomization() {{
                                matrices.push();
                                applyTransformsAsElytraOrParrot(matrices);
                                applyTransformsAsElytraOrParrot(transformStack);
                                stackReference = matrices.peek();
                                part = CustomModelPart.this;
                                visible = true;
                                matrices.pop();
                            }});
                            break;
                        case LeftParrotOrigin:
                            FiguraMod.currentData.model.originModifications.put(ParrotModelAPI.VANILLA_LEFT_PARROT_ID, new VanillaModelPartCustomization() {{
                                matrices.push();
                                applyTransformsAsElytraOrParrot(matrices);
                                applyTransformsAsElytraOrParrot(transformStack);
                                stackReference = matrices.peek();
                                part = CustomModelPart.this;
                                visible = true;
                                matrices.pop();
                            }});
                            break;
                        case RightParrotOrigin:
                            FiguraMod.currentData.model.originModifications.put(ParrotModelAPI.VANILLA_RIGHT_PARROT_ID, new VanillaModelPartCustomization() {{
                                matrices.push();
                                applyTransformsAsElytraOrParrot(matrices);
                                applyTransformsAsElytraOrParrot(transformStack);
                                stackReference = matrices.peek();
                                part = CustomModelPart.this;
                                visible = true;
                                matrices.pop();
                            }});
                            break;
                        case LeftSpyglass:
                            FiguraMod.currentData.model.originModifications.put(SpyglassModelAPI.VANILLA_LEFT_SPYGLASS_ID, new VanillaModelPartCustomization() {{
                                matrices.push();
                                applyTransformsAsSpyglass(matrices);
                                applyTransformsAsSpyglass(transformStack);
                                stackReference = matrices.peek();
                                part = CustomModelPart.this;
                                visible = true;
                                matrices.pop();
                            }});
                            break;
                        case RightSpyglass:
                            FiguraMod.currentData.model.originModifications.put(SpyglassModelAPI.VANILLA_RIGHT_SPYGLASS_ID, new VanillaModelPartCustomization() {{
                                matrices.push();
                                applyTransformsAsSpyglass(matrices);
                                applyTransformsAsSpyglass(transformStack);
                                stackReference = matrices.peek();
                                part = CustomModelPart.this;
                                visible = true;
                                matrices.pop();
                            }});
                            break;
                        case Camera:
                            Quaternion rot = MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation().copy();
                            Vector3f euler = MathUtils.quaternionToEulerXYZ(rot);
                            matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(euler.getZ()));
                            matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-euler.getY()));
                            matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-euler.getX()));
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void applyTransforms(MatrixStack stack) {
        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);

        stack.translate(-this.pivot.getX() / 16.0f, -this.pivot.getY() / 16.0f, -this.pivot.getZ() / 16.0f);

        if (this.isMimicMode || this.rotationType == RotationType.Vanilla)
            vanillaRotate(stack, this.rot);
        else if (this.rotationType == RotationType.BlockBench)
            rotate(stack, this.rot);

        stack.scale(this.scale.getX(), this.scale.getY(), this.scale.getZ());

        stack.translate(this.pivot.getX() / 16.0f, this.pivot.getY() / 16.0f, this.pivot.getZ() / 16.0f);
    }

    //TODO move these to the mixins, probably.
    public void applyTransformsAsItem(MatrixStack stack) {
        stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-90.0F));
        stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180.0F));
        //stack.translate(0, 0.125D, -0.625D);
        stack.translate(pivot.getX() / 16.0f, pivot.getZ() / 16.0f, pivot.getY() / 16.0f);
        rotate(stack, this.rot);
        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);
    }

    //TODO move these to the mixins, probably.
    public void applyTransformsAsElytraOrParrot(MatrixStack stack) {
        stack.translate(pivot.getX() / 16.0f, pivot.getY() / 16.0f, -pivot.getZ() / 16.0f);
        rotate(stack, this.rot);
        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);
    }

    //TODO move these to the mixins, probably. //OK GOT IT
    public void applyTransformsAsSpyglass(MatrixStack stack) {
        stack.translate(-pivot.getX() / 16.0f, -pivot.getY() / 16.0f, -pivot.getZ() / 16.0f);
        rotate(stack, this.rot);
        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);
    }

    //Re-builds the mesh data for a custom model part.
    public void rebuild() {}

    public void rotate(MatrixStack stack, Vector3f rot) {
        stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
        stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-rot.getY()));
        stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-rot.getX()));
    }

    public void vanillaRotate(MatrixStack stack, Vector3f rot) {
        stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
        stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(rot.getY()));
        stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(rot.getX()));
    }

    public void addVertex(Vector3f vert, float u, float v, Vector3f normal, FloatList vertexData) {
        vertexData.add(vert.getX() / 16.0f);
        vertexData.add(vert.getY() / 16.0f);
        vertexData.add(vert.getZ() / 16.0f);
        vertexData.add(u);
        vertexData.add(v);
        vertexData.add(-normal.getX());
        vertexData.add(-normal.getY());
        vertexData.add(-normal.getZ());
    }

    public void readNbt(CompoundTag partNbt) {
        //Name
        if (partNbt.contains("nm"))
            this.name = partNbt.get("nm").asString();
        else
            this.name = "NULL";

        if (partNbt.contains("pos")) {
            ListTag list = (ListTag) partNbt.get("pos");
            this.pos = vec3fFromNbt(list);
        }
        if (partNbt.contains("rot")) {
            ListTag list = (ListTag) partNbt.get("rot");
            this.rot = vec3fFromNbt(list);
        }
        if (partNbt.contains("scl")) {
            ListTag list = (ListTag) partNbt.get("scl");
            this.scale = vec3fFromNbt(list);
        }
        if (partNbt.contains("piv")) {
            ListTag list = (ListTag) partNbt.get("piv");
            this.pivot = vec3fFromNbt(list);
        }

        if (partNbt.contains("ptype")) {
            try {
                this.parentType = ParentType.valueOf(partNbt.get("ptype").asString());
            } catch (Exception ignored) {
                this.parentType = ParentType.Model;
            }
        }
        if (partNbt.contains("mmc")) {
            this.isMimicMode = ((ByteTag) partNbt.get("mmc")).getByte() == 1;
        }

        //compat with older versions
        if (partNbt.contains("vsb")) {
            this.visible = partNbt.getBoolean("vsb");
        }

        if (partNbt.contains("chld")) {
            ListTag childrenNbt = (ListTag) partNbt.get("chld");
            if (childrenNbt == null || childrenNbt.getElementType() != NbtType.COMPOUND)
                return;

            for (Tag child : childrenNbt) {
                CompoundTag childNbt = (CompoundTag) child;
                CustomModelPart part = fromNbt(childNbt);
                part.rebuild();
                this.children.add(part);
            }
        }
    }

    public void writeNbt(CompoundTag partNbt) {
        if (!name.equals("NULL")) {
            partNbt.put("nm", StringTag.of(name));
        }
        if (!this.pos.equals(new Vector3f(0f, 0f, 0f))) {
            partNbt.put("pos", vec3fToNbt(this.pos));
        }
        if (!this.rot.equals(new Vector3f(0f, 0f, 0f))) {
            partNbt.put("rot", vec3fToNbt(this.rot));
        }
        if (!this.pivot.equals(new Vector3f(0f, 0f, 0f))) {
            partNbt.put("piv", vec3fToNbt(this.pivot));
        }
        if (!this.scale.equals(new Vector3f(1, 1, 1))) {
            partNbt.put("scl", vec3fToNbt(this.scale));
        }
        if (this.parentType != ParentType.None) {
            partNbt.put("ptype", StringTag.of(this.parentType.toString()));
        }
        if (this.isMimicMode) {
            partNbt.put("mmc", ByteTag.of(true));
        }
        if (this.getPartType() != PartType.GROUP) {
            partNbt.put("pt", StringTag.of(this.getPartType().val));
        }

        //Parse children.
        if (this.children.size() > 0) {
            ListTag childrenList = new ListTag();

            for (CustomModelPart child : this.children) {
                CompoundTag childNbt = new CompoundTag();
                writeToNbt(childNbt, child);
                childrenList.add(childNbt);
            }

            partNbt.put("chld", childrenList);
        }
    }

    public PartType getPartType() {
        //Default part type is group
        return PartType.GROUP;
    }

    public void applyTrueOffset(Vector3f offset) {}

    public enum ParentType {
        None,
        Model,
        Head,
        LeftArm,
        RightArm,
        LeftLeg,
        RightLeg,
        Torso,
        WORLD(true),
        LeftItemOrigin, //Origin position of the held item in the left hand
        RightItemOrigin, //Origin position of the held item
        LeftElytraOrigin, //Left origin position of the elytra
        RightElytraOrigin, //Right origin position of the elytra
        LeftParrotOrigin, //Left origin position of the shoulder parrot
        RightParrotOrigin, //Right origin position of the shoulder parrot
        LeftElytra(true), //Left position of the elytra model
        RightElytra(true), //Right position of the elytra model
        LeftSpyglass(true), //Left position of the spyglass model
        RightSpyglass(true), //Right position of the spyglass model
        Camera, //paparazzi
        Skull(true); // A replacement for the "Head" type, but only rendered in the tab list and player head item/blocks

        private final boolean special;
        ParentType(boolean special) {
            this.special = special;
        }
        ParentType() {
            this.special = false;
        }
    }

    public boolean isSpecial() {
        return this.parentType.special;
    }

    public enum RotationType {
        BlockBench,
        Vanilla
    }

    public enum ShaderType {
        None(0),
        EndPortal(1),
        Glint(2);

        public final int id;
        ShaderType(int id) {
            this.id = id;
        }

        public boolean isShader(int shader) {
            return (id & shader) == id;
        }
    }

    public enum TextureType {
        Custom,
        Skin,
        Cape,
        Elytra,
        Resource
    }

    public enum UV {
        ALL,
        NORTH,
        SOUTH,
        WEST,
        EAST,
        UP,
        DOWN
    }

    public enum PartType {
        GROUP("na"),
        CUBE("cub"),
        MESH("msh");

        public final String val;
        PartType(String value) {
            this.val = value;
        }
    }

    public static class uvData {
        public Vec2f uvOffset, uvSize;

        public void setUVOffset(Vec2f uvOffset) {
            this.uvOffset = uvOffset;
        }

        public void setUVSize(Vec2f uvSize) {
            this.uvSize = uvSize;
        }
    }

    public void applyUVMods(LuaVector v) {
        if (v != null) texSize = new Vec2f(v.x(), v.y());
        rebuild();

        children.forEach(child -> {
            child.UVCustomizations = UVCustomizations;
            child.applyUVMods(v);
        });
    }

    //---------MODEL PART TYPES---------

    public static final Map<String, Supplier<CustomModelPart>> MODEL_PART_TYPES =
            new ImmutableMap.Builder<String, Supplier<CustomModelPart>>()
                    .put("na", CustomModelPart::new)
                    .put("cub", CustomModelPartCuboid::new)
                    .put("msh", CustomModelPartMesh::new)
                    .build();

    /**
     * Gets a CustomModelPart from NBT, automatically reading the type from that NBT.
     */
    public static CustomModelPart fromNbt(CompoundTag nbt) {
        Tag pt = nbt.get("pt");
        String partType = pt == null ? "na" : pt.asString();

        if (!MODEL_PART_TYPES.containsKey(partType))
            return null;

        Supplier<CustomModelPart> sup = MODEL_PART_TYPES.get(partType);
        CustomModelPart part = sup.get();

        part.readNbt(nbt);
        return part;
    }

    /**
     * Writes a model part to an NBT compound.
     *
     * @param nbt  the NBT compound
     * @param part the model part
     */
    public static void writeToNbt(CompoundTag nbt, CustomModelPart part) {
        String partType = part.getPartType().val;
        if (!MODEL_PART_TYPES.containsKey(partType))
            return;

        part.writeNbt(nbt);
    }

    public static Vector3f vec3fFromNbt(@Nullable ListTag nbt) {
        if (nbt == null || nbt.getElementType() != NbtType.FLOAT)
            return new Vector3f(0f, 0f, 0f);
        return new Vector3f(nbt.getFloat(0), nbt.getFloat(1), nbt.getFloat(2));
    }

    public static ListTag vec3fToNbt(Vector3f vec) {
        ListTag nbt = new ListTag();
        nbt.add(FloatTag.of(vec.getX()));
        nbt.add(FloatTag.of(vec.getY()));
        nbt.add(FloatTag.of(vec.getZ()));
        return nbt;
    }

    public static Vector4f v4fFromNbtList(ListTag list) {
        return new Vector4f(list.getFloat(0), list.getFloat(1), list.getFloat(2), list.getFloat(3));
    }

    public static Vec2f v2fFromNbtList(ListTag list) {
        return new Vec2f(list.getFloat(0), list.getFloat(1));
    }
}
