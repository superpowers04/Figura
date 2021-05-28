package net.blancworks.figura.lua.api.world.entity;

import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class EntityAPI {

    public static class EntityLuaAPITable<T extends Entity> extends ReadOnlyLuaTable {

        public Supplier<T> targetEntity;

        public LuaString typeString;

        public EntityLuaAPITable(Supplier<T> targetEntity) {
            this.targetEntity = targetEntity;
        }


        public LuaTable getTable() {

            return new LuaTable() {{

                set("getPos", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        Entity e = targetEntity.get();
                        return LuaVector.of(e.getPos());
                    }
                });

                set("getRot", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return new LuaVector(targetEntity.get().pitch, targetEntity.get().yaw);
                    }
                });

                set("getType", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaString.valueOf(Registry.ENTITY_TYPE.getId(targetEntity.get().getType()).toString());
                    }
                });

                set("getVelocity", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaVector.of(targetEntity.get().getVelocity());
                    }
                });

                set("getLookDir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaVector.of(targetEntity.get().getRotationVector());
                    }
                });

                set("getUUID", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaString.valueOf(targetEntity.get().getEntityName());
                    }
                });

                set("getFireTicks", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.get().getFireTicks());
                    }
                });

                set("getAir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.get().getAir());
                    }
                });

                set("getMaxAir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.get().getMaxAir());
                    }
                });

                set("getAirPercentage", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(((float)targetEntity.get().getAir()) / targetEntity.get().getMaxAir());
                    }
                });

                set("getWorldName", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        if (targetEntity == null) return NIL;
                        World w = targetEntity.get().world;

                        return LuaString.valueOf(w.getRegistryKey().getValue().toString());
                    }
                });

                set("getEquipmentItem", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        int index = arg.checkint() - 1;
                        ItemStack stack = retrieveItemByIndex(targetEntity.get().getItemsEquipped(), index);
                        return ItemStackAPI.getTable(stack);
                    }
                });

                set("getAnimation", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        if (targetEntity == null) return NIL;

                        EntityPose p = targetEntity.get().getPose();

                        if (p == null)
                            return NIL;

                        return LuaString.valueOf(p.name());
                    }
                });

                set("getVehicle", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        if (targetEntity.get().getVehicle() == null) return NIL;

                        Entity vehicle = targetEntity.get().getVehicle();

                        if (vehicle instanceof LivingEntity) return new LivingEntityAPI.LivingEntityAPITable(()->(LivingEntity) vehicle).getTable();

                        return new EntityLuaAPITable(()->vehicle).getTable();
                    }
                });

                set("isGrounded", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaBoolean.valueOf(targetEntity.get().isOnGround());
                    }
                });

                set("getEyeHeight", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.get().getEyeHeight(targetEntity.get().getPose()));
                    }
                });

                set("getBoundingBox", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        Entity ent = targetEntity.get();
                        EntityDimensions dims = ent.getDimensions(ent.getPose());
                        float x = dims.width;
                        float y = dims.height;
                        float z = dims.width;

                        return new LuaVector(x, y, z);
                    }
                });

                set("getName", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        Entity ent = targetEntity.get();
                        if (ent.hasCustomName() && ent.getCustomName() != null)
                            return LuaValue.valueOf(ent.getCustomName().getString());
                        else
                            return LuaValue.valueOf(ent.getName().getString());
                    }
                });

                set("getNearbyEntity", new TwoArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1, LuaValue arg2) {

                        Entity ent = targetEntity.get();
                        Vec3d pos = ent.getPos();

                        int distance = arg2.checkint();
                        Vec3d min = pos.subtract(distance, distance, distance);
                        Vec3d max = pos.add(distance, distance, distance);

                        Box box = new Box(min, max);
                        List<Entity> entityList = ent.getEntityWorld().getOtherEntities(null, box, EntityPredicates.EXCEPT_SPECTATOR);

                        try {
                            UUID uuid = UUID.fromString(arg1.checkjstring());
                            for (Entity entity : entityList) {
                                if (entity.getUuid().compareTo(uuid) == 0 && !entity.isInvisible())
                                    return new EntityLuaAPITable(() -> entity).getTable();
                            }
                        }
                        catch (Exception ignored) {
                            return LuaValue.error("malformed UUID");
                        }

                        return NIL;
                    }
                });

                set("getNbtValue", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        String pathArg = arg.checkjstring();

                        String[] path = pathArg.split("\\.");

                        CompoundTag tag = new CompoundTag();
                        targetEntity.get().toTag(tag);

                        Tag current = tag;
                        for (String key : path) {
                            if (current == null)
                                current = tag.get(key);
                            else if (current instanceof CompoundTag)
                                current = ((CompoundTag)current).get(key);
                            else current = null;
                        }

                        if (current == null) return NIL;

                        return NBTAPI.fromTag(current);
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
