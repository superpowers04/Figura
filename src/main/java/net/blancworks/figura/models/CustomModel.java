package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.assets.FiguraAsset;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
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
import java.util.HashMap;

public class CustomModel extends FiguraAsset {
    public PlayerData owner;
    public ArrayList<CustomModelPart> allParts = new ArrayList<>();

    //Customized pivots for stuff like elytra, held items, that sort.
    public HashMap<Identifier, CustomModelPart> customParents = new HashMap<>();

    //TODO - probably improve this?
    public ArrayList<CustomModelPart> leftElytraParts = new ArrayList<>();
    public ArrayList<CustomModelPart> rightElytraParts = new ArrayList<>();
    public ArrayList<CustomModelPart> worldParts = new ArrayList<>();

    public float texWidth = 64, texHeight = 64;

    //The size of the avatar in bytes, either from when it was downloaded, or otherwise.
    public long totalSize = 0;

    public int leftToRender = 0;
    public int lastComplexity = 0;

    //This contains all the modifications to origins for stuff like elytra and held items.
    //This is separate from script customizations, as these are groups from blockbench that are the new,
    //override origins against vanilla.
    public HashMap<Identifier, VanillaModelPartCustomization> originModifications = new HashMap<>();

    public int getRenderComplexity() {
        lastComplexity = 0;

        try {
            for (CustomModelPart part : allParts) {
                lastComplexity += part.getComplexity();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Integer.MAX_VALUE - 100;
        }

        return lastComplexity;
    }

    public int getMaxRenderAmount() {
        Identifier playerId = new Identifier("players", this.owner.playerId.toString());
        TrustContainer tc = PlayerTrustManager.getContainer(playerId);
        return tc.getIntSetting(PlayerTrustManager.MAX_COMPLEXITY_ID);
    }


    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay, float red, float green, float blue, float alpha) {
        render(player_model, matrices, new MatrixStack(), vcp, light, overlay, red, green, blue, alpha);
    }

    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, MatrixStack transformStack,  VertexConsumerProvider vcp, int light, int overlay, float red, float green, float blue, float alpha) {
        leftToRender = getMaxRenderAmount();
        int maxRender = leftToRender;

        if (owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        for (CustomModelPart part : allParts) {

            if (part.isParentSpecial() || !part.shouldRender || !part.visible)
                continue;

            matrices.push();

            try {
                player_model.setVisible(false);

                //By default, use blockbench rotation.
                part.rotationType = CustomModelPart.RotationType.BlockBench;

                //render only heads in spectator
                if (owner.lastEntity != null && owner.lastEntity.isSpectator()) {
                    leftToRender = part.renderUsingAllTexturesFiltered(CustomModelPart.ParentType.Head, owner, matrices, transformStack, vcp, light, overlay, alpha);
                }
                else {
                    leftToRender = part.renderUsingAllTextures(owner, matrices, transformStack, vcp, light, overlay, alpha);
                }

                lastComplexity = MathHelper.clamp(maxRender - leftToRender, 0, maxRender);
            } catch (Exception e) {
                e.printStackTrace();
            }

            matrices.pop();
        }
    }

    public void renderArm(PlayerData playerData, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, PlayerEntityModel model, float alpha) {
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(playerData.texture.id));

        if (owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        int prevCount = playerData.model.leftToRender;
        playerData.model.leftToRender = Integer.MAX_VALUE - 100;

        for (CustomModelPart part : playerData.model.allParts) {
            if (arm == model.rightArm)
                playerData.model.leftToRender = part.renderUsingAllTexturesFiltered(CustomModelPart.ParentType.RightArm, playerData, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, alpha);
            else if (arm == model.leftArm)
                playerData.model.leftToRender = part.renderUsingAllTexturesFiltered(CustomModelPart.ParentType.LeftArm, playerData, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, alpha);
        }

        playerData.model.leftToRender = prevCount;
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

        sortAllParts();
    }

    //Sorts parts into their respective places.
    public void sortAllParts(){
        for (CustomModelPart part : allParts) {
            sortPart(part);
        }
    }

    public void sortPart(CustomModelPart part){
        if (part.parentType == CustomModelPart.ParentType.LeftElytra) {
            leftElytraParts.add(part);
        } else if (part.parentType == CustomModelPart.ParentType.RightElytra) {
            rightElytraParts.add(part);
        } else if (part.parentType == CustomModelPart.ParentType.WORLD) {
            worldParts.add(part);
        }

        for (CustomModelPart child : part.children) {
            sortPart(child);
        }
    }
}
