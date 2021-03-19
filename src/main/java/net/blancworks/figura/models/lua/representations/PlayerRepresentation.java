package net.blancworks.figura.models.lua.representations;

import net.blancworks.figura.models.lua.CustomScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;


public class PlayerRepresentation extends LuaRepresentation {
    
    
    public PlayerRepresentation(CustomScript targetScript) {
        super(targetScript);

        LuaTable playerTable = new LuaTable();
        
        playerTable.set("getPos", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                PlayerEntity pEnt = getPlayerEntity();
                if(pEnt == null)
                    return NIL;
                
                LuaTable ret = new LuaTable();
                ret.set(1, LuaNumber.valueOf(pEnt.getX()));
                ret.set(2, LuaNumber.valueOf(pEnt.getY()));
                ret.set(3, LuaNumber.valueOf(pEnt.getZ()));
                return ret;
            }
        });
        
        playerTable.set("getLookDir", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                PlayerEntity pEnt = getPlayerEntity();
                if(pEnt == null)
                    return NIL;

                LuaTable ret = new LuaTable();
                Vec3d look = pEnt.getRotationVector();
                ret.set(1, LuaNumber.valueOf(look.getX()));
                ret.set(2, LuaNumber.valueOf(look.getY()));
                ret.set(3, LuaNumber.valueOf(look.getZ()));
                return ret;
            }
        });
        
        playerTable.set("getAnimation", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                PlayerEntity pEnt = getPlayerEntity();
                if(pEnt == null)
                    return NIL;
                
                EntityPose p = getPlayerEntity().getPose();
                
                if(p == null)
                    return NIL;
                
                return LuaString.valueOf(p.name());
            }
        });
        
        playerTable.set("getWorldName", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                PlayerEntity pEnt = getPlayerEntity();
                if(pEnt == null)
                    return NIL;

                World w = MinecraftClient.getInstance().world;
                
                return LuaString.valueOf(w.getRegistryKey().getValue().toString());
            }
        });
        
        
        targetScript.scriptGlobals.set("player", playerTable);
    }
    
    public PlayerEntity getPlayerEntity(){
        return script.playerData.getEntityIfLoaded();
    }
}
