package net.blancworks.figura.models;

import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;

public class CustomModelPartCuboid extends CustomModelPart {

    //Used to store the data for a cuboid, so that we can re-build it later if need be.
    public NbtCompound cuboidProperties = new NbtCompound();
    
    @Override
    public void rebuild() {

        vertexData.clear();
        vertexCount = 0;

        float inflate = 0;
        if(cuboidProperties.contains("inf")) inflate = cuboidProperties.getFloat("inf");

        Vec3f from = v3fFromNbtList((NbtList) cuboidProperties.get("f"));
        Vec3f to = v3fFromNbtList((NbtList) cuboidProperties.get("t"));
        Vec3f mid = new Vec3f(
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
            NbtCompound faceData = (NbtCompound) cuboidProperties.get("n");
            Vector4f v = v4fFromNbtList((NbtList) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((NbtFloat) faceData.get("rotation")).floatValue();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vec3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vec3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vec3f(-to.getX(), -to.getY(), from.getZ()),
                    new Vec3f(-from.getX(), -to.getY(), from.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //South
        {

            NbtCompound faceData = (NbtCompound) cuboidProperties.get("s");
            Vector4f v = v4fFromNbtList((NbtList) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((NbtFloat) faceData.get("rotation")).floatValue();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vec3f(-to.getX(), -from.getY(), to.getZ()),
                    new Vec3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vec3f(-from.getX(), -to.getY(), to.getZ()),
                    new Vec3f(-to.getX(), -to.getY(), to.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //East
        {
            NbtCompound faceData = (NbtCompound) cuboidProperties.get("e");
            Vector4f v = v4fFromNbtList((NbtList) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((NbtFloat) faceData.get("rotation")).floatValue();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vec3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vec3f(-to.getX(), -from.getY(), to.getZ()),
                    new Vec3f(-to.getX(), -to.getY(), to.getZ()),
                    new Vec3f(-to.getX(), -to.getY(), from.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //West
        {
            NbtCompound faceData = (NbtCompound) cuboidProperties.get("w");
            Vector4f v = v4fFromNbtList((NbtList) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((NbtFloat) faceData.get("rotation")).floatValue();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vec3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vec3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vec3f(-from.getX(), -to.getY(), from.getZ()),
                    new Vec3f(-from.getX(), -to.getY(), to.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //Top
        {
            NbtCompound faceData = (NbtCompound) cuboidProperties.get("u");
            Vector4f v = v4fFromNbtList((NbtList) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((NbtFloat) faceData.get("rotation")).floatValue();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vec3f(-to.getX(), -to.getY(), to.getZ()),
                    new Vec3f(-from.getX(), -to.getY(), to.getZ()),
                    new Vec3f(-from.getX(), -to.getY(), from.getZ()),
                    new Vec3f(-to.getX(), -to.getY(), from.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }

        //Bottom
        {
            NbtCompound faceData = (NbtCompound) cuboidProperties.get("d");
            Vector4f v = v4fFromNbtList((NbtList) faceData.get("uv"));

            float rotation = 0;

            if (faceData.contains("rotation")) {
                rotation = ((NbtFloat) faceData.get("rotation")).floatValue();
            }

            ArrayList<Vec2f> cornerUVs = rotateUV(v, rotation);

            generateFace(
                    new Vec3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vec3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vec3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vec3f(-to.getX(), -from.getY(), to.getZ()),
                    cornerUVs,
                    texWidth, texHeight
            );
        }
    }

    public void generateFace(Vec3f a, Vec3f b, Vec3f c, Vec3f d, ArrayList<Vec2f> uv, float texWidth, float texHeight) {
        Vec3f nA = b.copy();
        nA.subtract(a);
        Vec3f nB = c.copy();
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
    public void fromNBT(NbtCompound partTag) {
        super.fromNBT(partTag);
        cuboidProperties = (NbtCompound) partTag.get("props");
    }


    @Override
    public void toNBT(NbtCompound partTag) {
        super.toNBT(partTag);
        partTag.put("props", cuboidProperties);
    }

    @Override
    public String getPartType() {
        return "cub";
    }

    public Vec3f v3fFromNbtList(NbtList list) {
        return new Vec3f(list.getFloat(0), list.getFloat(1), list.getFloat(2));
    }

    public Vector4f v4fFromNbtList(NbtList list) {
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
