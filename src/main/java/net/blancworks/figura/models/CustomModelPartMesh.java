package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;

import java.util.ArrayList;
import java.util.List;

public class CustomModelPartMesh extends CustomModelPart {
    public NbtCompound meshProperties;

    @Override
    public void rebuild() {
        FloatList vertexData = new FloatArrayList();
        int vertexCount = 0;

        Vec2f texSize = this.texSize;
        if (texSize == null)
            texSize = new Vec2f(meshProperties.getFloat("tw"), meshProperties.getFloat("th"));

        NbtCompound verticesNbt = meshProperties.getCompound("vertices");
        NbtList facesNbt = meshProperties.getList("faces", NbtElement.COMPOUND_TYPE);

        if (facesNbt == null || verticesNbt == null || facesNbt.size() == 0 || verticesNbt.getSize() == 0)
            return;

        for (NbtElement faceData : facesNbt) {
            NbtList vertices = ((NbtCompound) faceData).getList("vertices", NbtElement.STRING_TYPE);
            NbtCompound uvs = ((NbtCompound) faceData).getCompound("uvs");

            if (uvs == null || vertices == null) continue;

            int size = vertices.size();

            if (size > 3) {
                List<Vec3f> vectors = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    vectors.add(vec3fFromNbt(verticesNbt.getList(vertices.getString(i), NbtElement.FLOAT_TYPE)));
                }

                if (testOppositeSides(vectors.get(1), vectors.get(2), vectors.get(0) ,vectors.get(3))) {
                    NbtElement temp = vertices.get(2);
                    vertices.remove(2);
                    vertices.add(0, temp);
                }
                else if (testOppositeSides(vectors.get(0), vectors.get(1), vectors.get(2) ,vectors.get(3))) {
                    NbtElement temp = vertices.get(1);
                    vertices.set(1, vertices.get(2));
                    vertices.set(2, temp);
                }
            }

            for (int i = 0; i < 4; i++) {
                String vertexName = vertices.getString(i % size);

                Vec2f uv = v2fFromNbtList(uvs.getList(vertexName, NbtElement.FLOAT_TYPE));
                Vec3f vertex = vec3fFromNbt(verticesNbt.getList(vertexName, NbtElement.FLOAT_TYPE));

                Vec3f previous = vec3fFromNbt(verticesNbt.getList(vertices.getString((i - 1 + size) % size), NbtElement.FLOAT_TYPE));
                Vec3f next = vec3fFromNbt(verticesNbt.getList(vertices.getString((i + 1) % size), NbtElement.FLOAT_TYPE));

                Vec3f normal = previous.copy();
                normal.subtract(vertex);
                Vec3f normalTwo = next.copy();
                normalTwo.subtract(vertex);

                normal.cross(normalTwo);
                normal.normalize();

                vertex.subtract(this.pivot);
                addVertex(vertex, uv.x / texSize.x, uv.y / texSize.y, normal, vertexData);
            }

            vertexCount += 4;
        }

        this.vertexData = vertexData;
        this.vertexCount = vertexCount;
    }

    @Override
    public void writeNbt(NbtCompound partNbt) {
        super.writeNbt(partNbt);
        partNbt.put("props", meshProperties.copy());
    }

    @Override
    public void readNbt(NbtCompound partNbt) {
        super.readNbt(partNbt);
        this.meshProperties = partNbt.getCompound("props");
    }

    @Override
    public PartType getPartType() {
        return PartType.MESH;
    }

    @Override
    public void applyTrueOffset(Vec3f offset) {
        super.applyTrueOffset(offset);

        pivot.add(offset);
        rebuild();
    }

    private static boolean testOppositeSides(Vec3f linePoint1, Vec3f linePoint2, Vec3f point1, Vec3f point2) {
        linePoint1 = linePoint1.copy();
        linePoint2 = linePoint2.copy();
        point1 = point1.copy();
        point2 = point2.copy();

        linePoint2.subtract(linePoint1);
        point1.subtract(linePoint1);
        point2.subtract(linePoint1);

        Vec3f crossProduct1 = linePoint2.copy();
        crossProduct1.cross(point1);
        linePoint2.cross(point2);

        return crossProduct1.dot(linePoint2) < 0;
    }
}
