package net.blancworks.figura.models;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ObjReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;


public class CustomModelPartMesh extends CustomModelPart {
    public boolean isReady = false;
    
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

    @Override
    public void writeNbt(CompoundTag partNbt) {
        super.writeNbt(partNbt);
        ListTag geometryData = new ListTag();

        for (int i = 0; i < this.vertexData.size(); i++) {
            geometryData.add(FloatTag.of(this.vertexData.getFloat(i)));
        }
        partNbt.put("vc", IntTag.of(this.vertexCount));
        partNbt.put("geo", geometryData);
    }

    @Override
    public void readNbt(CompoundTag partNbt) {
        super.readNbt(partNbt);
        ListTag geometryData = (ListTag) partNbt.get("geo");

        for (int i = 0; i < geometryData.size(); i++) {
            this.vertexData.add(geometryData.getFloat(i));
        }
        this.vertexCount = partNbt.getInt("vc");
    }

    public String getPartType() {
        return "msh";
    }
}
