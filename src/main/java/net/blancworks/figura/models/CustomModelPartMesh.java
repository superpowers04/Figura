package net.blancworks.figura.models;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ObjReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec3f;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;


public class CustomModelPartMesh extends CustomModelPart {
    public boolean isReady = false;
    public NbtCompound meshProperties;

    /*
    public static CustomModelPartMesh loadFromObj(Path path) {
        CustomModelPartMesh newPart = new CustomModelPartMesh();

        MinecraftClient.getInstance().execute(() -> {
            try {
                newPart.parseObj(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return newPart;
    }

    public void parseObj(Path path) throws Exception {
        this.isReady = false;
        vertexData.clear();

        InputStream fileStream = new FileInputStream(path.toString());
        Obj objectFile = ObjReader.read(fileStream);
        fileStream.close();

        int triCount = objectFile.getNumFaces();

        for (int i = 0; i < triCount; i++) {
            ObjFace face = objectFile.getFace(i);
            int vertCount = face.getNumVertices();

            if (vertCount == 4) {
                for (int j = vertCount - 1; j >= 0; j--) {
                    FloatTuple vertex = objectFile.getVertex(face.getVertexIndex(j));
                    vertexData.add(vertex.getX());
                    vertexData.add(-vertex.getY());
                    vertexData.add(vertex.getZ());

                    FloatTuple uv = objectFile.getTexCoord(face.getTexCoordIndex(j));
                    vertexData.add(uv.getX());
                    vertexData.add(1 - uv.getY());

                    FloatTuple normal = objectFile.getNormal(face.getNormalIndex(j));
                    vertexData.add(normal.getX());
                    vertexData.add(-normal.getY());
                    vertexData.add(normal.getZ());

                    vertexCount++;
                }
            } else if (vertCount == 3) {
                for (int j = vertCount - 1; j >= 0; j--) {
                    FloatTuple vertex = objectFile.getVertex(face.getVertexIndex(j));
                    vertexData.add(vertex.getX());
                    vertexData.add(-vertex.getY());
                    vertexData.add(vertex.getZ());

                    FloatTuple uv = objectFile.getTexCoord(face.getTexCoordIndex(j));
                    vertexData.add(uv.getX());
                    vertexData.add(1 - uv.getY());

                    FloatTuple normal = objectFile.getNormal(face.getNormalIndex(j));
                    vertexData.add(normal.getX());
                    vertexData.add(-normal.getY());
                    vertexData.add(normal.getZ());

                    if (j == vertCount - 1) {
                        vertex = objectFile.getVertex(face.getVertexIndex(j));
                        vertexData.add(vertex.getX());
                        vertexData.add(-vertex.getY());
                        vertexData.add(vertex.getZ());

                        uv = objectFile.getTexCoord(face.getTexCoordIndex(j));
                        vertexData.add(uv.getX());
                        vertexData.add(1 - uv.getY());

                        normal = objectFile.getNormal(face.getNormalIndex(j));
                        vertexData.add(normal.getX());
                        vertexData.add(-normal.getY());
                        vertexData.add(normal.getZ());
                    }
                    
                    vertexCount++;
                }
            }

        }

        this.isReady = true;
    }
    */
    @Override
    public void writeNbt(NbtCompound partNbt) {
        super.writeNbt(partNbt);
        /*
        NbtList geometryData = new NbtList();

        for (int i = 0; i < this.vertexData.size(); i++) {
            geometryData.add(NbtFloat.of(this.vertexData.getFloat(i)));
        }
        partNbt.put("vc", NbtInt.of(this.vertexCount));
        partNbt.put("geo", geometryData);
         */
        partNbt.put("geo", meshProperties.copy());
    }

    @Override
    public void readNbt(NbtCompound partNbt) {
        super.readNbt(partNbt);
        NbtCompound geometryData = (NbtCompound) partNbt.get("geo");
        NbtCompound vertexData = (NbtCompound) geometryData.get("vertices");

        ArrayList<Vec3f> vertexList = new ArrayList<>();
        vertexData.getKeys().forEach(key -> {
            NbtList curVertex = vertexData.getList(key, NbtElement.LIST_TYPE);
            float x = curVertex.getFloat(0);
            float y = curVertex.getFloat(1);
            float z = curVertex.getFloat(2);
            vertexList.add(new Vec3f(x, y, z));
        });

    }

    public PartType getPartType() {
        return PartType.MESH;
    }
}
