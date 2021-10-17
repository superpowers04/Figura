package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;


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

        if (facesNbt == null || facesNbt.size() == 0 || verticesNbt == null || verticesNbt.getSize() == 0)
            return;

        for (NbtElement faceData : facesNbt) {
            NbtList vertices = ((NbtCompound) faceData).getList("vertices", NbtElement.STRING_TYPE);
            NbtCompound uvs = ((NbtCompound) faceData).getCompound("uvs");

            if (uvs == null) continue;

            int size = vertices.size();
            for (int i = 0; i <= size; i++) {
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

                addVertex(vertex, uv.x / texSize.x, uv.y / texSize.y, normal, vertexData);
            }

            vertexCount += size + 1;
        }

        this.vertexData = vertexData;
        this.vertexCount = vertexCount;
    }

    @Override
    public void writeNbt(NbtCompound partNbt) {
        super.writeNbt(partNbt);
        partNbt.put("geo", meshProperties.copy());
    }

    @Override
    public void readNbt(NbtCompound partNbt) {
        super.readNbt(partNbt);
        this.meshProperties = partNbt.getCompound("geo");
    }

    public PartType getPartType() {
        return PartType.MESH;
    }
}
