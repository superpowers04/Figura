package net.blancworks.figura.lua.api.world.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LivingEntityAPI {
    
    
    public static class LivingEntityAPITable<T extends LivingEntity> extends EntityAPI.EntityLuaAPITable<T> {

        public LivingEntityAPITable(T targetEntity) {
            super(targetEntity);
        }

        @Override
        public LuaTable getTable() {
            LuaTable superTable = super.getTable();

            superTable.set("getBodyYaw", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.bodyYaw);
                }
            });
            
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

            superTable.set("getStatusEffectTypes", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable effects = new LuaTable();

                    int i = 1;
                    for (StatusEffectInstance inst : targetEntity.getStatusEffects()) {
                        effects.set(i, LuaString.valueOf(Registry.STATUS_EFFECT.getId(inst.getEffectType()).toString()));
                    }

                    return effects;
                }
            });

            superTable.set("getStatusEffect", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Identifier effectId = Identifier.tryParse(arg.checkjstring());
                    if(effectId == null)
                        return NIL;

                    StatusEffect statusEffect = Registry.STATUS_EFFECT.get(effectId);

                    if (!targetEntity.hasStatusEffect(statusEffect))
                        return NIL;

                    LuaTable effect = new LuaTable();
                    StatusEffectInstance instance = targetEntity.getStatusEffect(statusEffect);
                    effect.set("duration", instance.getDuration());
                    effect.set("amplifier", instance.getAmplifier());

                    return effect;
                }
            });

            superTable.set("isSneaky", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.isSneaky());
                }
            });
            
            return superTable;
        }
    }
}
