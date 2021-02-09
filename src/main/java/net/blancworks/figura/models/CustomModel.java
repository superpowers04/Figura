package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.models.lua.CustomScript;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.luaj.vm2.LuaNumber;

import java.util.ArrayList;

public class CustomModel {
    public PlayerData owner;
    public ArrayList<CustomModelPart> all_parts = new ArrayList<CustomModelPart>();

    public int getMaxRenderAmount() {
        return 128;
    }

    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        int left_to_render = getMaxRenderAmount();
        
        if(owner.script != null)
            owner.script.runFunctionImmediate("render", CustomScript.max_lua_instructions_render, LuaNumber.valueOf(FiguraMod.deltaTime));
        
        for (CustomModelPart part : all_parts) {

            matrices.push();

            player_model.setVisible(false);

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
            }

            left_to_render = part.render(left_to_render, matrices, vertices, light, overlay);

            matrices.pop();
        }
    }

    public void toNBT(CompoundTag tag) {

        ListTag partList = new ListTag();

        for (int i = 0; i < all_parts.size(); i++) {
            CompoundTag partTag = new CompoundTag();
            all_parts.get(i).toNBT(partTag);
            partList.add(partTag);
        }

        tag.put("parts", partList);
    }

    public void fromNBT(CompoundTag tag) {
        ListTag partList = (ListTag) tag.get("parts");

        for (int i = 0; i < partList.size(); i++) {
            CompoundTag partTag = (CompoundTag) partList.get(i);
            int type = partTag.getInt("type");
            CustomModelPart part = null;
            
            switch (type) {
                case 0:
                    CustomModelPart np = new CustomModelPart();
                    np.fromNBT(partTag);
                    part = np;
                    break;
                case 1:
                    CustomModelPartMesh nmp = new CustomModelPartMesh();
                    nmp.fromNBT(partTag);
                    part=nmp;
                    break;
            }
            
            all_parts.add(part);
        }
    }

}
