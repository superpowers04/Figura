package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.access.MatrixStackAccess;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;

public class CustomModel {
    public PlayerData owner;
    public ArrayList<CustomModelPart> allParts = new ArrayList<CustomModelPart>();

    public float texWidth = 64, texHeight = 64;

    //The size of the avatar in bytes, either from when it was downloaded, or otherwise.
    public long totalSize = 0;


    public int getRenderComplexity() {

        int ret = 0;

        for (CustomModelPart all_part : allParts) {
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
        Identifier playerId = new Identifier("players", this.owner.playerId.toString());
        TrustContainer tc = PlayerTrustManager.getContainer(playerId);
        return tc.getIntSetting(PlayerTrustManager.MAX_COMPLEXITY_ID);
    }

    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        int leftToRender = getMaxRenderAmount();

        if (owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        FiguraMod.currentModel = player_model;

        for (CustomModelPart part : allParts) {
            matrices.push();

            try {
                player_model.setVisible(false);
                MatrixStack tempStack = null;

                //By default, use blockbench rotation.
                part.rotationType = CustomModelPart.RotationType.BlockBench;

                if (!part.isMimicMode && part.parentType != CustomModelPart.ParentType.Model) {
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
                            ((MatrixStackAccess) matrices).copyTo(tempStack);

                            //Push to be sure we don't modify the original stack values.
                            tempStack.pop();
                            tempStack.pop();
                            tempStack.push();

                            double d = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.currentPlayer.lastRenderX, FiguraMod.currentPlayer.getX());
                            double e = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.currentPlayer.lastRenderY, FiguraMod.currentPlayer.getY());
                            double f = MathHelper.lerp(FiguraMod.deltaTime, FiguraMod.currentPlayer.lastRenderZ, FiguraMod.currentPlayer.getZ());

                            tempStack.translate(-d, -e, -f);
                            tempStack.push();
                            tempStack.scale(-1, -1, 1);
                            tempStack.push();

                            break;
                    }
                }

                if (tempStack != null)
                    leftToRender = part.render(leftToRender, tempStack, vertices, light, overlay);
                else
                    leftToRender = part.render(leftToRender, matrices, vertices, light, overlay);

            } catch (Exception e) {
                e.printStackTrace();
            }

            matrices.pop();
        }

        FiguraMod.currentModel = null;
    }
    
    public void renderArm(PlayerData playerData, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve){
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(playerData.texture.id));

        if (owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }
        
        for (CustomModelPart part : playerData.model.allParts) {
            if (part.parentType == CustomModelPart.ParentType.RightArm) {
                matrices.push();

                arm.rotate(matrices);
                part.render(99999, matrices, vc, light, OverlayTexture.DEFAULT_UV);

                matrices.pop();
            }
        }
    }

    public void writeNbt(CompoundTag nbt) {
        ListTag partList = new ListTag();

        for (CustomModelPart part : allParts) {
            CompoundTag partNbt = new CompoundTag();
            CustomModelPart.writeToNbt(partNbt, part);
            partList.add(partNbt);
        }

        nbt.put("parts", partList);
    }

    public void readNbt(CompoundTag tag) {
        ListTag partList = (ListTag) tag.get("parts");

        for (int i = 0; i < partList.size(); i++) {
            CompoundTag partTag = (CompoundTag) partList.get(i);
            int type = partTag.getInt("type");

            CustomModelPart part = CustomModelPart.fromNbt(partTag);

            if (part != null) {
                part.rebuild();
                allParts.add(part);
            }
        }
    }

}
