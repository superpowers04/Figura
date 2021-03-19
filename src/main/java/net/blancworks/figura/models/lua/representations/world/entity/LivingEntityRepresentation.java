package net.blancworks.figura.models.lua.representations.world.entity;

import net.blancworks.figura.models.lua.CustomScript;
import net.minecraft.entity.LivingEntity;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LivingEntityRepresentation<T extends LivingEntity> extends EntityRepresentation<T> {

    public LivingEntityRepresentation(CustomScript customScript) {
        super(customScript);
    }

    @Override
    public String getDefaultTableKey() {
        return "living_" + super.getDefaultTableKey();
    }

    @Override
    public void fillLuaTable(LuaTable table) {
        super.fillLuaTable(table);
        
        table.set("getHealth", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if(targetEntity == null) return NIL;
                return LuaNumber.valueOf(targetEntity.getHealth());
            }
        });

        table.set("getMaxHealth", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if(targetEntity == null) return NIL;
                return LuaNumber.valueOf(targetEntity.getMaxHealth());
            }
        });

        table.set("getDeathTime", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if(targetEntity == null) return NIL;
                return LuaNumber.valueOf(targetEntity.deathTime);
            }
        });

        table.set("getArmor", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if(targetEntity == null) return NIL;
                return LuaNumber.valueOf(targetEntity.getArmor());
            }
        });
    }
}
