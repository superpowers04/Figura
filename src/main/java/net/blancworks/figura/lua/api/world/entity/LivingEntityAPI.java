package net.blancworks.figura.lua.api.world.entity;

import net.minecraft.entity.LivingEntity;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LivingEntityAPI {
    
    
    public static class LivingEntityAPITable<T extends LivingEntity> extends EntityAPI.EntityLuaAPITable<T> {

        public LivingEntityAPITable(T targetEntity) {
            super(targetEntity);
        }

        @Override
        public LuaTable getTable() {
            LuaTable superTable = super.getTable();
            
            superTable.set("getHealth", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.getHealth());
                }
            });

            superTable.set("getMaxHealth", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.getMaxHealth());
                }
            });

            superTable.set("getHealthPercentage", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.getHealth() / targetEntity.getMaxHealth());
                }
            });

            superTable.set("getArmor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.getArmor());
                }
            });

            superTable.set("getDeathTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.deathTime);
                }
            });
            
            return superTable;
        }
    }
}
