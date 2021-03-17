package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.MatrixStackAccess;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.luaj.vm2.LuaNumber;

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

        ret += part.vertexCount;

        for (CustomModelPart child : part.children) {
            ret += getComplexityRecursive(child);
        }

        return ret;
    }

    public int getMaxRenderAmount() {
        Identifier playerID = new Identifier("players", owner.playerId.toString());
        TrustContainer tc = PlayerTrustManager.getContainer(playerID);
        PermissionFloatSetting setting = (PermissionFloatSetting) tc.getSetting(PlayerTrustManager.maxComplexityID);

        return (int) setting.value;
    }

    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        int left_to_render = getMaxRenderAmount();

        if (owner.script != null) {
            owner.script.runFunctionImmediate("render", owner.script.getTrustInstructionLimit(PlayerTrustManager.maxRenderID), LuaNumber.valueOf(FiguraMod.deltaTime));
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
                        player_model.torso.rotate(matrices);
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
                        //tempStack.pop();
                        
                        Vec3d camPos = MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getPos();


                        double d = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.curr_player.lastRenderX, FiguraMod.curr_player.getX());
                        double e = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.curr_player.lastRenderY, FiguraMod.curr_player.getY());
                        double f = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.curr_player.lastRenderZ, FiguraMod.curr_player.getZ());
                        
                        tempStack.translate(-d/2,-e/2,-f/2);
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

    public void toNBT(CompoundTag tag) {

        ListTag partList = new ListTag();

        for (int i = 0; i < all_parts.size(); i++) {
            CompoundTag partTag = new CompoundTag();
            CustomModelPart.writeToCompoundTag(partTag, all_parts.get(i));
            partList.add(partTag);
        }

        tag.put("parts", partList);
    }

    public void fromNBT(CompoundTag tag) {
        ListTag partList = (ListTag) tag.get("parts");

        for (int i = 0; i < partList.size(); i++) {
            CompoundTag partTag = (CompoundTag) partList.get(i);
            int type = partTag.getInt("type");

            CustomModelPart part = CustomModelPart.getFromNbtTag(partTag);

            if (part != null) {
                part.rebuild();
                all_parts.add(part);
            }
        }
    }

}
