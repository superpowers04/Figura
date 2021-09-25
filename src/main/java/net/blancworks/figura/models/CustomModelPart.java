package net.blancworks.figura.models;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.lua.api.model.*;
import net.blancworks.figura.lua.api.renderer.RenderTask;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomModelPart {
    public String name = "NULL";

    //Transform data
    public Vec3f pivot = new Vec3f();
    public Vec3f pos = new Vec3f();
    public Vec3f rot = new Vec3f();
    public Vec3f scale = new Vec3f(1, 1, 1);
    public Vec3f color = new Vec3f(1, 1, 1);

    //Offsets
    public float uOffset = 0;
    public float vOffset = 0;

    public boolean visible = true;
    public boolean isHidden = false;

    public ParentType parentType = ParentType.None;
    public boolean isMimicMode = false;

    public RotationType rotationType = RotationType.BlockBench;

    public ArrayList<CustomModelPart> children = new ArrayList<>();

    public ShaderType shaderType = ShaderType.None;

    //public RenderType renderType = RenderType.None;

    public float alpha = 1.0f;

    //All the vertex data is stored here! :D
    public FloatList vertexData = new FloatArrayList();
    public int vertexCount = 0;

    public Matrix4f lastModelMatrix = new Matrix4f();
    public Matrix3f lastNormalMatrix = new Matrix3f();

    public Matrix4f lastModelMatrixInverse = new Matrix4f();
    public Matrix3f lastNormalMatrixInverse = new Matrix3f();

    //Extra special rendering for this part
    public ArrayList<RenderTask> renderTasks = new ArrayList<>();

    public static boolean canRenderHitBox;
    public static ParentType renderOnly = null;

    //Renders a model part (and all sub-parts) using the textures provided by a PlayerData instance.
    public int renderUsingAllTextures(PlayerData data, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        if (data.texture.isDone) {
            //hit boxes :3
            canRenderHitBox = (boolean) Config.entries.get("partsHitBox").value && MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes();

            //render!
            int ret = render(data, data.model.leftToRender, matrices, transformStack, vcp, light, overlay, 0, 0, new Vec3f(1, 1, 1), alpha, new HashSet<>(), false);

            //post render
            renderOnly = null;
            return ret;
        }
        return 0;
    }

    //Renders this custom model part and all its children.
    //Returns the cuboids left to render after this one, and only renders until leftToRender is zero.
    public int render(PlayerData data, int leftToRender, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float u, float v, Vec3f prevColor, float alpha, HashSet<ShaderType> shadersToRender, boolean canRenderChild) {
        //do not render invisible parts
        if (!this.visible || this.isHidden)
            return leftToRender;

        matrices.push();
        transformStack.push();

        try {
            PlayerEntityModel<?> model = FiguraMod.currentData.vanillaModel;
            if (this.isMimicMode && parentType != ParentType.None && parentType != ParentType.Model) {
                switch (this.parentType) {
                    case Head -> this.rot = new Vec3f(model.head.pitch, model.head.yaw, model.head.roll);
                    case Torso -> this.rot = new Vec3f(model.body.pitch, model.body.yaw, model.body.roll);
                    case LeftArm -> this.rot = new Vec3f(model.leftArm.pitch, model.leftArm.yaw, model.leftArm.roll);
                    case LeftLeg -> this.rot = new Vec3f(model.leftLeg.pitch, model.leftLeg.yaw, model.leftLeg.roll);
                    case RightArm -> this.rot = new Vec3f(model.rightArm.pitch, model.rightArm.yaw, model.rightArm.roll);
                    case RightLeg -> this.rot = new Vec3f(model.rightLeg.pitch, model.rightLeg.yaw, model.rightLeg.roll);
                }

                float multiply = MathHelper.DEGREES_PER_RADIAN;
                this.rot.multiplyComponentwise(multiply, multiply, multiply);
            }
            else if (parentType != ParentType.Model) {
                switch (parentType) {
                    case Head -> {
                        model.head.rotate(matrices);
                        model.head.rotate(transformStack);
                    }
                    case Torso -> {
                        model.body.rotate(matrices);
                        model.body.rotate(transformStack);
                    }
                    case LeftArm -> {
                        model.leftArm.rotate(matrices);
                        model.leftArm.rotate(transformStack);
                    }
                    case LeftLeg -> {
                        model.leftLeg.rotate(matrices);
                        model.leftLeg.rotate(transformStack);
                    }
                    case RightArm -> {
                        model.rightArm.rotate(matrices);
                        model.rightArm.rotate(transformStack);
                    }
                    case RightLeg -> {
                        model.rightLeg.rotate(matrices);
                        model.rightLeg.rotate(transformStack);
                    }
                    case LeftItemOrigin -> FiguraMod.currentData.model.originModifications.put(ItemModelAPI.VANILLA_LEFT_HAND_ID, new VanillaModelPartCustomization() {{
                        matrices.push();
                        applyTransformsAsItem(matrices);
                        applyTransformsAsItem(transformStack);
                        stackReference = matrices.peek();
                        part = CustomModelPart.this;
                        visible = true;
                        matrices.pop();
                    }});
                    case RightItemOrigin -> FiguraMod.currentData.model.originModifications.put(ItemModelAPI.VANILLA_RIGHT_HAND_ID, new VanillaModelPartCustomization() {{
                        matrices.push();
                        applyTransformsAsItem(matrices);
                        applyTransformsAsItem(transformStack);
                        stackReference = matrices.peek();
                        part = CustomModelPart.this;
                        visible = true;
                        matrices.pop();
                    }});
                    case LeftElytraOrigin -> FiguraMod.currentData.model.originModifications.put(ElytraModelAPI.VANILLA_LEFT_WING_ID, new VanillaModelPartCustomization() {{
                        matrices.push();
                        applyTransformsAsElytraOrParrot(matrices);
                        applyTransformsAsElytraOrParrot(transformStack);
                        stackReference = matrices.peek();
                        part = CustomModelPart.this;
                        visible = true;
                        matrices.pop();
                    }});
                    case RightElytraOrigin -> FiguraMod.currentData.model.originModifications.put(ElytraModelAPI.VANILLA_RIGHT_WING_ID, new VanillaModelPartCustomization() {{
                        matrices.push();
                        applyTransformsAsElytraOrParrot(matrices);
                        applyTransformsAsElytraOrParrot(transformStack);
                        stackReference = matrices.peek();
                        part = CustomModelPart.this;
                        visible = true;
                        matrices.pop();
                    }});
                    case LeftParrotOrigin -> FiguraMod.currentData.model.originModifications.put(ParrotModelAPI.VANILLA_LEFT_PARROT_ID, new VanillaModelPartCustomization() {{
                        matrices.push();
                        applyTransformsAsElytraOrParrot(matrices);
                        applyTransformsAsElytraOrParrot(transformStack);
                        stackReference = matrices.peek();
                        part = CustomModelPart.this;
                        visible = true;
                        matrices.pop();
                    }});
                    case RightParrotOrigin -> FiguraMod.currentData.model.originModifications.put(ParrotModelAPI.VANILLA_RIGHT_PARROT_ID, new VanillaModelPartCustomization() {{
                        matrices.push();
                        applyTransformsAsElytraOrParrot(matrices);
                        applyTransformsAsElytraOrParrot(transformStack);
                        stackReference = matrices.peek();
                        part = CustomModelPart.this;
                        visible = true;
                        matrices.pop();
                    }});
                    case LeftSpyglass -> FiguraMod.currentData.model.originModifications.put(SpyglassModelAPI.VANILLA_LEFT_SPYGLASS_ID, new VanillaModelPartCustomization() {{
                        matrices.push();
                        applyTransformsAsSpyglass(matrices);
                        applyTransformsAsSpyglass(transformStack);
                        stackReference = matrices.peek();
                        part = CustomModelPart.this;
                        visible = true;
                        matrices.pop();
                    }});
                    case RightSpyglass -> FiguraMod.currentData.model.originModifications.put(SpyglassModelAPI.VANILLA_RIGHT_SPYGLASS_ID, new VanillaModelPartCustomization() {{
                        matrices.push();
                        applyTransformsAsSpyglass(matrices);
                        applyTransformsAsSpyglass(transformStack);
                        stackReference = matrices.peek();
                        part = CustomModelPart.this;
                        visible = true;
                        matrices.pop();
                    }});
                    case Camera -> {
                        Quaternion rot = MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation().copy();
                        Vec3f euler = rot.method_35828();
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-euler.getY()));
                        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-euler.getX()));
                        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(euler.getZ()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        applyTransforms(matrices);
        applyTransforms(transformStack);

        lastModelMatrix = transformStack.peek().getModel().copy();
        lastNormalMatrix = transformStack.peek().getNormal().copy();
        
        lastModelMatrixInverse = lastModelMatrix.copy();
        lastModelMatrixInverse.invert();
        lastNormalMatrixInverse = lastNormalMatrix.copy();
        lastNormalMatrixInverse.invert();

        //uv -> color -> alpha -> shaders
        u += this.uOffset;
        v += this.vOffset;

        Vec3f color = this.color.copy();
        color.multiplyComponentwise(prevColor.getX(), prevColor.getY(), prevColor.getZ());

        alpha = this.alpha * alpha;

        HashSet<ShaderType> shaders = (HashSet<ShaderType>) shadersToRender.clone();
        if (this.shaderType != ShaderType.None)
            shaders.add(this.shaderType);

        //render!
        if (renderOnly == null || this.parentType == renderOnly || canRenderChild) {
            canRenderChild = true;

            //render using default texture
            renderCube(leftToRender, matrices, vcp.getBuffer(RenderLayer.getEntityTranslucent(data.texture.id)), light, overlay, u, v, color, alpha);

            //render using other texture types
            for (FiguraTexture extraTexture : data.extraTextures) {
                Function<Identifier, RenderLayer> renderLayerGetter = FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.get(extraTexture.type);

                if (renderLayerGetter != null) {
                    VertexConsumer extraTextureVertexConsumer = vcp.getBuffer(renderLayerGetter.apply(extraTexture.id));
                    renderCube(leftToRender, matrices, extraTextureVertexConsumer, light, overlay, u, v, color, alpha);
                }
            }

            //shaders
            if (shaders.contains(ShaderType.EndPortal))
                renderCube(leftToRender, matrices, vcp.getBuffer(RenderLayer.getEndGateway()), light, overlay, u, v, color, alpha);
            if (shaders.contains(ShaderType.Glint))
                renderCube(leftToRender, matrices, vcp.getBuffer(RenderLayer.getDirectEntityGlint()), light, overlay, u, v, color, alpha);

            //render extras
            leftToRender = renderExtras(leftToRender, matrices, vcp, light);

            //render hit box
            if (canRenderHitBox) renderHitBox(matrices, vcp.getBuffer(RenderLayer.LINES));
        }

        for (CustomModelPart child : this.children) {
            if (leftToRender <= 0)
                break;

            //Don't render special parts.
            if (child.isParentSpecial())
                continue;

            //render part
            leftToRender = child.render(data, leftToRender, matrices, transformStack, vcp, light, overlay, u, v, color, alpha, shaders, canRenderChild);
        }

        matrices.pop();
        transformStack.pop();

        return leftToRender;
    }

    public void renderCube(int leftToRender, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float u, float v, Vec3f color, float alpha) {
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

            Vec3f normal = new Vec3f(
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

        ((VertexConsumerProvider.Immediate) FiguraMod.vertexConsumerProvider).draw();
    }

    public int renderExtras(int leftToRender, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        //Render extra parts
        for (RenderTask task : this.renderTasks) {
            leftToRender -= task.render(matrices, vcp, light);
            ((VertexConsumerProvider.Immediate) FiguraMod.vertexConsumerProvider).draw();

            if (leftToRender <= 0) break;
        }

        return leftToRender;
    }

    public void renderHitBox(MatrixStack matrices, VertexConsumer vertices) {
        Vec3f color;
        float boxSize;
        if (this instanceof CustomModelPartCuboid) {
            color = new Vec3f(1f, 0.45f, 0.72f); //0xff72b7 aka fran_pink
            boxSize = 1 / 48f;
        }
        else {
            color = new Vec3f(0.69f, 0.95f, 1f); //0xaff2ff aka ace_blue
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
        //don't render filtered parts
        if (!this.visible || this.isParentSpecial() || this.isHidden) {
            return 0;
        }

        int complexity = this.vertexCount;

        //iterate over children
        for (CustomModelPart child : this.children) {
            complexity += child.getComplexity();
        }

        return complexity;
    }

    public void applyTransforms(MatrixStack stack) {
        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);

        stack.translate(-this.pivot.getX() / 16.0f, -this.pivot.getY() / 16.0f, -this.pivot.getZ() / 16.0f);

        if (this.isMimicMode || this.rotationType == RotationType.Vanilla) {
            stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(this.rot.getZ()));
            stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(this.rot.getY()));
            stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(this.rot.getX()));
        } else if (this.rotationType == RotationType.BlockBench) {
            stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(this.rot.getZ()));
            stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-this.rot.getY()));
            stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-this.rot.getX()));
        }
        stack.scale(this.scale.getX(), this.scale.getY(), this.scale.getZ());

        stack.translate(this.pivot.getX() / 16.0f, this.pivot.getY() / 16.0f, this.pivot.getZ() / 16.0f);
    }

    //TODO move these to the mixins, probably.
    public void applyTransformsAsItem(MatrixStack stack) {
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90.0F));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F));
        //stack.translate(0, 0.125D, -0.625D);
        stack.translate(pivot.getX() / 16.0f, pivot.getZ() / 16.0f, pivot.getY() / 16.0f);
        stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(this.rot.getZ()));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-this.rot.getY()));
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-this.rot.getX()));
        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);
    }

    //TODO move these to the mixins, probably.
    public void applyTransformsAsElytraOrParrot(MatrixStack stack) {
        stack.translate(pivot.getX() / 16.0f, pivot.getY() / 16.0f, -pivot.getZ() / 16.0f);
        stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(this.rot.getZ()));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-this.rot.getY()));
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-this.rot.getX()));
        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);
    }

    //TODO move these to the mixins, probably. //OK GOT IT
    public void applyTransformsAsSpyglass(MatrixStack stack) {
        stack.translate(-pivot.getX() / 16.0f, -pivot.getY() / 16.0f, -pivot.getZ() / 16.0f);
        stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(this.rot.getZ()));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-this.rot.getY()));
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-this.rot.getX()));
        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);
    }

    //Re-builds the mesh data for a custom model part.
    public void rebuild() {}

    public void addVertex(Vec3f vert, float u, float v, Vec3f normal) {
        this.vertexData.add(vert.getX() / 16.0f);
        this.vertexData.add(vert.getY() / 16.0f);
        this.vertexData.add(vert.getZ() / 16.0f);
        this.vertexData.add(u);
        this.vertexData.add(v);
        this.vertexData.add(-normal.getX());
        this.vertexData.add(-normal.getY());
        this.vertexData.add(-normal.getZ());
        this.vertexCount++;
    }

    public void readNbt(NbtCompound partNbt) {
        //Name
        this.name = partNbt.get("nm").asString();

        if (partNbt.contains("pos")) {
            NbtList list = (NbtList) partNbt.get("pos");
            this.pos = vec3fFromNbt(list);
        }
        if (partNbt.contains("rot")) {
            NbtList list = (NbtList) partNbt.get("rot");
            this.rot = vec3fFromNbt(list);
        }
        if (partNbt.contains("scl")) {
            NbtList list = (NbtList) partNbt.get("scl");
            this.scale = vec3fFromNbt(list);
        }
        if (partNbt.contains("piv")) {
            NbtList list = (NbtList) partNbt.get("piv");
            this.pivot = vec3fFromNbt(list);
        }

        if (partNbt.contains("ptype")) {
            try {
                this.parentType = ParentType.valueOf(partNbt.get("ptype").asString());
            } catch (Exception ignored) {}
        }
        if (partNbt.contains("mmc")) {
            this.isMimicMode = ((NbtByte) partNbt.get("mmc")).byteValue() == 1;
        }

        if (partNbt.contains("vsb")) {
            this.isHidden = partNbt.getBoolean("vsb");
        }

        if (partNbt.contains("alp")) {
            this.alpha = partNbt.getFloat("alp");
        }

        if (partNbt.contains("stype")) {
            try {
                this.shaderType = ShaderType.valueOf(partNbt.get("stype").asString());
            } catch (Exception ignored) {}
        }

        if (partNbt.contains("chld")) {
            NbtList childrenNbt = (NbtList) partNbt.get("chld");
            if (childrenNbt == null || childrenNbt.getHeldType() != NbtType.COMPOUND)
                return;

            for (NbtElement child : childrenNbt) {
                NbtCompound childNbt = (NbtCompound) child;
                CustomModelPart part = fromNbt(childNbt);
                part.rebuild();
                this.children.add(part);
            }
        }
    }

    public void writeNbt(NbtCompound partNbt) {
        partNbt.put("nm", NbtString.of(name));

        if (!this.pos.equals(new Vec3f(0, 0, 0))) {
            partNbt.put("pos", vec3fToNbt(this.pos));
        }
        if (!this.rot.equals(new Vec3f(0, 0, 0))) {
            partNbt.put("rot", vec3fToNbt(this.rot));
        }
        if (!this.scale.equals(new Vec3f(1, 1, 1))) {
            partNbt.put("scl", vec3fToNbt(this.scale));
        }
        if (!this.pivot.equals(new Vec3f(0, 0, 0))) {
            partNbt.put("piv", vec3fToNbt(this.pivot));
        }

        if (this.parentType != ParentType.None) {
            partNbt.put("ptype", NbtString.of(this.parentType.toString()));
        }
        partNbt.put("mmc", NbtByte.of(this.isMimicMode));

        if (this.isHidden) {
            partNbt.put("vsb", NbtByte.of(true));
        }

        if (this.alpha != 1.0f) {
            partNbt.put("alp", NbtFloat.of(this.alpha));
        }

        if (this.shaderType != ShaderType.None) {
            partNbt.put("stype", NbtString.of(this.shaderType.toString()));
        }

        //Parse children.
        if (this.children.size() > 0) {
            NbtList childrenList = new NbtList();

            for (CustomModelPart child : this.children) {
                NbtCompound childNbt = new NbtCompound();
                writeToNbt(childNbt, child);
                childrenList.add(childNbt);
            }

            partNbt.put("chld", childrenList);
        }
    }

    public String getPartType() {
        //Default part type is N/A
        return "na";
    }

    public boolean isParentSpecial() {
        return parentType == ParentType.WORLD || parentType == ParentType.LeftElytra || parentType == ParentType.RightElytra;
    }

    public void applyTrueOffset(Vec3f offset) {}

    public enum ParentType {
        None,
        Model,
        Head,
        LeftArm,
        RightArm,
        LeftLeg,
        RightLeg,
        Torso,
        WORLD,
        LeftItemOrigin, //Origin position of the held item in the left hand
        RightItemOrigin, //Origin position of the held item
        LeftElytraOrigin, //Left origin position of the elytra
        RightElytraOrigin, //Right origin position of the elytra
        LeftParrotOrigin, //Left origin position of the shoulder parrot
        RightParrotOrigin, //Right origin position of the shoulder parrot
        LeftElytra, //Left position of the elytra model
        RightElytra, //Right position of the elytra model,
        LeftSpyglass, //Left position of the spyglass model
        RightSpyglass, //Right position of the spyglass model
        Camera //paparazzi
    }

    public enum RotationType {
        BlockBench,
        Vanilla
    }

    public enum ShaderType {
        None,
        EndPortal,
        Glint
    }

    public enum RenderType {
        None,
        Cutout,
        CutoutNoCull,
        Translucent,
        TranslucentNoCull,
        NoTransparent
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
    public static CustomModelPart fromNbt(NbtCompound nbt) {
        if (!nbt.contains("pt"))
            return null;
        String partType = nbt.get("pt").asString();

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
    public static void writeToNbt(NbtCompound nbt, CustomModelPart part) {
        String partType = part.getPartType();
        if (!MODEL_PART_TYPES.containsKey(partType))
            return;

        nbt.put("pt", NbtString.of(partType));
        part.writeNbt(nbt);
    }

    private static Vec3f vec3fFromNbt(@Nullable NbtList nbt) {
        if (nbt == null || nbt.getHeldType() != NbtType.FLOAT)
            return new Vec3f(0.f, 0.f, 0.f);
        return new Vec3f(nbt.getFloat(0), nbt.getFloat(1), nbt.getFloat(2));
    }

    private static NbtList vec3fToNbt(Vec3f vec) {
        NbtList nbt = new NbtList();
        nbt.add(NbtFloat.of(vec.getX()));
        nbt.add(NbtFloat.of(vec.getY()));
        nbt.add(NbtFloat.of(vec.getZ()));
        return nbt;
    }
}
