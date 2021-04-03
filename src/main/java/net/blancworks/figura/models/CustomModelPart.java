package net.blancworks.figura.models;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.access.MatrixStackAccess;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.nbt.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

public class CustomModelPart {
    public String name = "NULL";

    //Transform data
    public Vector3f pivot = new Vector3f();
    public Vector3f pos = new Vector3f();
    public Vector3f rot = new Vector3f();
    public Vector3f scale = new Vector3f(1, 1, 1);
    public Vector3f color = new Vector3f(1, 1, 1);

    //Offsets
    public float uOffset = 0;
    public float vOffset = 0;

    public boolean visible = true;

    public ParentType parentType = ParentType.None;
    public boolean isMimicMode = false;

    public RotationType rotationType = RotationType.BlockBench;

    public ArrayList<CustomModelPart> children = new ArrayList<>();

    //All the vertex data is stored here! :D
    public FloatList vertexData = new FloatArrayList();
    public int vertexCount = 0;

    public int render(int leftToRender, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        return render(leftToRender, matrices, vertices, light, overlay, 0, 0, new Vector3f(1, 1, 1));
    }

    //Renders this custom model part and all its children.
    //Returns the cuboids left to render after this one, and only renders until left_to_render is zero.
    public int render(int leftToRender, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float u, float v, Vector3f prevColor) {
        //Don't render invisible parts.
        if (!this.visible) {
            return leftToRender;
        }
        MatrixStack tempStack = null;
        
        //if(tempStack != null)
            //matrices = tempStack;
        
        matrices.push();

        try {

            if (this.isMimicMode) {
                PlayerEntityModel model = FiguraMod.currentModel;

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
            } else if(parentType != CustomModelPart.ParentType.Model){
                PlayerEntityModel playerModel = FiguraMod.currentModel;

                switch (parentType) {
                    case Head:
                        playerModel.head.rotate(matrices);
                        break;
                    case Torso:
                        playerModel.torso.rotate(matrices);
                        break;
                    case LeftArm:
                        playerModel.leftArm.rotate(matrices);
                        break;
                    case LeftLeg:
                        playerModel.leftLeg.rotate(matrices);
                        break;
                    case RightArm:
                        playerModel.rightArm.rotate(matrices);
                        break;
                    case RightLeg:
                        playerModel.rightLeg.rotate(matrices);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        applyTransforms(matrices);

        Matrix4f modelMatrix = matrices.peek().getModel();
        Matrix3f normalMatrix = matrices.peek().getNormal();

        u += this.uOffset;
        v += this.vOffset;

        Vector3f tempColor = color.copy();
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

            Vector3f normal = new Vector3f(
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex++)
            );

            fullVert.transform(modelMatrix);
            normal.transform(normalMatrix);

            //Push vertex.
            vertices.vertex(
                    fullVert.getX(), fullVert.getY(), fullVert.getZ(),
                    tempColor.getX(), tempColor.getY(), tempColor.getZ(), 1,
                    vertU + u, vertV + v,
                    overlay, light,
                    normal.getX(), normal.getY(), normal.getZ()
            );

            //Every 4 verts (1 face)
            if (i % 4 == 0) {
                leftToRender--;

                if (leftToRender == 0)
                    break;
            }
        }

        for (CustomModelPart child : this.children) {
            if (leftToRender == 0)
                break;
            if(child.parentType == CustomModelPart.ParentType.WORLD)
                continue;
            leftToRender = child.render(leftToRender, matrices, vertices, light, overlay, u, v, tempColor);
        }

        matrices.pop();
        return leftToRender;
    }

    public void applyTransforms(MatrixStack stack) {
        stack.translate(-this.pivot.getX() / 16.0f, -this.pivot.getY() / 16.0f, -this.pivot.getZ() / 16.0f);

        if (this.isMimicMode || this.rotationType == RotationType.Vanilla) {
            stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(this.rot.getZ()));
            stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(this.rot.getY()));
            stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(this.rot.getX()));
        } else if (this.rotationType == RotationType.BlockBench) {
            stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(this.rot.getZ()));
            stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-this.rot.getY()));
            stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-this.rot.getX()));
        }

        stack.translate(this.pos.getX() / 16.0f, this.pos.getY() / 16.0f, this.pos.getZ() / 16.0f);

        stack.translate(this.pivot.getX() / 16.0f, this.pivot.getY() / 16.0f, this.pivot.getZ() / 16.0f);

        stack.scale(this.scale.getX(), this.scale.getY(), this.scale.getZ());
    }

    //Re-builds the mesh data for a custom model part.
    public void rebuild() {
    }

    public void addVertex(Vector3f vert, float u, float v, Vector3f normal) {
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

    public void readNbt(CompoundTag partNbt) {
        //Name
        this.name = partNbt.get("nm").asString();

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
            this.parentType = ParentType.valueOf(partNbt.get("ptype").asString());
        }
        if (partNbt.contains("mmc")) {
            this.isMimicMode = ((ByteTag) partNbt.get("mmc")).getByte() == 1;
        }

        if (partNbt.contains("vsb")) {
            this.visible = partNbt.getBoolean("vsb");
        }

        if (partNbt.contains("uv")) {
            ListTag uvOffsetNbt = (ListTag) partNbt.get("uv");
            this.uOffset = uvOffsetNbt.getFloat(0);
            this.vOffset = uvOffsetNbt.getFloat(1);
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
        partNbt.put("nm", StringTag.of(name));

        if (!this.pos.equals(new Vector3f(0, 0, 0))) {
            partNbt.put("pos", vec3fToNbt(this.pos));
        }
        if (!this.rot.equals(new Vector3f(0, 0, 0))) {
            partNbt.put("rot", vec3fToNbt(this.rot));
        }
        if (!this.scale.equals(new Vector3f(1, 1, 1))) {
            partNbt.put("scl", vec3fToNbt(this.scale));
        }
        if (!this.pivot.equals(new Vector3f(0, 0, 0))) {
            partNbt.put("piv", vec3fToNbt(this.pivot));
        }

        if (Math.abs(this.uOffset) > 0.0001f && Math.abs(this.vOffset) > 0.0001f) {
            ListTag uvOffsetNbt = new ListTag() {{
                add(FloatTag.of(uOffset));
                add(FloatTag.of(vOffset));
            }};
            partNbt.put("uv", uvOffsetNbt);
        }

        if (this.parentType != ParentType.None) {
            partNbt.put("ptype", StringTag.of(this.parentType.toString()));
        }
        partNbt.put("mmc", ByteTag.of(this.isMimicMode));

        if (!this.visible) {
            partNbt.put("vsb", ByteTag.of(false));
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

    public String getPartType() {
        //Default part type is N/A
        return "na";
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
        WORLD
    }

    public enum RotationType {
        BlockBench,
        Vanilla
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
     * @param nbt the NBT compound
     * @param part the model part
     */
    public static void writeToNbt(CompoundTag nbt, CustomModelPart part) {
        String partType = part.getPartType();
        if (!MODEL_PART_TYPES.containsKey(partType))
            return;

        nbt.put("pt", StringTag.of(partType));
        part.writeNbt(nbt);
    }

    private static Vector3f vec3fFromNbt(@Nullable ListTag nbt) {
        if (nbt == null || nbt.getElementType() != NbtType.FLOAT)
            return new Vector3f(0.f, 0.f, 0.f);
        return new Vector3f(nbt.getFloat(0), nbt.getFloat(1), nbt.getFloat(2));
    }

    private static ListTag vec3fToNbt(Vector3f vec) {
        ListTag nbt = new ListTag();
        nbt.add(FloatTag.of(vec.getX()));
        nbt.add(FloatTag.of(vec.getY()));
        nbt.add(FloatTag.of(vec.getZ()));
        return nbt;
    }
}
