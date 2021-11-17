package net.blancworks.figura.lua.api.world.block;

import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.mixin.AbstractBlockAccessorMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class BlockStateAPI {

    public static final HashMap<BlockState, ReadOnlyLuaTable> STATE_CACHE = new HashMap<>();
    public static final HashMap<BlockState, ReadOnlyLuaTable> STATE_SOUND_GROUPS = new HashMap<>();

    public static ReadOnlyLuaTable getTable(BlockState state, World world) {
        if (STATE_CACHE.containsKey(state)) return STATE_CACHE.get(state);

        ReadOnlyLuaTable tbl = (ReadOnlyLuaTable) NBTAPI.fromTag(NbtHelper.fromBlockState(state));

        tbl.javaRawSet(LuaValue.valueOf("getBlockTags"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                LuaTable table = new LuaTable();
                BlockTags.getTagGroup().getTagsFor(state.getBlock()).forEach(identifier -> table.insert(0, LuaValue.valueOf(String.valueOf(identifier))));

                return new ReadOnlyLuaTable(table);
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getMaterial"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                for (Field field : Material.class.getFields()) {
                    try {
                        if (field.get(null) == state.getMaterial())
                            return LuaValue.valueOf(field.getName());
                    } catch (Exception ignored) {
                    }
                }

                return NIL;
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getMapColor"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getBlock().getDefaultMapColor().color);
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("isSolidBlock"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(state.isSolidBlock(world, LuaVector.checkOrNew(arg).asBlockPos()));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("isFullCube"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(state.isFullCube(world, LuaVector.checkOrNew(arg).asBlockPos()));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("hasBlockEntity"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.hasBlockEntity());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("isOpaque"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.isOpaque());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("hasEmissiveLighting"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(state.hasEmissiveLighting(world, LuaVector.checkOrNew(arg).asBlockPos()));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("isTranslucent"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(state.isTranslucent(world, LuaVector.checkOrNew(arg).asBlockPos()));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("emitsRedstonePower"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.emitsRedstonePower());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getOpacity"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(state.getOpacity(world, LuaVector.checkOrNew(arg).asBlockPos()));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getLuminance"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getLuminance());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getHardness"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(state.getHardness(world, LuaVector.checkOrNew(arg).asBlockPos()));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getComparatorOutput"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(state.getComparatorOutput(world, LuaVector.checkOrNew(arg).asBlockPos()));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getSlipperiness"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getBlock().getSlipperiness());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getVelocityMultiplier"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getBlock().getVelocityMultiplier());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getJumpVelocityMultiplier"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getBlock().getJumpVelocityMultiplier());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getBlastResistance"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getBlock().getBlastResistance());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("isCollidable"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(((AbstractBlockAccessorMixin) state.getBlock()).isCollidable());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getCollisionShape"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                BlockPos pos = LuaVector.checkOrNew(arg).asBlockPos();
                return voxelShapeToTable(state.getCollisionShape(world, pos));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getOutlineShape"), new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                BlockPos pos = LuaVector.checkOrNew(arg).asBlockPos();
                return voxelShapeToTable(state.getOutlineShape(world, pos));
            }
        });

        tbl.javaRawSet(LuaString.valueOf("getSoundGroup"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (STATE_SOUND_GROUPS.containsKey(state)) return STATE_SOUND_GROUPS.get(state);
                ReadOnlyLuaTable stateGroup = new ReadOnlyLuaTable();
                BlockSoundGroup snd = state.getSoundGroup();

                stateGroup.javaRawSet(LuaString.valueOf("pitch"), LuaString.valueOf(snd.getPitch()));
                stateGroup.javaRawSet(LuaString.valueOf("volume"), LuaString.valueOf(snd.getVolume()));
                stateGroup.javaRawSet(LuaString.valueOf("break"), LuaString.valueOf(snd.getBreakSound().getId().toString()));
                stateGroup.javaRawSet(LuaString.valueOf("fall"), LuaString.valueOf(snd.getFallSound().getId().toString()));
                stateGroup.javaRawSet(LuaString.valueOf("hit"), LuaString.valueOf(snd.getHitSound().getId().toString()));
                stateGroup.javaRawSet(LuaString.valueOf("plate"), LuaString.valueOf(snd.getPlaceSound().getId().toString()));
                stateGroup.javaRawSet(LuaString.valueOf("step"), LuaString.valueOf(snd.getStepSound().getId().toString()));

                STATE_SOUND_GROUPS.put(state, stateGroup);
                return stateGroup;
            }
        });

        tbl.javaRawSet(LuaString.valueOf("toStateString"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaString.valueOf(state.toString());
            }
        });

        tbl.javaRawSet(LuaString.valueOf("state"), LuaValue.userdataOf(state));

        return tbl;
    }

    public static LuaValue voxelShapeToTable(VoxelShape shape) {
        ReadOnlyLuaTable shapes = new ReadOnlyLuaTable();
        List<Box> boxes = shape.getBoundingBoxes();
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            shapes.javaRawSet(i+1, new LuaVector((float)box.minX,(float)box.minY,(float)box.minZ,(float)box.maxX,(float)box.maxY,(float)box.maxZ));
        }
        return shapes;
    }

    public static BlockState checkState(LuaValue arg) {
        if (arg.get("state").checkuserdata() instanceof BlockState state) {
            return state;
        }
        throw new LuaError("Not a BlockState table!");
    }
}