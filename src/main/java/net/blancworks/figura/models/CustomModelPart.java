package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public class CustomModelPart {

    public String name = "NULL";

    //Transform data
    public Vector3f pivot = new Vector3f();
    public Vector3f pos = new Vector3f();
    public Vector3f rot = new Vector3f();
    public Vector3f scale = new Vector3f(1, 1, 1);
    public Vector3f color = new Vector3f(1,1,1);

    //Offsets
    public float uOffset = 0;
    public float vOffset = 0;

    public boolean visible = true;

    public ParentType parentType = ParentType.None;

    public ArrayList<CustomModelPart> children = new ArrayList<>();

    //All the vertex data is stored here! :D
    public FloatList vertexData = new FloatArrayList();
    public int vertexCount = 0;

    public int render(int left_to_render, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        return render(left_to_render, matrices, vertices, light, overlay, 0, 0, new Vector3f(1,1,1));
    }

    //Renders this custom model part and all its children.
    //Returns the cuboids left to render after this one, and only renders until left_to_render is zero.
    public int render(int left_to_render, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float u, float v, Vector3f prevColor) {

        //Don't render invisible parts.
        if (!visible) {
            return left_to_render;
        }
        matrices.push();

        applyTransforms(matrices);

        Matrix4f modelMatrix = matrices.peek().getModel();
        Matrix3f normalMatrix = matrices.peek().getNormal();

        u += uOffset;
        v += vOffset;

        Vector3f tempColor = color.copy();
        tempColor.multiplyComponentwise(prevColor.getX(), prevColor.getY(), prevColor.getZ());

        for (int i = 0; i < vertexCount; i++) {
            int startIndex = i * 8;

            //Get vertex.
            Vector4f fullVert = new Vector4f(
                    vertexData.getFloat(startIndex++),
                    vertexData.getFloat(startIndex++),
                    vertexData.getFloat(startIndex++),
                    1
            );

            float vertU = vertexData.getFloat(startIndex++);
            float vertV = vertexData.getFloat(startIndex++);

            Vector3f normal = new Vector3f(
                    vertexData.getFloat(startIndex++),
                    vertexData.getFloat(startIndex++),
                    vertexData.getFloat(startIndex++)
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
                left_to_render--;

                if (left_to_render == 0)
                    break;
            }
        }

        for (CustomModelPart child : children) {
            if (left_to_render == 0)
                break;
            left_to_render = child.render(left_to_render, matrices, vertices, light, overlay, u, v, tempColor);
        }

        matrices.pop();
        return left_to_render;
    }

    public void applyTransforms(MatrixStack stack) {
        stack.translate(-pivot.getX() / 16.0f, -pivot.getY() / 16.0f, -pivot.getZ() / 16.0f);

        stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
        stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-rot.getY()));
        stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-rot.getX()));

        stack.translate(pos.getX() / 16.0f, pos.getY() / 16.0f, pos.getZ() / 16.0f);

        stack.translate(pivot.getX() / 16.0f, pivot.getY() / 16.0f, pivot.getZ() / 16.0f);

        stack.scale(scale.getX(), scale.getY(), scale.getZ());
    }

    //Re-builds the mesh data for a custom model part.
    public void rebuild() {
    }

    public void addVertex(Vector3f vert, float u, float v, Vector3f normal) {
        vertexData.add(vert.getX() / 16.0f);
        vertexData.add(vert.getY() / 16.0f);
        vertexData.add(vert.getZ() / 16.0f);
        vertexData.add(u);
        vertexData.add(v);
        vertexData.add(-normal.getX());
        vertexData.add(-normal.getY());
        vertexData.add(-normal.getZ());
        vertexCount++;
    }

    public void fromNBT(CompoundTag partTag) {

        //Name
        name = partTag.get("nm").asString();

        if (partTag.contains("pos")) {
            ListTag list = (ListTag) partTag.get("pos");
            pos = new Vector3f(
                    list.getFloat(0),
                    list.getFloat(1),
                    list.getFloat(2)
            );
        }
        if (partTag.contains("rot")) {
            ListTag list = (ListTag) partTag.get("rot");
            rot = new Vector3f(
                    list.getFloat(0),
                    list.getFloat(1),
                    list.getFloat(2)
            );
        }
        if (partTag.contains("scl")) {
            ListTag list = (ListTag) partTag.get("scl");
            scale = new Vector3f(
                    list.getFloat(0),
                    list.getFloat(1),
                    list.getFloat(2)
            );
        }
        if (partTag.contains("piv")) {
            ListTag list = (ListTag) partTag.get("piv");
            pivot = new Vector3f(
                    list.getFloat(0),
                    list.getFloat(1),
                    list.getFloat(2)
            );
        }

        if (partTag.contains("ptype")) {
            parentType = ParentType.valueOf(partTag.get("ptype").asString());
        }

        if (partTag.contains("vsb")) {
            visible = partTag.getBoolean("vsb");
        }

        if (partTag.contains("uv")) {
            ListTag uvOffsetTag = (ListTag) partTag.get("uv");
            uOffset = uvOffsetTag.getFloat(0);
            vOffset = uvOffsetTag.getFloat(1);
        }

        if (partTag.contains("chld")) {
            ListTag childTag = (ListTag) partTag.get("chld");

            for (Tag child : childTag) {
                CompoundTag ct = (CompoundTag) child;
                CustomModelPart part = getFromNbtTag(ct);
                part.rebuild();
                children.add(part);
            }
        }
    }

    public void toNBT(CompoundTag partTag) {
        partTag.put("nm", StringTag.of(name));

        if (!pos.equals(new Vector3f(0, 0, 0))) {
            ListTag posTag = new ListTag() {{
                add(FloatTag.of(pos.getX()));
                add(FloatTag.of(pos.getY()));
                add(FloatTag.of(pos.getZ()));
            }};
            partTag.put("pos", posTag);
        }
        if (!rot.equals(new Vector3f(0, 0, 0))) {
            ListTag rotTag = new ListTag() {{
                add(FloatTag.of(rot.getX()));
                add(FloatTag.of(rot.getY()));
                add(FloatTag.of(rot.getZ()));
            }};
            partTag.put("rot", rotTag);
        }
        if (!scale.equals(new Vector3f(1, 1, 1))) {
            ListTag scaleTag = new ListTag() {{
                add(FloatTag.of(scale.getX()));
                add(FloatTag.of(scale.getY()));
                add(FloatTag.of(scale.getZ()));
            }};
            partTag.put("scl", scaleTag);
        }
        if (!pivot.equals(new Vector3f(0, 0, 0))) {
            ListTag pivTag = new ListTag() {{
                add(FloatTag.of(pivot.getX()));
                add(FloatTag.of(pivot.getY()));
                add(FloatTag.of(pivot.getZ()));
            }};
            partTag.put("piv", pivTag);
        }

        if(Math.abs(uOffset) > 0.0001f && Math.abs(vOffset) > 0.0001f){
            ListTag uvOffsetTag = new ListTag(){{
                add(FloatTag.of(uOffset));
                add(FloatTag.of(vOffset));
            }};
            partTag.put("uv", uvOffsetTag);
        }

        if (parentType != ParentType.None) {
            partTag.put("ptype", StringTag.of(parentType.toString()));
        }

        if (!visible) {
            partTag.put("vsb", ByteTag.of(false));
        }

        //Parse children.
        if (children.size() > 0) {
            ListTag childrenTag = new ListTag();

            for (CustomModelPart child : children) {
                CompoundTag tag = new CompoundTag();
                writeToCompoundTag(tag, child);
                childrenTag.add(tag);
            }

            partTag.put("chld", childrenTag);
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
        Torso
    }

    //---------MODEL PART TYPES---------

    public static HashMap<String, Supplier<CustomModelPart>> model_part_types = new HashMap<String, Supplier<CustomModelPart>>() {{
        put("na", CustomModelPart::new);
        put("cub", CustomModelPartCuboid::new);
        put("msh", CustomModelPartMesh::new);
    }};

    //Get a CustomModelPart from a tag, automatically reading the type from that tag.
    public static <T extends CustomModelPart> CustomModelPart getFromNbtTag(CompoundTag tag) {

        if (!tag.contains("pt"))
            return null;
        String partType = tag.get("pt").asString();

        if (!model_part_types.containsKey(partType))
            return null;

        Supplier sup = model_part_types.get(partType);
        CustomModelPart part = (CustomModelPart) sup.get();

        part.fromNBT(tag);
        return part;
    }

    //Write a model part to an NBT Compound Tag
    public static void writeToCompoundTag(CompoundTag tag, CustomModelPart part) {
        String partType = part.getPartType();
        if (!model_part_types.containsKey(partType))
            return;

        tag.put("pt", StringTag.of(partType));
        part.toNBT(tag);
    }
}
