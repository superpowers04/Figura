package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.assets.FiguraAsset;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.lua.api.model.VanillaModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomModel extends FiguraAsset {
    public PlayerData owner;
    public CompoundTag modelNbt = new CompoundTag();

    public final ArrayList<CustomModelPart> allParts = new ArrayList<>();
    public final HashMap<CustomModelPart.ParentType, ArrayList<CustomModelPart>> specialParts = new HashMap<>();

    public Vec2f defaultTextureSize;

    public int leftToRender = 0;
    public int lastComplexity = 0;

    //used during rendering
    public boolean applyHiddenTransforms = true;
    public CustomModelPart.ParentType renderOnly = null;

    //This contains all the modifications to origins for stuff like elytra and held items.
    //This is separate from script customizations, as these are groups from blockbench that are the new,
    //override origins against vanilla.
    public HashMap<Identifier, VanillaModelPartCustomization> originModifications = new HashMap<>();

    public ArrayList<CustomModelPart> getSpecialParts(CustomModelPart.ParentType type) {
        synchronized (specialParts) {
            ArrayList<CustomModelPart> list = specialParts.get(type);
            return list == null ? new ArrayList<>() : list;
        }
    }

    public void removeSpecialPart(CustomModelPart part) {
        synchronized (specialParts) {
            getSpecialParts(part.parentType).remove(part);
        }
    }

    public void addSpecialPart(CustomModelPart part) {
        synchronized (specialParts) {
            ArrayList<CustomModelPart> list = getSpecialParts(part.parentType);
            list.add(part);
            specialParts.put(part.parentType, list);
        }
    }

    public int getRenderComplexity() {
        lastComplexity = 0;

        try {
            synchronized (this.allParts) {
                for (CustomModelPart part : this.allParts) {
                    lastComplexity += part.getComplexity();
                }
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

        TrustContainer tc = PlayerTrustManager.getContainer(new Identifier("player", this.owner.playerId.toString()));
        return tc != null ? tc.getTrust(TrustContainer.Trust.COMPLEXITY) : 0;
    }

    public void render(EntityModel<?> entity_model, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        if (owner == null) return;

        leftToRender = getMaxRenderAmount();
        int maxRender = leftToRender;

        if (owner.script != null)
            owner.script.render(FiguraMod.deltaTime);

        synchronized (this.allParts) {
            for (CustomModelPart part : this.allParts) {
                if (part.isSpecial() || !part.visible)
                    continue;

                matrices.push();

                try {
                    if (entity_model instanceof PlayerEntityModel) {
                        ((PlayerEntityModel<?>) entity_model).setVisible(false);
                    }

                    //By default, use blockbench rotation.
                    part.rotationType = CustomModelPart.RotationType.BlockBench;

                    //render only heads in spectator
                    if (owner.lastEntity != null && owner.lastEntity.isSpectator())
                        renderOnly = CustomModelPart.ParentType.Head;

                    //hitboxes :p
                    CustomModelPart.canRenderHitBox = (boolean) Config.RENDER_DEBUG_PARTS_PIVOT.value && MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes();

                    leftToRender = part.render(owner, matrices, transformStack, vcp, light, overlay, alpha);
                    lastComplexity = MathHelper.clamp(maxRender - leftToRender, 0, maxRender);

                    CustomModelPart.canRenderHitBox = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                matrices.pop();
            }
        }
    }

    public void renderArm(PlayerData playerData, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ModelPart arm, PlayerEntityModel<?> model, float alpha) {
        if (owner != null && owner.script != null) {
            owner.script.render(FiguraMod.deltaTime);
        }

        int prevCount = playerData.model.leftToRender;
        playerData.model.leftToRender = Integer.MAX_VALUE - 100;

        synchronized (playerData.model.allParts) {
            //applyHiddenTransforms = !(boolean) Config.FIX_FIRST_PERSON_HANDS.value;
            for (CustomModelPart part : playerData.model.allParts) {
                if (arm == model.rightArm)
                    renderOnly = CustomModelPart.ParentType.RightArm;
                else if (arm == model.leftArm)
                    renderOnly = CustomModelPart.ParentType.LeftArm;

                playerData.model.leftToRender = part.render(playerData, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, alpha);
            }
            //applyHiddenTransforms = true;
        }

        playerData.model.leftToRender = prevCount;
    }

    public boolean renderSkull(PlayerData data, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        data.model.leftToRender = getMaxRenderAmount();

        ArrayList<CustomModelPart> skullParts = data.model.getSpecialParts(CustomModelPart.ParentType.Skull);
        if (!skullParts.isEmpty()) {
            for (CustomModelPart modelPart : skullParts) {
                data.model.leftToRender = modelPart.render(data, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, 1f);

                if (data.model.leftToRender <= 0)
                    break;
            }

            return true;
        }
        else {
            synchronized (this.allParts) {
                applyHiddenTransforms = false;
                for (CustomModelPart modelPart : data.model.allParts) {
                    renderOnly = CustomModelPart.ParentType.Head;
                    data.model.leftToRender = modelPart.render(data, matrices, new MatrixStack(), vertexConsumers, light, OverlayTexture.DEFAULT_UV, 1f);

                    if (data.model.leftToRender <= 0)
                        break;
                }
                applyHiddenTransforms = true;
            }

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

        CustomModelPart.canRenderHitBox = (boolean) Config.RENDER_DEBUG_PARTS_PIVOT.value && MinecraftClient.getInstance().getEntityRenderDispatcher().shouldRenderHitboxes();

        synchronized (specialParts) {
            for (CustomModelPart part : data.model.getSpecialParts(CustomModelPart.ParentType.WORLD)) {
                data.model.leftToRender = part.render(data, matrices, new MatrixStack(), vertexConsumers, light, overlay, alpha);
            }
        }

        CustomModelPart.canRenderHitBox = false;
    }

    public void renderHudParts(PlayerData data, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        float scale = (float) (128f / client.getWindow().getScaleFactor());
        float w = client.getWindow().getScaledWidth() / 2f;
        float h = client.getWindow().getScaledHeight() / 2f;

        matrices.push();
        matrices.translate(w, h, -500);
        matrices.scale(scale, scale, -scale);
        DiffuseLighting.disableGuiDepthLighting();

        data.model.leftToRender = Integer.MAX_VALUE - 100;

        synchronized (specialParts) {
            for (CustomModelPart part : data.model.getSpecialParts(CustomModelPart.ParentType.Hud)) {
                part.render(data, matrices, new MatrixStack(), FiguraMod.tryGetImmediate(), 0xF000F0, OverlayTexture.DEFAULT_UV, 1f);
            }
        }
        matrices.pop();
    }

    public void readNbt(CompoundTag tag) {
        ListTag partList = (ListTag) tag.get("parts");

        ListTag uv = tag.getList("uv", NbtType.FLOAT);
        if (uv.size() >= 2) this.defaultTextureSize = new Vec2f(uv.getFloat(0), uv.getFloat(1));

        if (partList != null) {
            for (Tag nbtElement : partList) {
                CompoundTag partTag = (CompoundTag) nbtElement;

                CustomModelPart part = CustomModelPart.fromNbt(partTag);

                if (part != null) {
                    part.rebuildAll(this.defaultTextureSize);
                    allParts.add(part);
                }
            }
        }

        sortAllParts();
    }

    //Sorts parts into their respective places.
    public void sortAllParts() {
        specialParts.clear();

        synchronized (this.allParts) {
            for (CustomModelPart part : this.allParts)
                sortPart(part);
        }
    }

    public void sortPart(CustomModelPart part) {
        if (part.isSpecial())
            addSpecialPart(part);

        for (CustomModelPart child : part.children)
            sortPart(child);
    }
}
