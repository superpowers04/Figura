package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.client.util.math.MatrixStack;
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
    public void rebuild(Vec2f newTexSize) {
        if (newTexSize == null)
            newTexSize = new Vec2f(meshProperties.getFloat("tw"), meshProperties.getFloat("th"));

        super.rebuild(newTexSize);

        FloatList vertexData = new FloatArrayList();
        int vertexCount = 0;

        NbtCompound verticesNbt = meshProperties.getCompound("vertices");
        NbtList facesNbt = meshProperties.getList("faces", NbtElement.LIST_TYPE);

        if (facesNbt == null || verticesNbt == null || facesNbt.size() == 0 || verticesNbt.getSize() == 0)
            return;

        for (NbtElement faceElement : facesNbt) {
            NbtList faceData = (NbtList) faceElement;
            int size = faceData.size();

            if (size > 3) {
                List<Vec3f> vectors = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    vectors.add(vec3fFromNbt(verticesNbt.getList(faceData.getCompound(i).getString("id"), NbtElement.FLOAT_TYPE)));
                }

                if (testOppositeSides(vectors.get(1), vectors.get(2), vectors.get(0) ,vectors.get(3))) {
                    NbtElement temp = faceData.get(2);
                    faceData.remove(2);
                    faceData.add(0, temp);
                }
                else if (testOppositeSides(vectors.get(0), vectors.get(1), vectors.get(2) ,vectors.get(3))) {
                    NbtElement temp = faceData.get(1);
                    faceData.set(1, faceData.get(2));
                    faceData.set(2, temp);
                }
            }

            for (int i = 0; i < 4; i++) {
                NbtCompound vertexNbt = faceData.getCompound(i % size);
                String vertexName = vertexNbt.getString("id");

                Vec2f uv = v2fFromNbtList(vertexNbt.getList("uv", NbtElement.FLOAT_TYPE));
                Vec3f vertex = vec3fFromNbt(verticesNbt.getList(vertexName, NbtElement.FLOAT_TYPE));

                Vec3f previous = vec3fFromNbt(verticesNbt.getList(faceData.getCompound((i - 1 + size) % size).getString("id"), NbtElement.FLOAT_TYPE));
                Vec3f next = vec3fFromNbt(verticesNbt.getList(faceData.getCompound((i + 1) % size).getString("id"), NbtElement.FLOAT_TYPE));

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
    public void rotate(MatrixStack stack, Vec3f rot) {
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-rot.getX()));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-rot.getY()));
        stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
    }

    @Override
    public void vanillaRotate(MatrixStack stack, Vec3f rot) {
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rot.getX()));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rot.getY()));
        stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
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
