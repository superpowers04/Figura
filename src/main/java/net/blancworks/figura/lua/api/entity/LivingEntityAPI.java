package net.blancworks.figura.lua.api.entity;

import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.function.Supplier;

public class LivingEntityAPI {
    public static class LivingEntityAPITable<T extends LivingEntity> extends EntityAPI.EntityLuaAPITable<T> {

        public LivingEntityAPITable(Supplier<T> targetEntity) {
            super(targetEntity);
        }

        @Override
        public void setTable() {
            super.setTable();

            set("getBodyYaw", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    float bodyYaw = targetEntity.get().bodyYaw;

                    if (!arg.isnil())
                        bodyYaw = MathHelper.lerp(arg.tofloat(), targetEntity.get().prevBodyYaw, bodyYaw);

                    return LuaNumber.valueOf(bodyYaw);
                }
            });

            set("getHealth", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getHealth());
                }
            });

            set("getMaxHealth", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getMaxHealth());
                }
            });

            set("getHealthPercentage", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getHealth() / targetEntity.get().getMaxHealth());
                }
            });

            set("getArmor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getArmor());
                }
            });

            set("getDeathTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().deathTime);
                }
            });

            set("getStatusEffectTypes", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable effects = new LuaTable();

                    int i = 1;
                    for (StatusEffectInstance inst : targetEntity.get().getStatusEffects()) {
                        effects.set(i, LuaString.valueOf(Registry.STATUS_EFFECT.getId(inst.getEffectType()).toString()));
                        i++;
                    }

                    return effects;
                }
            });

            set("getStatusEffect", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Identifier effectId = Identifier.tryParse(arg.checkjstring());
                    if (effectId == null)
                        return NIL;

                    StatusEffect statusEffect = Registry.STATUS_EFFECT.get(effectId);

                    if (!targetEntity.get().hasStatusEffect(statusEffect))
                        return NIL;

                    LuaTable effect = new LuaTable();
                    StatusEffectInstance instance = targetEntity.get().getStatusEffect(statusEffect);
                    effect.set("duration", instance.getDuration());
                    effect.set("amplifier", instance.getAmplifier());

                    return effect;
                }
            });

            set("getStuckArrowCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getStuckArrowCount());
                }
            });

            set("getStingerCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getStingerCount());
                }
            });

            set("isLeftHanded", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetEntity.get().getMainArm() == Arm.LEFT);
                }
            });

            set("isUsingItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetEntity.get().isUsingItem());
                }
            });

            set("getActiveHand", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Hand hand = targetEntity.get().getActiveHand();
                    return hand == null ? NIL : LuaString.valueOf(hand.toString());
                }
            });

            set("getActiveItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    ItemStack targetStack = targetEntity.get().getActiveItem();
                    if (targetStack.equals(ItemStack.EMPTY))
                        return NIL;

                    return ItemStackAPI.getTable(targetStack);
                }
            });

            set("isClimbing", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(targetEntity.get().isClimbing());
                }
            });
        }
    }
}
