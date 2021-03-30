package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.MatrixStackAccess;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

public class CustomModel {
    public PlayerData owner;
    public ArrayList<CustomModelPart> all_parts = new ArrayList<CustomModelPart>();

    public float texWidth = 64, texHeight = 64;
    
    //The size of the avatar in bytes, either from when it was downloaded, or otherwise.
    public long totalSize = 0;


    public int getRenderComplexity() {

        int ret = 0;

        for (CustomModelPart all_part : all_parts) {
            ret += getComplexityRecursive(all_part);
        }

        return ret;
    }

    private int getComplexityRecursive(CustomModelPart part) {
        int ret = 0;

        ret += part.vertexCount / 4;

        for (CustomModelPart child : part.children) {
            ret += getComplexityRecursive(child);
        }

        return ret;
    }

    public int getMaxRenderAmount() {
        Identifier playerID = new Identifier("players", owner.playerId.toString());
        TrustContainer tc = PlayerTrustManager.getContainer(playerID);
        return tc.getIntSetting(PlayerTrustManager.maxComplexityID);
    }

    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        int left_to_render = getMaxRenderAmount();

        if (owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        for (CustomModelPart part : all_parts) {

            matrices.push();

            try {
                player_model.setVisible(false);
                MatrixStack tempStack = null;

                switch (part.parentType) {
                    case Head:
                        player_model.head.rotate(matrices);
                        break;
                    case Torso:
                        player_model.body.rotate(matrices);
                        break;
                    case LeftArm:
                        player_model.leftArm.rotate(matrices);
                        break;
                    case LeftLeg:
                        player_model.leftLeg.rotate(matrices);
                        break;
                    case RightArm:
                        player_model.rightArm.rotate(matrices);
                        break;
                    case RightLeg:
                        player_model.rightLeg.rotate(matrices);
                        break;
                    case None:

                        //Make shallow copy of OG stack.
                        tempStack = new MatrixStack();
                        ((MatrixStackAccess) (Object) matrices).copyTo(tempStack);

                        //Push to be sure we don't modify the original stack values.
                        tempStack.pop();
                        tempStack.pop();
                        tempStack.push();
                        
                        double d = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.curr_player.lastRenderX, FiguraMod.curr_player.getX());
                        double e = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.curr_player.lastRenderY, FiguraMod.curr_player.getY());
                        double f = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.curr_player.lastRenderZ, FiguraMod.curr_player.getZ());

                        tempStack.translate(-d, -e, -f);
                        tempStack.push();
                        tempStack.scale(-1,-1,1);
                        tempStack.push();

                        break;
                }

                if (tempStack != null)
                    left_to_render = part.render(left_to_render, tempStack, vertices, light, overlay);
                else
                    left_to_render = part.render(left_to_render, matrices, vertices, light, overlay);
                
            } catch (Exception e){
                e.printStackTrace();
            }
            
            matrices.pop();
        }
    }

    public void toNBT(NbtCompound tag) {

        NbtList partList = new NbtList();

        for (int i = 0; i < all_parts.size(); i++) {
            NbtCompound partTag = new NbtCompound();
            CustomModelPart.writeToCompoundTag(partTag, all_parts.get(i));
            partList.add(partTag);
        }

        tag.put("parts", partList);
    }

    public void fromNBT(NbtCompound tag) {
        NbtList partList = (NbtList) tag.get("parts");

        for (int i = 0; i < partList.size(); i++) {
            NbtCompound partTag = (NbtCompound) partList.get(i);
            int type = partTag.getInt("type");

            CustomModelPart part = CustomModelPart.getFromNbtTag(partTag);

            if (part != null) {
                part.rebuild();
                all_parts.add(part);
            }
        }
    }

}
