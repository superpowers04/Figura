package net.blancworks.figura.lua.api.world.block;

import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.mixin.AbstractBlockAccessorMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.lang.reflect.Field;

public class BlockStateAPI {
    
    public static ReadOnlyLuaTable getTable(BlockState state, World world, BlockPos pos) {
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
                    } catch (Exception ignored) {}
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

        tbl.javaRawSet(LuaValue.valueOf("isSolidBlock"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.isSolidBlock(world, pos));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("isFullCube"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.isFullCube(world, pos));
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

        tbl.javaRawSet(LuaValue.valueOf("hasEmissiveLighting"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.hasEmissiveLighting(world, pos));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("isTranslucent"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.isTranslucent(world, pos));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("emitsRedstonePower"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.emitsRedstonePower());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getOpacity"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getOpacity(world, pos));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getLuminance"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getLuminance());
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getHardness"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getHardness(world, pos));
            }
        });

        tbl.javaRawSet(LuaValue.valueOf("getComparatorOutput"), new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(state.getComparatorOutput(world, pos));
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

        return tbl;
    }
}
