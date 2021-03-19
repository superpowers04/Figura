package net.blancworks.figura.models.lua.representations.item;

import net.blancworks.figura.models.lua.representations.LuaRepresentation;
import net.blancworks.figura.models.lua.representations.nbt.NbtTagRepresentation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.function.Supplier;

public class ItemStackRepresentation extends LuaRepresentation {
    ItemStack targetStack;

    @Override
    public String getDefaultTableKey() {
        return "itemStack";
    }

    @Override
    public void fillLuaTable(LuaTable table) {
        super.fillLuaTable(table);
        realFillTable(table, () -> targetStack);
    }

    public static LuaTable getTable(ItemStack stack) {
        LuaTable newTable = new LuaTable();
        realFillTable(newTable, () -> stack);
        return newTable;
    }

    private static void realFillTable(LuaTable table, Supplier<ItemStack> stack) {
        if (stack == null) return;

        table.set("getItemType", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                ItemStack getStack = stack.get();
                if (getStack == null)
                    return NIL;

                if (getStack == ItemStack.EMPTY) return LuaString.valueOf("EMPTY");
                
                return LuaString.valueOf(Registry.ITEM.getId(getStack.getItem()).toString());
            }
        });
        
        table.set("getData", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                ItemStack getStack = stack.get();
                if (getStack == null || getStack == ItemStack.EMPTY)
                    return NIL;
                
                return NbtTagRepresentation.fromTag(getStack.getTag());
            }
        });
    }
}
