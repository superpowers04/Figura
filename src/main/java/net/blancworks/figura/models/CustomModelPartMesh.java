package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.List;

public class CustomModelPartMesh extends CustomModelPart {
    public CompoundTag meshProperties;

    @Override
    public void rebuild(Vec2f newTexSize) {
        if (newTexSize == null)
            newTexSize = new Vec2f(meshProperties.getFloat("tw"), meshProperties.getFloat("th"));

        super.rebuild(newTexSize);

        FloatList vertexData = new FloatArrayList();
        int vertexCount = 0;

        CompoundTag verticesNbt = meshProperties.getCompound("vertices");
        ListTag facesNbt = meshProperties.getList("faces", NbtType.LIST);

        if (facesNbt == null || verticesNbt == null || facesNbt.size() == 0 || verticesNbt.getSize() == 0)
            return;

        for (Tag faceElement : facesNbt) {
            ListTag faceData = (ListTag) faceElement;
            int size = faceData.size();

            if (size > 3) {
                List<Vector3f> vectors = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    vectors.add(vec3fFromNbt(verticesNbt.getList(faceData.getCompound(i).getString("id"), NbtType.FLOAT)));
                }

                if (testOppositeSides(vectors.get(1), vectors.get(2), vectors.get(0) ,vectors.get(3))) {
                    Tag temp = faceData.get(2);
                    faceData.remove(2);
                    faceData.add(0, temp);
                }
                else if (testOppositeSides(vectors.get(0), vectors.get(1), vectors.get(2) ,vectors.get(3))) {
                    Tag temp = faceData.get(1);
                    faceData.set(1, faceData.get(2));
                    faceData.set(2, temp);
                }
            }

            for (int i = 0; i < 4; i++) {
                CompoundTag vertexNbt = faceData.getCompound(i % size);
                String vertexName = vertexNbt.getString("id");

                Vec2f uv = v2fFromNbtList(vertexNbt.getList("uv", NbtType.FLOAT));
                Vector3f vertex = vec3fFromNbt(verticesNbt.getList(vertexName, NbtType.FLOAT));

                Vector3f previous = vec3fFromNbt(verticesNbt.getList(faceData.getCompound((i - 1 + size) % size).getString("id"), NbtType.FLOAT));
                Vector3f next = vec3fFromNbt(verticesNbt.getList(faceData.getCompound((i + 1) % size).getString("id"), NbtType.FLOAT));

                Vector3f normal = previous.copy();
                normal.subtract(vertex);
                Vector3f normalTwo = next.copy();
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
    public void rotate(MatrixStack stack, Vector3f rot) {
        stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-rot.getX()));
        stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-rot.getY()));
        stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
    }

    @Override
    public void vanillaRotate(MatrixStack stack, Vector3f rot) {
        stack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(rot.getX()));
        stack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(rot.getY()));
        stack.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
    }

    @Override
    public void writeNbt(CompoundTag partNbt) {
        super.writeNbt(partNbt);
        partNbt.put("props", meshProperties.copy());
    }

    @Override
    public void readNbt(CompoundTag partNbt) {
        super.readNbt(partNbt);
        this.meshProperties = partNbt.getCompound("props");
    }

    @Override
    public PartType getPartType() {
        return PartType.MESH;
    }

    @Override
    public void applyTrueOffset(Vector3f offset) {
        super.applyTrueOffset(offset);

        pivot.add(offset);
        rebuild(this.texSize);
    }

    private static boolean testOppositeSides(Vector3f linePoint1, Vector3f linePoint2, Vector3f point1, Vector3f point2) {
        linePoint1 = linePoint1.copy();
        linePoint2 = linePoint2.copy();
        point1 = point1.copy();
        point2 = point2.copy();

        linePoint2.subtract(linePoint1);
        point1.subtract(linePoint1);
        point2.subtract(linePoint1);

        Vector3f crossProduct1 = linePoint2.copy();
        crossProduct1.cross(point1);
        linePoint2.cross(point2);

        return crossProduct1.dot(linePoint2) < 0;
    }
}
