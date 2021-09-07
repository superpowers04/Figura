package net.blancworks.figura.models;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.lua.api.RendererAPI;
import net.blancworks.figura.lua.api.model.*;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
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

    public boolean shouldRender = true;

    public RenderType renderType = RenderType.None;

    public float alpha = 1.0f;

    //All the vertex data is stored here! :D
    public FloatList vertexData = new FloatArrayList();
    public int vertexCount = 0;

    public Matrix4f lastModelMatrix = new Matrix4f();
    public Matrix3f lastNormalMatrix = new Matrix3f();

    public Matrix4f lastModelMatrixInverse = new Matrix4f();
    public Matrix3f lastNormalMatrixInverse = new Matrix3f();

    // Items/Blocks to render with this part
    public Queue<RendererAPI.RenderTask> renderTasks = new LinkedList<>();

    public RenderLayer temp;

    //Renders a model part (and all sub-parts) using the textures provided by a PlayerData instance.
    public int renderUsingAllTextures(PlayerData data,  MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        if(data.texture.isDone) {

            //apply part alpha value
            alpha = this.alpha * alpha;

            //Store this value for extra textures
            int prevLeftToRender = data.model.leftToRender;

            //Render with main texture.
            temp = RenderLayer.getEntityTranslucent(data.texture.id);
            VertexConsumer mainTextureConsumer = vcp.getBuffer(temp);
            int ret = render(prevLeftToRender, matrices, transformStack, mainTextureConsumer, light, overlay, alpha, true);

            //Render extra textures (emission, that sort)
            for (FiguraTexture extraTexture : data.extraTextures) {
                Function<Identifier, RenderLayer> renderLayerGetter = FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.get(extraTexture.type);

                if (renderLayerGetter != null) {
                    VertexConsumer extraTextureVertexConsumer = vcp.getBuffer(renderLayerGetter.apply(extraTexture.id));

                    render(prevLeftToRender, matrices, transformStack, extraTextureVertexConsumer, light, overlay, alpha, false);
                }
            }

            //render shader groups

            //end portal
            excludeFilterParts(this, ShaderType.EndPortal);

            VertexConsumer portalConsumer = vcp.getBuffer(RenderLayer.getEndGateway());
            render(prevLeftToRender, matrices, transformStack, portalConsumer, light, overlay, alpha, false);

            //glint
            filterParts(this, ShaderType.Glint);

            VertexConsumer glintConsumer = vcp.getBuffer(RenderLayer.getDirectEntityGlint());
            render(prevLeftToRender, matrices, transformStack, glintConsumer, light, overlay, alpha, false);

            //reset rendering status
            setRenderStatus(this, true);

            return ret;
        }
        return 0;
    }

    public int renderUsingAllTexturesFiltered(ParentType filter, PlayerData data, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        filterParts(this, filter);
        return renderUsingAllTextures(data, matrices, transformStack, vcp, light, overlay, alpha);
    }

    public int render(int leftToRender, MatrixStack matrices, MatrixStack transformStack, VertexConsumer vertices, int light, int overlay, float alpha, boolean mayRenderExtras) {
        return render(leftToRender, matrices, transformStack, vertices, light, overlay, 0, 0, new Vec3f(1, 1, 1), alpha, mayRenderExtras);
    }

    //Renders this custom model part and all its children.
    //Returns the cuboids left to render after this one, and only renders until left_to_render is zero.
    public int render(int leftToRender, MatrixStack matrices, MatrixStack transformStack, VertexConsumer vertices, int light, int overlay, float u, float v, Vec3f prevColor, float alpha, boolean mayRenderExtras) {
        //Don't render invisible parts.
        if (!this.visible || !this.shouldRender || this.isHidden) {
            return leftToRender;
        }

        matrices.push();
        transformStack.push();

        try {
            if (this.isMimicMode && parentType != ParentType.None && parentType != ParentType.Model) {
                PlayerEntityModel model = FiguraMod.currentData.vanillaModel;

                switch (this.parentType) {
                    case Head:
                        this.rot = new Vec3f(model.head.pitch, model.head.yaw, model.head.roll);
                        break;
                    case Torso:
                        this.rot = new Vec3f(model.body.pitch, model.body.yaw, model.body.roll);
                        break;
                    case LeftArm:
                        this.rot = new Vec3f(model.leftArm.pitch, model.leftArm.yaw, model.leftArm.roll);
                        break;
                    case LeftLeg:
                        this.rot = new Vec3f(model.leftLeg.pitch, model.leftLeg.yaw, model.leftLeg.roll);
                        break;
                    case RightArm:
                        this.rot = new Vec3f(model.rightArm.pitch, model.rightArm.yaw, model.rightArm.roll);
                        break;
                    case RightLeg:
                        this.rot = new Vec3f(model.rightLeg.pitch, model.rightLeg.yaw, model.rightLeg.roll);
                        break;
                }

                float multiply = 57.2958f;
                this.rot.multiplyComponentwise(multiply, multiply, multiply);
            } else if (parentType != ParentType.Model) {
                PlayerEntityModel playerModel = FiguraMod.currentData.vanillaModel;

                switch (parentType) {
                    case Head:
                        playerModel.head.rotate(matrices);
                        playerModel.head.rotate(transformStack);
                        break;
                    case Torso:
                        playerModel.body.rotate(matrices);
                        playerModel.body.rotate(transformStack);
                        break;
                    case LeftArm:
                        playerModel.leftArm.rotate(matrices);
                        playerModel.leftArm.rotate(transformStack);
                        break;
                    case LeftLeg:
                        playerModel.leftLeg.rotate(matrices);
                        playerModel.leftLeg.rotate(transformStack);
                        break;
                    case RightArm:
                        playerModel.rightArm.rotate(matrices);
                        playerModel.rightArm.rotate(transformStack);
                        break;
                    case RightLeg:
                        playerModel.rightLeg.rotate(matrices);
                        playerModel.rightLeg.rotate(transformStack);
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

        Matrix4f modelMatrix = matrices.peek().getModel();
        Matrix3f normalMatrix = matrices.peek().getNormal();

        u += this.uOffset;
        v += this.vOffset;

        Vec3f tempColor = color.copy();
        tempColor.multiplyComponentwise(prevColor.getX(), prevColor.getY(), prevColor.getZ());

        for (int i = 0; i < this.vertexCount; i++) {
            int startIndex = i * 8;

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
                    this.vertexData.getFloat(startIndex++)
            );

            fullVert.transform(modelMatrix);
            normal.transform(normalMatrix);

            //Push vertex.
            vertices.vertex(
                    fullVert.getX(), fullVert.getY(), fullVert.getZ(),
                    tempColor.getX(), tempColor.getY(), tempColor.getZ(), alpha,
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

        for (CustomModelPart child : this.children) {
            if (leftToRender == 0)
                break;

            //Don't render special parts.
            if (child.isParentSpecial())
                continue;

            //set child alpha
            float childAlpha = child.alpha * alpha;

            //render part
            leftToRender = child.render(leftToRender, matrices, transformStack, vertices, light, overlay, u, v, tempColor, childAlpha, mayRenderExtras);
        }

        /** temp
         * if (mayRenderExtras) {
         *             MinecraftClient client = MinecraftClient.getInstance();
         *             while (!renderTasks.isEmpty() && leftToRender > 0) {
         *                 RendererAPI.RenderTask task = renderTasks.remove();
         *                 matrices.push();
         *                 transformStack.push();
         *                 leftToRender -= task.render(matrices, transformStack, FiguraMod.vertexConsumerProvider, light, overlay, tempColor.getX(), tempColor.getY(), tempColor.getZ(), alpha, client);
         *                 matrices.pop();
         *                 transformStack.pop();
         *             }
         *         }
         */

        transformStack.pop();
        matrices.pop();



        return leftToRender;
    }

    public int getComplexity() {
        //don't render filtered parts
        if (!this.visible || !this.shouldRender || this.isParentSpecial() || this.isHidden) {
            return 0;
        }

        int complexity = this.vertexCount;

        //iterate over children
        for (CustomModelPart child : this.children) {
            complexity += child.getComplexity();
        }

        return complexity;
    }

    public static void setRenderStatus(CustomModelPart part, boolean status) {
        //set for parent
        part.shouldRender = status;

        //iterate over the children
        for (CustomModelPart child : part.children) {
            setRenderStatus(child, status);
        }
    }

    public static void filterParts(CustomModelPart part, Object filter) {
        //error temp variable
        boolean unmatched = false;

        //check for filter type, then flag to render if the property matches the filter
        if (filter instanceof ParentType) {
            if (part.parentType == filter)
                setRenderStatus(part, true);
            else
                unmatched = true;
        }
        else if (filter instanceof ShaderType) {
            if (part.shaderType == filter)
                setRenderStatus(part, true);
            else
                unmatched = true;
        }
        else if (filter instanceof RenderType) {
            if (part.renderType == filter)
                setRenderStatus(part, true);
            else
                unmatched = true;
        }
        else {
            //filter not found, then no elements should render
            unmatched = true;
        }

        if (unmatched) {
            //flag it to dont render
            part.shouldRender = false;

            //if is a group, enable the group rendering and iterate through its children
            if (!(part instanceof CustomModelPartCuboid) && !(part instanceof CustomModelPartMesh)) {
                part.shouldRender = true;
                for (CustomModelPart child : part.children) {
                    filterParts(child, filter);
                }
            }
        }
    }

    public static void excludeFilterParts(CustomModelPart part, Object filter) {
        //error temp variable
        boolean unmatched = false;

        //check for filter type, then flag to render if the property matches the filter
        if (filter instanceof ParentType) {
            if (part.parentType != filter)
                unmatched = true;
        }
        else if (filter instanceof ShaderType) {
            if (part.shaderType != filter)
                unmatched = true;
        }
        else if (filter instanceof RenderType) {
            if (part.renderType != filter)
                unmatched = true;
        }
        else {
            //filter not found, then no elements should render
            unmatched = true;
        }

        if (unmatched) {
            //flag it to dont render
            part.shouldRender = false;

            //if is a group, enable the group rendering and iterate through its children
            if (!(part instanceof CustomModelPartCuboid) && !(part instanceof CustomModelPartMesh)) {
                part.shouldRender = true;
                for (CustomModelPart child : part.children) {
                    excludeFilterParts(child, filter);
                }
            }
        }
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
    public void rebuild() {
    }

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

    public void applyTrueOffset(Vec3f offset) {
    }

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
        RightSpyglass //Right position of the spyglass model
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
