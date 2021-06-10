package net.blancworks.figura.lua.api.item;

import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ItemStackAPI {

    public static ReadOnlyLuaTable getTable(ItemStack stack) {
        ReadOnlyLuaTable outTable = new ReadOnlyLuaTable(new LuaTable() {{

            set("getType", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(Registry.ITEM.getId(stack.getItem()).toString());
                }
            });

            set("getTag", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Tag tag = stack.getTag();
                    LuaValue fVal = NBTAPI.fromTag(tag);
                    return fVal;
                }
            });

            set("getCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(stack.getCount());
                }
            });

            set("getDamage", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(stack.getDamage());
                }
            });

            set("getCooldown", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(stack.getCooldown());
                }
            });

            set("hasGlint", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(stack.hasGlint());
                }
            });

            set("getItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return ItemAPI.getTable(stack.getItem());
                }
            });

            set("getItemTags", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable table = new LuaTable();

                    ItemTags.getTagGroup().getTagsFor(stack.getItem()).forEach(identifier -> table.insert(0, LuaValue.valueOf(String.valueOf(identifier))));

                    return new ReadOnlyLuaTable(table);
                }
            });

        }});

        return outTable;
    }
}
