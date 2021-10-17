package net.blancworks.figura.models;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;

import java.util.Set;


public class CustomModelPartMesh extends CustomModelPart {
    public NbtCompound meshProperties;

    @Override
    public void rebuild() {
        FloatList vertexData = new FloatArrayList();
        int vertexCount = 0;

        Vec2f texSize = this.texSize;
        if (texSize == null)
            texSize = new Vec2f(meshProperties.getFloat("tw"), meshProperties.getFloat("th"));

        NbtCompound verticesNbt = (NbtCompound) meshProperties.get("vertices");
        NbtCompound facesNbt = (NbtCompound) meshProperties.get("faces");

        if (facesNbt == null)
            return;

        Set<String> faces = facesNbt.getKeys();

        for (String faceName : faces) {
            NbtCompound face = facesNbt.getCompound(faceName);

            NbtList verticesList = face.getList("vertices", NbtElement.STRING_TYPE);
            NbtCompound uvList = face.getCompound("uvs");

            int size = verticesList.size();
            for (int i = size - 1; i >= 0; i--) {
                String vertexName = verticesList.getString(i);

                Vec2f uv = v2fFromNbtList((NbtList) uvList.get(vertexName));
                Vec3f vertex = vec3fFromNbt((NbtList) verticesNbt.get(vertexName));

                Vec3f previous = vec3fFromNbt((NbtList) verticesNbt.get(verticesList.getString((i - 1 + size) % size)));
                Vec3f next = vec3fFromNbt((NbtList) verticesNbt.get(verticesList.getString((i + 1) % size)));

                //normal = (previousVertex - currentVertex).cross(nextVertex - currentVertex)
                Vec3f normal = previous.copy();
                normal.subtract(vertex);

                Vec3f normalTwo = next.copy();
                normalTwo.subtract(vertex);

                normal.cross(normalTwo);
                normal.normalize();

                addVertex(vertex, uv.x / texSize.x, uv.y / texSize.y, normal, vertexData);
            }
            vertexCount += size;
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

        //TODO: WIP
        /*NbtCompound geometryData = (NbtCompound) partNbt.get("geo");
        NbtCompound vertexData = (NbtCompound) geometryData.get("vertices");

        ArrayList<Vec3f> vertexList = new ArrayList<>();
        vertexData.getKeys().forEach(key -> {
            NbtList curVertex = vertexData.getList(key, NbtElement.LIST_TYPE);
            float x = curVertex.getFloat(0);
            float y = curVertex.getFloat(1);
            float z = curVertex.getFloat(2);
            vertexList.add(new Vec3f(x, y, z));
        });*/
    }

    public PartType getPartType() {
        return PartType.MESH;
    }
}
