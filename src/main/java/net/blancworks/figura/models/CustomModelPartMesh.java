package net.blancworks.figura.models;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ObjReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;


public class CustomModelPartMesh extends CustomModelPart {

    public boolean is_ready = false;
    
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
        is_ready = false;
        vertexData.clear();

        InputStream fileStream = new FileInputStream(path.toString());
        Obj object_file = ObjReader.read(fileStream);
        fileStream.close();

        int triCount = object_file.getNumFaces();

        for (int i = 0; i < triCount; i++) {
            ObjFace face = object_file.getFace(i);
            int vertCount = face.getNumVertices();

            if (vertCount == 4) {
                for (int j = vertCount - 1; j >= 0; j--) {
                    FloatTuple vertex = object_file.getVertex(face.getVertexIndex(j));
                    vertexData.add(vertex.getX());
                    vertexData.add(-vertex.getY());
                    vertexData.add(vertex.getZ());

                    FloatTuple uv = object_file.getTexCoord(face.getTexCoordIndex(j));
                    vertexData.add(uv.getX());
                    vertexData.add(1 - uv.getY());

                    FloatTuple normal = object_file.getNormal(face.getNormalIndex(j));
                    vertexData.add(normal.getX());
                    vertexData.add(-normal.getY());
                    vertexData.add(normal.getZ());

                    vertexCount++;
                }
            } else if (vertCount == 3) {
                for (int j = vertCount - 1; j >= 0; j--) {
                    FloatTuple vertex = object_file.getVertex(face.getVertexIndex(j));
                    vertexData.add(vertex.getX());
                    vertexData.add(-vertex.getY());
                    vertexData.add(vertex.getZ());

                    FloatTuple uv = object_file.getTexCoord(face.getTexCoordIndex(j));
                    vertexData.add(uv.getX());
                    vertexData.add(1 - uv.getY());

                    FloatTuple normal = object_file.getNormal(face.getNormalIndex(j));
                    vertexData.add(normal.getX());
                    vertexData.add(-normal.getY());
                    vertexData.add(normal.getZ());

                    if (j == vertCount - 1) {
                        vertex = object_file.getVertex(face.getVertexIndex(j));
                        vertexData.add(vertex.getX());
                        vertexData.add(-vertex.getY());
                        vertexData.add(vertex.getZ());

                        uv = object_file.getTexCoord(face.getTexCoordIndex(j));
                        vertexData.add(uv.getX());
                        vertexData.add(1 - uv.getY());

                        normal = object_file.getNormal(face.getNormalIndex(j));
                        vertexData.add(normal.getX());
                        vertexData.add(-normal.getY());
                        vertexData.add(normal.getZ());
                    }
                    
                    vertexCount++;
                }
            }

        }
        
        is_ready = true;
    }

    @Override
    public void toNBT(NbtCompound tag) {
        super.toNBT(tag);
        NbtList geometryData = new NbtList();

        for (int i = 0; i < vertexData.size(); i++) {
            geometryData.add(NbtFloat.of(vertexData.getFloat(i)));
        }
        tag.put("vc", NbtInt.of(vertexCount));
        tag.put("geo", geometryData);
    }

    @Override
    public void fromNBT(NbtCompound tag) {
        super.fromNBT(tag);
        NbtList geometryData = (NbtList) tag.get("geo");

        for (int i = 0; i < geometryData.size(); i++) {
            vertexData.add(geometryData.getFloat(i));
        }
        vertexCount = tag.getInt("vc");
    }

    public String getPartType() {
        return "msh";
    }
}
