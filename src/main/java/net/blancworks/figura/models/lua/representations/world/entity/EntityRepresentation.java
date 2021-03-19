package net.blancworks.figura.models.lua.representations.world.entity;

import net.blancworks.figura.models.lua.CustomScript;
import net.blancworks.figura.models.lua.LuaUtils;
import net.blancworks.figura.models.lua.representations.LuaRepresentation;
import net.blancworks.figura.models.lua.representations.item.ItemStackRepresentation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;
import java.util.Iterator;

public class EntityRepresentation<T extends Entity> extends LuaRepresentation {
    public int entityID = 0;
    public T targetEntity = null;

    public EntityRepresentation(CustomScript customScript) {
        super(customScript);
    }

    @Override
    public void getReferences() {
        super.getReferences();
        targetEntity = (T) MinecraftClient.getInstance().world.getEntityById(entityID);
    }

    @Override
    public String getDefaultTableKey() {
        return "entity_" + entityID;
    }

    @Override
    public void fillLuaTable(LuaTable table) {
        super.fillLuaTable(table);

        table.set("getID", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (targetEntity == null) return NIL;
                return LuaInteger.valueOf(targetEntity.getEntityId());
            }
        });

        table.set("getPos", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (targetEntity == null) return NIL;
                return LuaUtils.getTableFromVec3(targetEntity.getPos());
            }
        });

        table.set("getLookDir", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (targetEntity == null) return NIL;
                return LuaUtils.getTableFromVec3(targetEntity.getRotationVector());
            }
        });

        table.set("getAnimation", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (targetEntity == null) return NIL;

                EntityPose p = targetEntity.getPose();

                if (p == null)
                    return NIL;

                return LuaString.valueOf(p.name());
            }
        });

        table.set("getWorldName", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (targetEntity == null) return NIL;
                World w = targetEntity.world;

                return LuaString.valueOf(w.getRegistryKey().getValue().toString());
            }
        });

        table.set("getEntityType", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (targetEntity == null) return NIL;

                return LuaString.valueOf(Registry.ENTITY_TYPE.getId(targetEntity.getType()).toString());
            }
        });

        table.set("getVelocity", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (targetEntity == null) return NIL;
                return LuaUtils.getTableFromVec3(targetEntity.getVelocity());
            }
        });

        table.set("getEquippedItem", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (arg == null)
                    arg = LuaNumber.valueOf(1);

                if (!arg.isnumber())
                    return NIL;

                int slot = arg.checkint();
                Iterable<ItemStack> equipped = targetEntity.getItemsEquipped();
                ItemStack targetStack = retrieveItemByIndex(equipped, slot);

                LuaTable getItemRepresentation = ItemStackRepresentation.getTable(targetStack);
                return getItemRepresentation;
            }
        });
    }
    
    private static <T> T retrieveItemByIndex(Iterable<T> iterable, int index) {

        if (iterable == null || index < 0) {

            return null;
        }

        int cursor = 0;

        Iterator<T> iterator = iterable.iterator();

        while (cursor < index && iterator.hasNext()) {

            iterator.next();
            cursor++;
        }

        return cursor == index && iterator.hasNext() ? iterator.next() : null;
    }
}
