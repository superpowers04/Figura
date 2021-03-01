package net.blancworks.figura.models;

import com.google.gson.JsonObject;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class CustomModelPartCuboid extends CustomModelPart {

    //Used to store the data for a cuboid, so that we can re-build it later if need be.
    public CompoundTag cuboidProperties = new CompoundTag();

    @Override
    public void rebuild() {
        
        vertexData.clear();
        vertexCount = 0;
        
        Vector3f from = v3fFromNbtList((ListTag) cuboidProperties.get("f"));
        Vector3f to = v3fFromNbtList((ListTag) cuboidProperties.get("t"));
        
        float texWidth = cuboidProperties.getFloat("tw");
        float texHeight = cuboidProperties.getFloat("th");

        //North
        {
            ListTag faceData = (ListTag) cuboidProperties.get("n");
            generateFace(
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    v4fFromNbtList((ListTag)faceData),
                    texWidth, texHeight
            );
        }

        //South
        {
            ListTag faceData = (ListTag) cuboidProperties.get("s");
            generateFace(
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    v4fFromNbtList((ListTag)faceData),
                    texWidth, texHeight
            );
        }

        //East
        {
            ListTag faceData = (ListTag) cuboidProperties.get("e");
            generateFace(
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    v4fFromNbtList((ListTag)faceData),
                    texWidth, texHeight
            );
        }

        //West
        {
            ListTag faceData = (ListTag) cuboidProperties.get("w");
            generateFace(
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    v4fFromNbtList((ListTag)faceData),
                    texWidth, texHeight
            );
        }

        //Top
        {
            ListTag faceData = (ListTag) cuboidProperties.get("u");
            generateFace(
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    v4fFromNbtList((ListTag)faceData),
                    texWidth, texHeight
            );
        }

        //Bottom
        {
            ListTag faceData = (ListTag) cuboidProperties.get("d");
            generateFace(
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    v4fFromNbtList((ListTag)faceData),
                    texWidth, texHeight
            );
        }
    }

    public void generateFace(Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector4f uv, float texWidth, float texHeight) {
        Vector3f nA = b.copy();
        nA.subtract(a);
        Vector3f nB = c.copy();
        nB.subtract(a);
        nA.cross(nB);
        nA.normalize();

        addVertex(b, uv.getX() / texWidth, uv.getW() / texHeight, nA);
        addVertex(a, uv.getZ() / texWidth, uv.getW() / texHeight, nA);
        addVertex(d, uv.getZ() / texWidth, uv.getY() / texHeight, nA);
        addVertex(c, uv.getX() / texWidth, uv.getY() / texHeight, nA);
    }
    

    @Override
    public void fromNBT(CompoundTag partTag) {
        super.fromNBT(partTag);
        cuboidProperties = (CompoundTag) partTag.get("props");
    }


    @Override
    public void toNBT(CompoundTag partTag) {
        super.toNBT(partTag);
        partTag.put("props", cuboidProperties);
    }

    @Override
    public String getPartType() {
        return "cub";
    }
    
    public Vector3f v3fFromNbtList(ListTag list){
        return new Vector3f(list.getFloat(0), list.getFloat(1), list.getFloat(2));
    }
    public Vector4f v4fFromNbtList(ListTag list){
        return new Vector4f(list.getFloat(0), list.getFloat(1), list.getFloat(2), list.getFloat(3));
    }
}
