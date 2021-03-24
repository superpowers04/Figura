package net.blancworks.figura.lua.api.world.entity;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

public class PlayerEntityAPI {

    public static Identifier getID() {
        return new Identifier("default", "player");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        World w = MinecraftClient.getInstance().world;
        PlayerEntity ent = w.getPlayerByUuid(script.playerData.playerId);
        if(ent == null)
            return null;
        
        return get(ent);
    }
    
    public static ReadOnlyLuaTable get(PlayerEntity entity){
        PlayerEntityLuaAPITable pentTable = new PlayerEntityLuaAPITable(entity);
        
        return pentTable;
    }

    public static class PlayerEntityLuaAPITable extends LivingEntityAPI.LivingEntityAPITable<PlayerEntity> {
        
        public PlayerEntityLuaAPITable(PlayerEntity targetEntity) {
            super(targetEntity);
            super.setTable(getTable());
        }
        
        public LuaTable getTable(){
            LuaTable superTable = super.getTable();

            superTable.set("getHeldItem", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    
                    int hand = arg.checkint();

                    ItemStack targetStack = null;

                    if (hand == 1)
                        targetStack = targetEntity.getMainHandStack();
                    else if (hand == 2)
                        targetStack = targetEntity.getOffHandStack();
                    else
                        return NIL;

                    if(targetStack.equals(ItemStack.EMPTY))
                        return NIL;
                    
                    LuaTable getItemRepresentation = ItemStackAPI.getTable(targetStack);
                    return getItemRepresentation;
                }
            });
            
            return superTable;
        }
    }

}
