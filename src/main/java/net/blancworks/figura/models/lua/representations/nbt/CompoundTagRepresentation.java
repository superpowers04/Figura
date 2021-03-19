package net.blancworks.figura.models.lua.representations.nbt;

import net.minecraft.nbt.CompoundTag;
import org.luaj.vm2.LuaValue;

public class CompoundTagRepresentation extends NbtTagRepresentation {
    
    
    //Experimental, but this creates a CompoundTag that keeps track of values in a way that's persistent, rather than being created & used one time.
    //TODO - Implement this!!!!!
    public static LuaValue getPersistentTable(CompoundTag tag){
     
        return LuaValue.NIL;
    }
}
