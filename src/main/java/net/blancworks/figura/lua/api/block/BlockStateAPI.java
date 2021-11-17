package net.blancworks.figura.lua.api.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.mixin.AbstractBlockAccessorMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.lang.reflect.Field;
import java.util.List;

public class BlockStateAPI {

    public static Identifier getID() {
        return new Identifier("default", "block_state");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("createBlock", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    try {
                        BlockState block = BlockStateArgumentType.blockState().parse(new StringReader(arg1.checkjstring())).getBlockState();
                        return getTable(block, MinecraftClient.getInstance().world, LuaVector.checkOrNew(arg2).asBlockPos());
                    } catch (CommandSyntaxException e) {
                        throw new LuaError("Could not create blockstate\n" + e.getMessage());
                    }
                }
            });

        }});
    }

    public static ReadOnlyLuaTable getTable(BlockState state, World world, BlockPos pos) {
        return new BlockStateTable(state, world, pos).getTable();
    }

    private static LuaValue voxelShapeToTable(VoxelShape shape) {
        ReadOnlyLuaTable shapes = new ReadOnlyLuaTable();
        List<Box> boxes = shape.getBoundingBoxes();
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            shapes.javaRawSet(i+1, new LuaVector((float)box.minX,(float)box.minY,(float)box.minZ,(float)box.maxX,(float)box.maxY,(float)box.maxZ));
        }
        return shapes;
    }

    private static class BlockStateTable extends ReadOnlyLuaTable {
        private final BlockState state;
        private final World world;
        private BlockPos blockPos;

        public BlockStateTable(BlockState state, World world, BlockPos blockPos) {
            this.state = state;
            this.world = world;
            this.blockPos = blockPos;
        }

        public ReadOnlyLuaTable getTable() {
            ReadOnlyLuaTable tbl = (ReadOnlyLuaTable) NBTAPI.fromTag(NbtHelper.fromBlockState(state));

            tbl.javaRawSet(LuaValue.valueOf("state"), LuaValue.userdataOf(state));

            tbl.javaRawSet(LuaValue.valueOf("setPos"), new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    blockPos = LuaVector.checkOrNew(arg).asBlockPos();
                    return NIL;
                }
            });

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
                    return LuaValue.valueOf(state.isSolidBlock(world, blockPos));
                }
            });

            tbl.javaRawSet(LuaValue.valueOf("isFullCube"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.isFullCube(world, blockPos));
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
                    return LuaValue.valueOf(state.hasEmissiveLighting(world, blockPos));
                }
            });

            tbl.javaRawSet(LuaValue.valueOf("isTranslucent"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.isTranslucent(world, blockPos));
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
                    return LuaValue.valueOf(state.getOpacity(world, blockPos));
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
                    return LuaValue.valueOf(state.getHardness(world, blockPos));
                }
            });

            tbl.javaRawSet(LuaValue.valueOf("getComparatorOutput"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.getComparatorOutput(world, blockPos));
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

            tbl.javaRawSet(LuaValue.valueOf("getCollisionShape"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return voxelShapeToTable(state.getCollisionShape(world, blockPos));
                }
            });

            tbl.javaRawSet(LuaValue.valueOf("getOutlineShape"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return voxelShapeToTable(state.getOutlineShape(world, blockPos));
                }
            });

            tbl.javaRawSet(LuaValue.valueOf("getSoundGroup"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    ReadOnlyLuaTable stateGroup = new ReadOnlyLuaTable();
                    BlockSoundGroup snd = state.getSoundGroup();

                    stateGroup.javaRawSet(LuaValue.valueOf("pitch"), LuaValue.valueOf(snd.getPitch()));
                    stateGroup.javaRawSet(LuaValue.valueOf("volume"), LuaValue.valueOf(snd.getVolume()));
                    stateGroup.javaRawSet(LuaValue.valueOf("break"), LuaValue.valueOf(snd.getBreakSound().getId().toString()));
                    stateGroup.javaRawSet(LuaValue.valueOf("fall"), LuaValue.valueOf(snd.getFallSound().getId().toString()));
                    stateGroup.javaRawSet(LuaValue.valueOf("hit"), LuaValue.valueOf(snd.getHitSound().getId().toString()));
                    stateGroup.javaRawSet(LuaValue.valueOf("plate"), LuaValue.valueOf(snd.getPlaceSound().getId().toString()));
                    stateGroup.javaRawSet(LuaValue.valueOf("step"), LuaValue.valueOf(snd.getStepSound().getId().toString()));

                    return stateGroup;
                }
            });

            tbl.javaRawSet(LuaValue.valueOf("toStateString"), new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(state.toString());
                }
            });

            return tbl;
        }
    }
}