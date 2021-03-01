package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

import java.util.ArrayList;
import java.util.Random;

public class CustomModelPart {

    public String name;

    //Transform data
    public Vector3f pivot = new Vector3f();
    public Vector3f pos = new Vector3f();
    public Vector3f rot = new Vector3f();
    public Vector3f scale = new Vector3f(1,1,1);

    //Offsets
    public float uOffset;
    public float vOffset;

    public boolean visible;

    public ParentType parentType;

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
                    1, 1, 1, 1,
                    u, v,
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
        stack.translate(pos.getX(), pos.getY(), pos.getZ());

        stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(rot.getX()));
        stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(rot.getY()));
        stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));

        stack.scale(scale.getX(), scale.getY(), scale.getZ());
    }

    public void addVertex(Vector3f vert, float u, float v, Vector3f normal) {
        vertexData.add(vert.getX());
        vertexData.add(vert.getY());
        vertexData.add(vert.getZ());
        vertexData.add(u);
        vertexData.add(v);
        vertexData.add(normal.getX());
        vertexData.add(normal.getY());
        vertexData.add(normal.getZ());
    }

    public void fromNBT(CompoundTag partTag) {

    }

    public void toNBT(CompoundTag partTag) {

    }

    public enum ParentType {
        None,
        Custom,
        Head,
        LeftArm,
        RightArm,
        LeftLeg,
        RightLeg,
        Torso
    }
}
