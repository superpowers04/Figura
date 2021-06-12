package net.blancworks.figura.lua.api.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.Tag;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ItemStackAPI {

    public static Identifier getID() {
        return new Identifier("default", "item_stack");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("createItem", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    return getTable(Registry.ITEM.get(Identifier.tryParse(arg.checkjstring())).getDefaultStack());
                }
            });

        }});
    }

    public static ReadOnlyLuaTable getTable(ItemStack stack) {

        return new ReadOnlyLuaTable(new LuaTable() {{

            set("stack", LuaValue.userdataOf(stack));

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
                    return NBTAPI.fromTag(tag);
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


            set("setCount", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    stack.setCount(arg.checkint());
                    return NIL;
                }
            });

            set("setDamage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    stack.setDamage(arg.checkint());
                    return NIL;
                }
            });

            set("setTag", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    StringReader reader = new StringReader(arg.checkjstring());

                    try {
                        stack.setTag((CompoundTag) new StringNbtReader(reader).parseTag());
                    } catch (CommandSyntaxException e) {
                        throw new LuaError(e.getMessage());
                    } catch (Exception e) {
                        throw new LuaError("Could not parse argument");
                    }

                    return NIL;
                }
            });

        }});
    }
}
