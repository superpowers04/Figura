package net.blancworks.figura.avatar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class EntityAvatarData extends AvatarData {

    public static final HashMap<Identifier, NbtCompound> CEM_MAP = new HashMap<>();

    public static void loadCemModels() {
        CEM_MAP.clear();

        //testing
        //CEM_MAP.put(new Identifier("minecraft", "piglin"), net.blancworks.figura.FiguraMod.cheese);
    }

    @Override
    public void tick() {
        if (this.entityId == null)
            return;

        if (MinecraftClient.getInstance().world != null) {
            MinecraftClient.getInstance().world.getEntities().forEach(entity -> {
                if (entity.getUuid().compareTo(this.entityId) == 0)
                    this.lastEntity = entity;
            });
        }

        if (lastEntity != null) {
            name = lastEntity.getName();

            if (script != null) {
                try {
                    script.tick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (model != null) {
                try {
                    model.tick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
