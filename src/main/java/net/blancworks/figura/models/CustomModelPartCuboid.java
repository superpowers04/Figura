package net.blancworks.figura.models;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.system.MathUtil;

import java.util.ArrayList;

public class CustomModelPartCuboid extends CustomModelPart {

    //Used to store the data for a cuboid, so that we can re-build it later if need be.
    public CompoundTag cuboidProperties = new CompoundTag();

    public float inflate = 0;

    @Override
    public void rebuild() {

        vertexData.clear();
        vertexCount = 0;

        Vector3f from = v3fFromNbtList((ListTag) cuboidProperties.get("f"));
        Vector3f to = v3fFromNbtList((ListTag) cuboidProperties.get("t"));
        Vector3f mid = new Vector3f(
                MathHelper.lerp(0.5f, from.getX(), to.getX()),
                MathHelper.lerp(0.5f, from.getY(), to.getY()),
                MathHelper.lerp(0.5f, from.getZ(), to.getZ())
        );

        from.subtract(mid);
        from.add(-inflate, -inflate, -inflate);
        from.add(mid);

        to.subtract(mid);
        to.add(inflate, inflate, inflate);
        to.add(mid);

        float texWidth = cuboidProperties.getFloat("tw");
        float texHeight = cuboidProperties.getFloat("th");

        //North
        {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("n");
            Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((FloatTag) faceData.get("rotation")).getFloat();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //South
        {

            CompoundTag faceData = (CompoundTag) cuboidProperties.get("s");
            Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((FloatTag) faceData.get("rotation")).getFloat();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //East
        {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("e");
            Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((FloatTag) faceData.get("rotation")).getFloat();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //West
        {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("w");
            Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((FloatTag) faceData.get("rotation")).getFloat();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //Top
        {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("u");
            Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((FloatTag) faceData.get("rotation")).getFloat();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //Bottom
        {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("d");
            Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((FloatTag) faceData.get("rotation")).getFloat();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }
    }

    public void generateFace(Vector3f a, Vector3f b, Vector3f c, Vector3f d, ArrayList<Vec2f> uv, float texWidth, float texHeight) {
        Vector3f nA = b.copy();
        nA.subtract(a);
        Vector3f nB = c.copy();
        nB.subtract(a);
        nA.cross(nB);
        nA.normalize();

        a.scale(1);
        b.scale(1);
        c.scale(1);
        d.scale(1);

        addVertex(b, uv.get(0).x / texWidth, uv.get(0).y / texHeight, nA);
        addVertex(a, uv.get(1).x / texWidth, uv.get(1).y / texHeight, nA);
        addVertex(d, uv.get(2).x / texWidth, uv.get(2).y / texHeight, nA);
        addVertex(c, uv.get(3).x / texWidth, uv.get(3).y / texHeight, nA);
    }


    @Override
    public void fromNBT(CompoundTag partTag) {
        super.fromNBT(partTag);
        cuboidProperties = (CompoundTag) partTag.get("props");
    }


    @Override
    public void toNBT(CompoundTag partTag) {
        super.toNBT(partTag);
        partTag.put("props", cuboidProperties);
    }

    @Override
    public String getPartType() {
        return "cub";
    }

    public Vector3f v3fFromNbtList(ListTag list) {
        return new Vector3f(list.getFloat(0), list.getFloat(1), list.getFloat(2));
    }

    public Vector4f v4fFromNbtList(ListTag list) {
        return new Vector4f(list.getFloat(0), list.getFloat(1), list.getFloat(2), list.getFloat(3));
    }

    public static ArrayList<Vec2f> rotateUV(Vector4f v, float rotation) {

        ArrayList<Vec2f> cornerUVs = new ArrayList<Vec2f>();
        cornerUVs.add(new Vec2f(v.getX(), v.getW())); //0,1
        cornerUVs.add(new Vec2f(v.getZ(), v.getW())); //1,1
        cornerUVs.add(new Vec2f(v.getZ(), v.getY())); //1,0
        cornerUVs.add(new Vec2f(v.getX(), v.getY())); //0,0

        int rotationAmount = Math.round(rotation / 90.0f);

        for (int i = 0; i < rotationAmount; i++) {
            Vec2f last = cornerUVs.get(0);
            cornerUVs.remove(0);
            cornerUVs.add(last);
        }

        return cornerUVs;
    }

    public static Vec2f rotateVec2f(Vec2f v, float degrees) {
        float sin = (float) Math.sin(Math.toRadians(degrees));
        float cos = (float) Math.cos(Math.toRadians(degrees));

        float tx = v.x;
        float ty = v.y;
        v = new Vec2f(
                (cos * tx) - (sin * ty),
                (sin * tx) + (cos * ty)
        );
        return v;
    }
}
