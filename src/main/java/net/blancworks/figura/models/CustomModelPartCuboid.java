package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.List;

public class CustomModelPartCuboid extends CustomModelPart {

    //Used to store the data for a cuboid, so that we can re-build it later if need be.
    public CompoundTag cuboidProperties = new CompoundTag();

    @Override
    public void rebuild() {
        FloatList vertexData = new FloatArrayList();
        int vertexCount = 0;

        float inflate = 0;
        if (cuboidProperties.contains("inf")) inflate = cuboidProperties.getFloat("inf");

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

        Vec2f texSize = this.texSize;
        if (texSize == null)
            texSize = new Vec2f(cuboidProperties.getFloat("tw"), cuboidProperties.getFloat("th"));

        //North
        if (cuboidProperties.contains("n")) {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("n");

            if (faceData.contains("texture")) {
                Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

                uvData data = UVCustomizations.get(UV.NORTH);
                if (data == null) {
                    data = new uvData();
                    data.uvOffset = new Vec2f(v.getX(), v.getY());
                    data.uvSize = new Vec2f(v.getZ(), v.getW());

                    UVCustomizations.put(UV.NORTH, data);
                } else {
                    v.set(data.uvOffset.x, data.uvOffset.y, v.getZ(), v.getW());
                    v.set(v.getX(), v.getY(), data.uvSize.x, data.uvSize.y);
                }

                float rotation = 0;

                if (faceData.contains("rotation")) {
                    rotation = ((FloatTag) faceData.get("rotation")).getFloat();
                }

                List<Vec2f> cornerUVs = rotateUV(v, rotation);

                generateFace(
                        new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                        new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                        new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                        new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                        cornerUVs,
                        texSize.x, texSize.y,
                        vertexData
                );
                vertexCount += 4;
            }
        }

        //South
        if (cuboidProperties.contains("s")) {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("s");

            if (faceData.contains("texture")) {
                Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

                uvData data = UVCustomizations.get(UV.SOUTH);
                if (data == null) {
                    data = new uvData();
                    data.uvOffset = new Vec2f(v.getX(), v.getY());
                    data.uvSize = new Vec2f(v.getZ(), v.getW());

                    UVCustomizations.put(UV.SOUTH, data);
                } else {
                    v.set(data.uvOffset.x, data.uvOffset.y, v.getZ(), v.getW());
                    v.set(v.getX(), v.getY(), data.uvSize.x, data.uvSize.y);
                }

                float rotation = 0;

                if (faceData.contains("rotation")) {
                    rotation = ((FloatTag) faceData.get("rotation")).getFloat();
                }

                List<Vec2f> cornerUVs = rotateUV(v, rotation);

                generateFace(
                        new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                        new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                        new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                        new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                        cornerUVs,
                        texSize.x, texSize.y,
                        vertexData
                );
                vertexCount += 4;
            }
        }

        //East
        if (cuboidProperties.contains("e")) {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("e");

            if (faceData.contains("texture")) {
                Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

                uvData data = UVCustomizations.get(UV.EAST);
                if (data == null) {
                    data = new uvData();
                    data.uvOffset = new Vec2f(v.getX(), v.getY());
                    data.uvSize = new Vec2f(v.getZ(), v.getW());

                    UVCustomizations.put(UV.EAST, data);
                } else {
                    v.set(data.uvOffset.x, data.uvOffset.y, v.getZ(), v.getW());
                    v.set(v.getX(), v.getY(), data.uvSize.x, data.uvSize.y);
                }

                float rotation = 0;

                if (faceData.contains("rotation")) {
                    rotation = ((FloatTag) faceData.get("rotation")).getFloat();
                }

                List<Vec2f> cornerUVs = rotateUV(v, rotation);

                generateFace(
                        new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                        new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                        new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                        new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                        cornerUVs,
                        texSize.x, texSize.y,
                        vertexData
                );
                vertexCount += 4;
            }
        }

        //West
        if (cuboidProperties.contains("w")) {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("w");

            if (faceData.contains("texture")) {
                Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

                uvData data = UVCustomizations.get(UV.WEST);
                if (data == null) {
                    data = new uvData();
                    data.uvOffset = new Vec2f(v.getX(), v.getY());
                    data.uvSize = new Vec2f(v.getZ(), v.getW());

                    UVCustomizations.put(UV.WEST, data);
                } else {
                    v.set(data.uvOffset.x, data.uvOffset.y, v.getZ(), v.getW());
                    v.set(v.getX(), v.getY(), data.uvSize.x, data.uvSize.y);
                }

                float rotation = 0;

                if (faceData.contains("rotation")) {
                    rotation = ((FloatTag) faceData.get("rotation")).getFloat();
                }

                List<Vec2f> cornerUVs = rotateUV(v, rotation);

                generateFace(
                        new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                        new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                        new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                        new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                        cornerUVs,
                        texSize.x, texSize.y,
                        vertexData
                );
                vertexCount += 4;
            }
        }

        //Top
        if (cuboidProperties.contains("u")) {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("u");

            if (faceData.contains("texture")) {
                Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

                uvData data = UVCustomizations.get(UV.UP);
                if (data == null) {
                    data = new uvData();
                    data.uvOffset = new Vec2f(v.getX(), v.getY());
                    data.uvSize = new Vec2f(v.getZ(), v.getW());

                    UVCustomizations.put(UV.UP, data);
                } else {
                    v.set(data.uvOffset.x, data.uvOffset.y, v.getZ(), v.getW());
                    v.set(v.getX(), v.getY(), data.uvSize.x, data.uvSize.y);
                }

                float rotation = 0;

                if (faceData.contains("rotation")) {
                    rotation = ((FloatTag) faceData.get("rotation")).getFloat();
                }

                List<Vec2f> cornerUVs = rotateUV(v, rotation);

                generateFace(
                        new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                        new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                        new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                        new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                        cornerUVs,
                        texSize.x, texSize.y,
                        vertexData
                );
                vertexCount += 4;
            }
        }

        //Bottom
        if (cuboidProperties.contains("d")) {
            CompoundTag faceData = (CompoundTag) cuboidProperties.get("d");

            if (faceData.contains("texture")) {
                Vector4f v = v4fFromNbtList((ListTag) faceData.get("uv"));

                uvData data = UVCustomizations.get(UV.DOWN);
                if (data == null) {
                    data = new uvData();
                    data.uvOffset = new Vec2f(v.getX(), v.getY());
                    data.uvSize = new Vec2f(v.getZ(), v.getW());

                    UVCustomizations.put(UV.DOWN, data);
                } else {
                    v.set(data.uvOffset.x, data.uvOffset.y, v.getZ(), v.getW());
                    v.set(v.getX(), v.getY(), data.uvSize.x, data.uvSize.y);
                }

                float rotation = 0;

                if (faceData.contains("rotation")) {
                    rotation = ((FloatTag) faceData.get("rotation")).getFloat();
                }

                List<Vec2f> cornerUVs = rotateUV(v, rotation);

                generateFace(
                        new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                        new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                        new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                        new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                        cornerUVs,
                        texSize.x, texSize.y,
                        vertexData
                );
                vertexCount += 4;
            }
        }

        this.vertexData = vertexData;
        this.vertexCount = vertexCount;
    }

