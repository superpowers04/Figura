package net.blancworks.figura.models;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ObjReader;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

@Deprecated
public class CustomModelPartMesh extends CustomModelPart {



    public void fromNBT(CompoundTag partTag) {

    }

    /*//All the mesh components.
    //Sorted in sets of 8.
    //Vertex X,Y,Z
    //UV X,Y
    //Normal X,Y,Z
    public FloatList meshComponents = new FloatArrayList();
    public int faceCount;

    public boolean is_ready = false;

    public CustomModelPartMesh() {
        super(64, 64, 0, 0);
    }

    @Override
    public int render(int left, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        //Render this as we would normally render a modelPart.
        super.render(left, matrices, vertices, light, overlay, red, green, blue, alpha);

        //Checks if the mesh part is finished.
        if (!is_ready)
            return left;

        matrices.push();
        rotate(matrices);

        Matrix4f transformation = matrices.peek().getModel();
        Matrix3f normalTransformation = matrices.peek().getNormal();

        Vector4f vert = null;
        Vector3f normal = null;

        int face_number = 0;
        int currIndex = 0;

        try {

            for (int i = 0; i < faceCount; i++) {

                face_number++;

                if (face_number == 4) {
                    left--;
                    face_number = 0;
                }
                if (left <= 0)
                    break;

                for (int j = 0; j < 4; j++) {

                    vert = new Vector4f(
                            meshComponents.getFloat(currIndex++),
                            meshComponents.getFloat(currIndex++),
                            meshComponents.getFloat(currIndex++),
                            1.0f
                    );
                    float u = meshComponents.getFloat(currIndex++);
                    float v = meshComponents.getFloat(currIndex++);
                    normal = new Vector3f(
                            meshComponents.getFloat(currIndex++),
                            meshComponents.getFloat(currIndex++),
                            meshComponents.getFloat(currIndex++)
                    );

                    vert.transform(transformation);
                    normal.transform(normalTransformation);

                    //Add vertex.
                    vertices.vertex(
                            //Vertex
                            vert.getX(), vert.getY(), vert.getZ(),
                            //Color
                            red, green, blue, alpha,
                            //UV
                            u, v,
                            //Overlay/light
                            overlay, light,
                            //Normal
                            normal.getX(), normal.getY(), normal.getZ()
                    );
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        matrices.pop();
        return left;
    }

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
        meshComponents.clear();

        InputStream fileStream = new FileInputStream(path.toString());
        Obj object_file = ObjReader.read(fileStream);
        fileStream.close();

        int triCount = object_file.getNumFaces();

        for (int i = 0; i < triCount; i++) {
            ObjFace face = object_file.getFace(i);
            int vertCount = face.getNumVertices();

            if (vertCount == 4) {
                faceCount++;
                for (int j = vertCount - 1; j >= 0; j--) {
                    FloatTuple vertex = object_file.getVertex(face.getVertexIndex(j));
                    meshComponents.add(vertex.getX());
                    meshComponents.add(-vertex.getY());
                    meshComponents.add(vertex.getZ());

                    FloatTuple uv = object_file.getTexCoord(face.getTexCoordIndex(j));
                    meshComponents.add(uv.getX());
                    meshComponents.add(1 - uv.getY());

                    FloatTuple normal = object_file.getNormal(face.getNormalIndex(j));
                    meshComponents.add(-normal.getX());
                    meshComponents.add(-normal.getY());
                    meshComponents.add(-normal.getZ());
                }
            } else if (vertCount == 3) {
                faceCount++;
                for (int j = vertCount - 1; j >= 0; j--) {
                    FloatTuple vertex = object_file.getVertex(face.getVertexIndex(j));
                    meshComponents.add(vertex.getX());
                    meshComponents.add(-vertex.getY());
                    meshComponents.add(vertex.getZ());

                    FloatTuple uv = object_file.getTexCoord(face.getTexCoordIndex(j));
                    meshComponents.add(uv.getX());
                    meshComponents.add(1 - uv.getY());

                    FloatTuple normal = object_file.getNormal(face.getNormalIndex(j));
                    meshComponents.add(-normal.getX());
                    meshComponents.add(-normal.getY());
                    meshComponents.add(-normal.getZ());

                    if (j == vertCount - 1) {
                        vertex = object_file.getVertex(face.getVertexIndex(j));
                        meshComponents.add(vertex.getX());
                        meshComponents.add(-vertex.getY());
                        meshComponents.add(vertex.getZ());

                        uv = object_file.getTexCoord(face.getTexCoordIndex(j));
                        meshComponents.add(uv.getX());
                        meshComponents.add(1 - uv.getY());

                        normal = object_file.getNormal(face.getNormalIndex(j));
                        meshComponents.add(-normal.getX());
                        meshComponents.add(-normal.getY());
                        meshComponents.add(-normal.getZ());
                    }
                }
            }

        }

        //System.out.println(meshComponents.size() / 7);
        is_ready = true;
    }


    @Override
    public void toNBT(CompoundTag tag) {
        super.toNBT(tag);
        ListTag geometryData = new ListTag();

        for (int i = 0; i < meshComponents.size(); i++) {
            geometryData.add(FloatTag.of(meshComponents.getFloat(i)));
        }
        tag.put("geo", geometryData);
        tag.putInt("type", 1);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        super.fromNBT(tag);
        ListTag geometryData = (ListTag) tag.get("geo");

        for (int i = 0; i < geometryData.size(); i++) {
            meshComponents.add(geometryData.getFloat(i));
        }
    }*/
}
