package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.assets.FiguraAsset;
import net.blancworks.figura.config.ConfigManager;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomModel extends FiguraAsset {
    public PlayerData owner;
    public ArrayList<CustomModelPart> allParts = new ArrayList<>();
    public NbtCompound modelNbt = new NbtCompound();

    //Customized pivots for stuff like elytra, held items, that sort.
    public HashMap<Identifier, CustomModelPart> customParents = new HashMap<>();

    //TODO - probably improve this?
    public ArrayList<CustomModelPart> leftElytraParts = new ArrayList<>();
    public ArrayList<CustomModelPart> rightElytraParts = new ArrayList<>();
    public ArrayList<CustomModelPart> worldParts = new ArrayList<>();
    public ArrayList<CustomModelPart> skullParts = new ArrayList<>();

    public Vec2f defaultTextureSize = new Vec2f(0f, 0f);

    public int leftToRender = 0;
    public int lastComplexity = 0;

    //This contains all the modifications to origins for stuff like elytra and held items.
    //This is separate from script customizations, as these are groups from blockbench that are the new,
    //override origins against vanilla.
    public HashMap<Identifier, VanillaModelPartCustomization> originModifications = new HashMap<>();

    public int getRenderComplexity() {
        lastComplexity = 0;

        try {
            ArrayList<CustomModelPart> allParts = new ArrayList<>(this.allParts);
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

    public void render(EntityModel<?> entity_model, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        if (owner == null) return;

        leftToRender = getMaxRenderAmount();
        int maxRender = leftToRender;

        if (owner.script != null)
            owner.script.render(FiguraMod.deltaTime);

        ArrayList<CustomModelPart> allParts = new ArrayList<>(this.allParts);
        for (CustomModelPart part : allParts) {
            if (part.isParentSpecial() || !part.visible)
                continue;

            matrices.push();

            try {
                if (entity_model instanceof PlayerEntityModel player_model) {
                    player_model.setVisible(false);
                }

                //By default, use blockbench rotation.
                part.rotationType = CustomModelPart.RotationType.BlockBench;

                //render only heads in spectator
                if (owner.lastEntity != null && owner.lastEntity.isSpectator())
                    CustomModelPart.renderOnly = CustomModelPart.ParentType.Head;

                //hitboxes :p
                CustomModelPart.canRenderHitBox = (boolean) ConfigManager.Config.RENDER_DEBUG_PARTS_PIVOT.value && MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes();

                leftToRender = part.render(owner, matrices, transformStack, vcp, light, overlay, alpha);
                lastComplexity = MathHelper.clamp(maxRender - leftToRender, 0, maxRender);

                CustomModelPart.canRenderHitBox = false;
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

        //CustomModelPart.applyHiddenTransforms = false;
        ArrayList<CustomModelPart> allParts = new ArrayList<>(playerData.model.allParts);
        for (CustomModelPart part : allParts) {
            if (arm == model.rightArm)
                CustomModelPart.renderOnly = CustomModelPart.ParentType.RightArm;
            else if (arm == model.leftArm)
                CustomModelPart.renderOnly = CustomModelPart.ParentType.LeftArm;

            playerData.model.leftToRender = part.render(playerData, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, alpha);
        }
        //CustomModelPart.applyHiddenTransforms = true;

        playerData.model.leftToRender = prevCount;
    }

    public boolean renderSkull(PlayerData data, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        data.model.leftToRender = getMaxRenderAmount();

        if (!data.model.skullParts.isEmpty()) {
            ArrayList<CustomModelPart> skullParts = new ArrayList<>(data.model.skullParts);
            for (CustomModelPart modelPart : skullParts) {
                data.model.leftToRender = modelPart.render(data, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, 1f);

                if (data.model.leftToRender <= 0)
                    break;
            }

            return true;
        }
        else {
            CustomModelPart.applyHiddenTransforms = false;
            ArrayList<CustomModelPart> allParts = new ArrayList<>(data.model.allParts);
            for (CustomModelPart modelPart : allParts) {
                CustomModelPart.renderOnly = CustomModelPart.ParentType.Head;
                data.model.leftToRender = modelPart.render(data, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, 1f);

                if (data.model.leftToRender <= 0)
                    break;
            }
            CustomModelPart.applyHiddenTransforms = true;

            if (data.script != null) {
                VanillaModelPartCustomization customization = data.script.allCustomizations.get(VanillaModelAPI.VANILLA_HEAD);
                return customization != null && customization.visible != null && !customization.visible;
            }
        }

        return false;
    }

    public void renderWorldParts(PlayerData data, double cameraX, double cameraY, double cameraZ, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, float alpha) {
        matrices.translate(-cameraX, -cameraY, -cameraZ);
        matrices.scale(-1, -1, 1);

        CustomModelPart.canRenderHitBox = (boolean) ConfigManager.Config.RENDER_DEBUG_PARTS_PIVOT.value && MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes();

        ArrayList<CustomModelPart> worldParts = new ArrayList<>(data.model.worldParts);
        for (CustomModelPart part : worldParts) {
            data.model.leftToRender = part.render(data, matrices, new MatrixStack(), vertexConsumers, light, overlay, alpha);
        }

        CustomModelPart.canRenderHitBox = false;
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
        skullParts.clear();

        for (CustomModelPart part : allParts) {
            sortPart(part);
        }
    }

    public void sortPart(CustomModelPart part) {
        switch(part.parentType) {
            case LeftElytra -> leftElytraParts.add(part);
            case RightElytra -> rightElytraParts.add(part);
            case WORLD -> worldParts.add(part);
            case Skull -> skullParts.add(part);
        }

        for (CustomModelPart child : part.children) {
            sortPart(child);
        }
    }
}
