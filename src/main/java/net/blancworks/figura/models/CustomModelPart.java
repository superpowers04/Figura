package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public class CustomModelPart {

    public String name = "NULL";

    //Transform data
    public Vec3f pivot = new Vec3f();
    public Vec3f pos = new Vec3f();
    public Vec3f rot = new Vec3f();
    public Vec3f scale = new Vec3f(1, 1, 1);

    //Offsets
    public float uOffset = 0;
    public float vOffset = 0;

    public boolean visible = true;

    public ParentType parentType = ParentType.None;

    public ArrayList<CustomModelPart> children = new ArrayList<>();

    //All the vertex data is stored here! :D
    public FloatList vertexData = new FloatArrayList();
    public int vertexCount = 0;

    //Renders this custom model part and all its children.
    //Returns the cuboids left to render after this one, and only renders until left_to_render is zero.
    public int render(int left_to_render, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {

        //Don't render invisible parts.
        if (!visible) {
            return left_to_render;
        }
        matrices.push();

        applyTransforms(matrices);

        Matrix4f modelMatrix = matrices.peek().getModel();
        Matrix3f normalMatrix = matrices.peek().getNormal();

        for (int i = 0; i < vertexCount; i++) {
            int startIndex = i * 8;

            //Get vertex.
            Vector4f fullVert = new Vector4f(
                    vertexData.getFloat(startIndex++),
                    vertexData.getFloat(startIndex++),
                    vertexData.getFloat(startIndex++),
                    1
            );

            float u = vertexData.getFloat(startIndex++);
            float v = vertexData.getFloat(startIndex++);

            Vec3f normal = new Vec3f(
                    vertexData.getFloat(startIndex++),
                    vertexData.getFloat(startIndex++),
                    vertexData.getFloat(startIndex++)
            );

            fullVert.transform(modelMatrix);
            normal.transform(normalMatrix);

            //Push vertex.
            vertices.vertex(
                    fullVert.getX(), fullVert.getY(), fullVert.getZ(),
                    1, 1, 1, 1,
                    u + uOffset, v + vOffset,
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
            left_to_render = child.render(left_to_render, matrices, vertices, light, overlay);
        }

        matrices.pop();
        return left_to_render;
    }

    public void applyTransforms(MatrixStack stack) {
        stack.translate(-pivot.getX() / 16.0f, -pivot.getY() / 16.0f, -pivot.getZ() / 16.0f);

        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-rot.getX()));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-rot.getY()));
        stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-rot.getZ()));

        stack.translate(pos.getX() / 16.0f, pos.getY() / 16.0f, pos.getZ() / 16.0f);

        stack.translate(pivot.getX() / 16.0f, pivot.getY() / 16.0f, pivot.getZ() / 16.0f);

        stack.scale(scale.getX(), scale.getY(), scale.getZ());
    }

    //Re-builds the mesh data for a custom model part.
    public void rebuild() {
    }

    public void addVertex(Vec3f vert, float u, float v, Vec3f normal) {
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

    public void fromNBT(NbtCompound partTag) {

        //Name
        name = partTag.get("nm").asString();

        if (partTag.contains("pos")) {
            NbtList list = (NbtList) partTag.get("pos");
            pos = new Vec3f(
                    list.getFloat(0),
                    list.getFloat(1),
                    list.getFloat(2)
            );
        }
        if (partTag.contains("rot")) {
            NbtList list = (NbtList) partTag.get("rot");
            rot = new Vec3f(
                    list.getFloat(0),
                    list.getFloat(1),
                    list.getFloat(2)
            );
        }
        if (partTag.contains("scl")) {
            NbtList list = (NbtList) partTag.get("scl");
            scale = new Vec3f(
                    list.getFloat(0),
                    list.getFloat(1),
                    list.getFloat(2)
            );
        }
        if (partTag.contains("piv")) {
            NbtList list = (NbtList) partTag.get("piv");
            pivot = new Vec3f(
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
            NbtList uvOffsetTag = (NbtList) partTag.get("uv");
            uOffset = uvOffsetTag.getFloat(0);
            vOffset = uvOffsetTag.getFloat(1);
        }

        if (partTag.contains("chld")) {
            NbtList childTag = (NbtList) partTag.get("chld");

            for (NbtElement child : childTag) {
                NbtCompound ct = (NbtCompound) child;
                CustomModelPart part = getFromNbtTag(ct);
                part.rebuild();
                children.add(part);
            }
        }
    }

    public void toNBT(NbtCompound partTag) {
        partTag.put("nm", NbtString.of(name));

        if (!pos.equals(new Vec3f(0, 0, 0))) {
            NbtList posTag = new NbtList() {{
                add(NbtFloat.of(pos.getX()));
                add(NbtFloat.of(pos.getY()));
                add(NbtFloat.of(pos.getZ()));
            }};
            partTag.put("pos", posTag);
        }
        if (!rot.equals(new Vec3f(0, 0, 0))) {
            NbtList rotTag = new NbtList() {{
                add(NbtFloat.of(rot.getX()));
                add(NbtFloat.of(rot.getY()));
                add(NbtFloat.of(rot.getZ()));
            }};
            partTag.put("rot", rotTag);
        }
        if (!scale.equals(new Vec3f(1, 1, 1))) {
            NbtList scaleTag = new NbtList() {{
                add(NbtFloat.of(scale.getX()));
                add(NbtFloat.of(scale.getY()));
                add(NbtFloat.of(scale.getZ()));
            }};
            partTag.put("scl", scaleTag);
        }
        if (!pivot.equals(new Vec3f(0, 0, 0))) {
            NbtList pivTag = new NbtList() {{
                add(NbtFloat.of(pivot.getX()));
                add(NbtFloat.of(pivot.getY()));
                add(NbtFloat.of(pivot.getZ()));
            }};
            partTag.put("piv", pivTag);
        }
        
        if(Math.abs(uOffset) > 0.0001f && Math.abs(vOffset) > 0.0001f){
            NbtList uvOffsetTag = new NbtList(){{
                add(NbtFloat.of(uOffset));
                add(NbtFloat.of(vOffset));
            }};
            partTag.put("uv", uvOffsetTag);
        }

        if (parentType != ParentType.None) {
            partTag.put("ptype", NbtString.of(parentType.toString()));
        }

        if (visible == false) {
            partTag.put("vsb", NbtByte.of(false));
        }

        //Parse children.
        if (children.size() > 0) {
            NbtList childrenTag = new NbtList();

            for (CustomModelPart child : children) {
                NbtCompound tag = new NbtCompound();
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
    public static <T extends CustomModelPart> CustomModelPart getFromNbtTag(NbtCompound tag) {

        if (tag.contains("pt") == false)
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
    public static void writeToCompoundTag(NbtCompound tag, CustomModelPart part) {
        String partType = part.getPartType();
        if (!model_part_types.containsKey(partType))
            return;

        tag.put("pt", NbtString.of(partType));
        part.toNBT(tag);
    }
}
