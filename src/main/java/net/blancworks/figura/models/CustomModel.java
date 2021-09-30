package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.assets.FiguraAsset;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
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
        if (this.owner == null)
            return 0;

        TrustContainer tc = PlayerTrustManager.getContainer(new Identifier("players", this.owner.playerId.toString()));
        return tc != null ? tc.getIntSetting(PlayerTrustManager.MAX_COMPLEXITY_ID) : 0;
    }

    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        render(player_model, matrices, new MatrixStack(), vcp, light, overlay, alpha);
    }

    public void render(PlayerEntityModel<?> player_model, MatrixStack matrices, MatrixStack transformStack,  VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        leftToRender = getMaxRenderAmount();
        int maxRender = leftToRender;

        if (owner != null && owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        for (CustomModelPart part : allParts) {

            if (part.isParentSpecial() || !part.visible || part.isHidden)
                continue;

            matrices.push();

            try {
                player_model.setVisible(false);

                //By default, use blockbench rotation.
                part.rotationType = CustomModelPart.RotationType.BlockBench;

                //render only heads in spectator
                if (owner.lastEntity != null && owner.lastEntity.isSpectator())
                    CustomModelPart.renderOnly = CustomModelPart.ParentType.Head;

                leftToRender = part.render(owner, matrices, transformStack, vcp, light, overlay, alpha);

                lastComplexity = MathHelper.clamp(maxRender - leftToRender, 0, maxRender);
            } catch (Exception e) {
                e.printStackTrace();
            }

            matrices.pop();
        }
    }

    public void renderArm(PlayerData playerData, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ModelPart arm, PlayerEntityModel<?> model, float alpha) {
        if (owner != null && owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        int prevCount = playerData.model.leftToRender;
        playerData.model.leftToRender = Integer.MAX_VALUE - 100;

        for (CustomModelPart part : playerData.model.allParts) {
            if (arm == model.rightArm)
                CustomModelPart.renderOnly = CustomModelPart.ParentType.RightArm;
            else if (arm == model.leftArm)
                CustomModelPart.renderOnly = CustomModelPart.ParentType.LeftArm;

            playerData.model.leftToRender = part.render(playerData, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, alpha);
        }

        playerData.model.leftToRender = prevCount;
    }

    public void writeNbt(NbtCompound nbt) {
        NbtList partList = new NbtList();

        for (CustomModelPart part : allParts) {
            NbtCompound partNbt = new NbtCompound();
            CustomModelPart.writeToNbt(partNbt, part);
            partList.add(partNbt);
        }

        nbt.put("parts", partList);
    }

    public void readNbt(NbtCompound tag) {
        NbtList partList = (NbtList) tag.get("parts");

        if (partList != null) {
            for (net.minecraft.nbt.NbtElement nbtElement : partList) {
                NbtCompound partTag = (NbtCompound) nbtElement;

                CustomModelPart part = CustomModelPart.fromNbt(partTag);

                if (part != null) {
                    part.rebuild();

                    allParts.add(part);
                }
            }
        }

        sortAllParts();
    }

    //Sorts parts into their respective places.
    public void sortAllParts() {
        leftElytraParts.clear();
        rightElytraParts.clear();
        worldParts.clear();

        for (CustomModelPart part : allParts) {
            sortPart(part);
        }
    }

    public void sortPart(CustomModelPart part) {
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
