package net.blancworks.figura.lua.api.world.block;

import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtHelper;

public class BlockStateAPI {
    
    public static ReadOnlyLuaTable getTable(BlockState state){
        return (ReadOnlyLuaTable) NBTAPI.fromTag(NbtHelper.fromBlockState(state));
    }
    
}
