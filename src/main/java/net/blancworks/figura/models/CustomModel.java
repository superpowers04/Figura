package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
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

    public int lastComplexity = 0;


    public int getRenderComplexity() {
        return lastComplexity;
    }

    public int getMaxRenderAmount() {
        Identifier playerId = new Identifier("players", this.owner.playerId.toString());
        TrustContainer tc = PlayerTrustManager.getContainer(playerId);
        return tc.getIntSetting(PlayerTrustManager.MAX_COMPLEXITY_ID);
    }

    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        int leftToRender = getMaxRenderAmount();
        int maxRender = leftToRender;

        if (owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        for (CustomModelPart part : allParts) {

            if (part.parentType == CustomModelPart.ParentType.WORLD)
                continue;

            matrices.push();

            try {
                player_model.setVisible(false);

                //By default, use blockbench rotation.
                part.rotationType = CustomModelPart.RotationType.BlockBench;

                leftToRender = part.render(leftToRender, matrices, vertices, light, overlay);

                lastComplexity = MathHelper.clamp(maxRender - leftToRender, 0, maxRender);
            } catch (Exception e) {
                e.printStackTrace();
            }

            matrices.pop();
        }
    }

    public void renderArm(PlayerData playerData, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(playerData.texture.id));

        if (owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        for (CustomModelPart part : playerData.model.allParts) {
            renderArmRecursive(part, playerData, matrices, vc, light, player, arm, sleeve);
        }
    }

    public void renderArmRecursive(CustomModelPart part, PlayerData playerData, MatrixStack matrices, VertexConsumer vertexConsumer, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve) {
        
        if(part.parentType == CustomModelPart.ParentType.RightArm)
            part.render(99999, matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        
        for (CustomModelPart child : part.children) {
            if (child.parentType == CustomModelPart.ParentType.RightArm) {
                matrices.push();

                child.render(99999, matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);

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
