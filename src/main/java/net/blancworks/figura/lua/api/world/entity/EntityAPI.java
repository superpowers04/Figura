package net.blancworks.figura.lua.api.world.entity;

import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Iterator;

public class EntityAPI {


    public static class EntityLuaAPITable<T extends Entity> extends ReadOnlyLuaTable {

        public T targetEntity;

        public LuaString typeString;
        
        public EntityLuaAPITable(T targetEntity) {
            this.targetEntity = targetEntity;
        }


        public LuaTable getTable() {
            
            typeString = LuaString.valueOf(Registry.ENTITY_TYPE.getId(targetEntity.getType()).toString());
            
            return new LuaTable() {{

                set("getPos", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaUtils.getTableFromVec3d(targetEntity.getPos());
                    }
                });

                set("getRot", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        LuaTable t = new LuaTable();
                        LuaValue pitch = LuaNumber.valueOf(targetEntity.pitch);
                        LuaValue yaw = LuaNumber.valueOf(targetEntity.yaw);
                        
                        t.set("pitch", pitch);
                        t.set("yaw", yaw);

                        t.set(1, pitch);
                        t.set(2, yaw);

                        return t;
                    }
                });
                
                set("getType", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return typeString;
                    }
                });

                set("getVelocity", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaUtils.getTableFromVec3d(targetEntity.getVelocity());
                    }
                });

                set("getLookDir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaUtils.getTableFromVec3d(targetEntity.getRotationVector());
                    }
                });

                set("getUUID", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaString.valueOf(targetEntity.getEntityName());
                    }
                });

                set("getFireTicks", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.getFireTicks());
                    }
                });

                set("getAir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.getAir());
                    }
                });

                set("getMaxAir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.getMaxAir());
                    }
                });

                set("getAirPercentage", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.getAir() / targetEntity.getMaxAir());
                    }
                });

                set("getWorldName", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        if (targetEntity == null) return NIL;
                        World w = targetEntity.world;

                        return LuaString.valueOf(w.getRegistryKey().getValue().toString());
                    }
                });
                
                set("getEquipmentItem", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        int index = arg.checkint() - 1;
                        ItemStack stack = retrieveItemByIndex(targetEntity.getItemsEquipped(), index);
                        return ItemStackAPI.getTable(stack);
                    }
                });

                set("getAnimation", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        if (targetEntity == null) return NIL;

                        EntityPose p = targetEntity.getPose();

                        if (p == null)
                            return NIL;

                        return LuaString.valueOf(p.name());
                    }
                });
                
            }};
        }


        private static <T> T retrieveItemByIndex(Iterable<T> iterable, int index) {

            if (iterable == null || index < 0) {

                return null;
            }

            int cursor = 0;

            Iterator<T> iterator = iterable.iterator();

            while (cursor < index && iterator.hasNext()) {

                iterator.next();
                cursor++;
            }

            return cursor == index && iterator.hasNext() ? iterator.next() : null;
        }

    }
}