    @Override
    public void applyTrueOffset(Vector3f offset) {
        super.applyTrueOffset(offset);

        Vector3f from = v3fFromNbtList((ListTag) cuboidProperties.get("f"));
        Vector3f to = v3fFromNbtList((ListTag) cuboidProperties.get("t"));
        
        from.add(offset);
        to.add(offset);

        cuboidProperties.put("f", new ListTag() {{
            add(FloatTag.of(from.getX()));
            add(FloatTag.of(from.getY()));
            add(FloatTag.of(from.getZ()));
        }});

        cuboidProperties.put("t", new ListTag() {{
            add(FloatTag.of(to.getX()));
            add(FloatTag.of(to.getY()));
            add(FloatTag.of(to.getZ()));
        }});
        
        pivot.add(offset);
        
        rebuild();
    }

    public void generateFace(Vector3f a, Vector3f b, Vector3f c, Vector3f d, List<Vec2f> uv, float texWidth, float texHeight, FloatList vertexData) {
        Vector3f nA = b.copy();
        nA.subtract(a);
        Vector3f nB = c.copy();
        nB.subtract(a);
        nA.cross(nB);
        nA.normalize();

        addVertex(b, uv.get(0).x / texWidth, uv.get(0).y / texHeight, nA, vertexData);
        addVertex(a, uv.get(1).x / texWidth, uv.get(1).y / texHeight, nA, vertexData);
        addVertex(d, uv.get(2).x / texWidth, uv.get(2).y / texHeight, nA, vertexData);
        addVertex(c, uv.get(3).x / texWidth, uv.get(3).y / texHeight, nA, vertexData);
    }

    @Override
    public void readNbt(CompoundTag partNbt) {
        super.readNbt(partNbt);
        this.cuboidProperties = (CompoundTag) partNbt.get("props");
    }

    @Override
    public void writeNbt(CompoundTag partNbt) {
        super.writeNbt(partNbt);
        partNbt.put("props", this.cuboidProperties);
    }

    @Override
    public PartType getPartType() {
        return PartType.CUBE;
    }

    public Vector3f v3fFromNbtList(ListTag list) {
        return new Vector3f(list.getFloat(0), list.getFloat(1), list.getFloat(2));
    }

    public Vector4f v4fFromNbtList(ListTag list) {
        return new Vector4f(list.getFloat(0), list.getFloat(1), list.getFloat(2), list.getFloat(3));
    }

    public static List<Vec2f> rotateUV(Vector4f v, float rotation) {
        List<Vec2f> cornerUVs = new ArrayList<>();
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
