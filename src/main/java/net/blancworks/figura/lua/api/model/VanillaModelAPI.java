package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.mixin.PlayerEntityModelAccessorMixin;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.function.Function;
import java.util.function.Supplier;

public class VanillaModelAPI {

    //Main body accessors
    public static final String VANILLA_HEAD = "HEAD";
    public static final String VANILLA_TORSO = "TORSO";
    public static final String VANILLA_LEFT_ARM = "LEFT_ARM";
    public static final String VANILLA_RIGHT_ARM = "RIGHT_ARM";
    public static final String VANILLA_LEFT_LEG = "LEFT_LEG";
    public static final String VANILLA_RIGHT_LEG = "RIGHT_LEG";

    //Layered accessors
    public static final String VANILLA_HAT = "HAT";
    public static final String VANILLA_JACKET = "JACKET";
    public static final String VANILLA_LEFT_SLEEVE = "LEFT_SLEEVE";
    public static final String VANILLA_RIGHT_SLEEVE = "RIGHT_SLEEVE";
    public static final String VANILLA_LEFT_PANTS = "LEFT_PANTS_LEG";
    public static final String VANILLA_RIGHT_PANTS = "RIGHT_PANTS_LEG";

    //extra parts
    public static final String VANILLA_CAPE = "CAPE";
    public static final String VANILLA_LEFT_EAR = "LEFT_EAR";
    public static final String VANILLA_RIGHT_EAR = "RIGHT_EAR";

    public static Identifier getID() {
        return new Identifier("default", "vanilla_model");
    }

    public static Function<CustomScript, PlayerEntityModel<?>> getCurrModel = (script) -> (PlayerEntityModel<?>) script.avatarData.vanillaModel;

    public static LuaTable getForScript(CustomScript script) {
        return new LuaTable() {{
            set(VANILLA_HEAD, getTableForPart(() -> getCurrModel.apply(script).head, VANILLA_HEAD, script));
            set(VANILLA_TORSO, getTableForPart(() -> getCurrModel.apply(script).body, VANILLA_TORSO, script));

            set(VANILLA_LEFT_ARM, getTableForPart(() -> getCurrModel.apply(script).leftArm, VANILLA_LEFT_ARM, script));
            set(VANILLA_RIGHT_ARM, getTableForPart(() -> getCurrModel.apply(script).rightArm, VANILLA_RIGHT_ARM, script));

            set(VANILLA_LEFT_LEG, getTableForPart(() -> getCurrModel.apply(script).leftLeg, VANILLA_LEFT_LEG, script));
            set(VANILLA_RIGHT_LEG, getTableForPart(() -> getCurrModel.apply(script).rightLeg, VANILLA_RIGHT_LEG, script));

            set(VANILLA_HAT, getTableForPart(() -> getCurrModel.apply(script).hat, VANILLA_HAT, script));
            set(VANILLA_JACKET, getTableForPart(() -> getCurrModel.apply(script).jacket, VANILLA_JACKET, script));

            set(VANILLA_LEFT_SLEEVE, getTableForPart(() -> getCurrModel.apply(script).leftSleeve, VANILLA_LEFT_SLEEVE, script));
            set(VANILLA_RIGHT_SLEEVE, getTableForPart(() -> getCurrModel.apply(script).rightSleeve, VANILLA_RIGHT_SLEEVE, script));

            set(VANILLA_LEFT_PANTS, getTableForPart(() -> getCurrModel.apply(script).leftPants, VANILLA_LEFT_PANTS, script));
            set(VANILLA_RIGHT_PANTS, getTableForPart(() -> getCurrModel.apply(script).rightPants, VANILLA_RIGHT_PANTS, script));

            set(VANILLA_CAPE, getTableForPart(() -> ((PlayerEntityModelAccessorMixin) getCurrModel.apply(script)).getCloak(), VANILLA_CAPE, script));
            set(VANILLA_LEFT_EAR, getTableForPart(() -> ((PlayerEntityModelAccessorMixin) getCurrModel.apply(script)).getEar(), VANILLA_LEFT_EAR, script));
            set(VANILLA_RIGHT_EAR, getTableForPart(() -> ((PlayerEntityModelAccessorMixin) getCurrModel.apply(script)).getEar(), VANILLA_RIGHT_EAR, script));
        }};
    }

    public static LuaTable getTableForPart(Supplier<ModelPart> part, String accessor, CustomScript script) {
        return new ModelPartTable(part, accessor, script).getTable();
    }

    public static class ModelPartTable {
        public float pivotX, pivotY, pivotZ;
        public float pitch, yaw, roll;
        public boolean visible;

        public final Supplier<ModelPart> targetPart;
        public final String accessor;
        public final CustomScript script;

        public ModelPartTable(Supplier<ModelPart> part, String accessor, CustomScript script) {
            this.targetPart = part;
            this.accessor = accessor;
            this.script = script;
            script.vanillaModelPartTables.add(this);
        }

        public LuaTable getTable() {
            LuaTable tbl = VanillaModelPartCustomization.getTableForPart(accessor, script);

            tbl.set("getOriginPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return new LuaVector(pivotX, pivotY, pivotZ);
                }
            });

            tbl.set("getOriginRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return new LuaVector(pitch, yaw, roll);
                }
            });

            tbl.set("getOriginEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(visible);
                }
            });

            tbl.set("isOptionEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    try {
                        return LuaBoolean.valueOf(((PlayerEntity) script.avatarData.lastEntity).isPartVisible(PlayerModelPart.valueOf(accessor)));
                    } catch (Exception ignored) {
                        return NIL;
                    }
                }
            });

            return tbl;
        }

        public void updateFromPart() {
            ModelPart part = targetPart.get();
            pivotX = part.pivotX;
            pivotY = part.pivotY;
            pivotZ = part.pivotZ;

            pitch = part.pitch;
            yaw = part.yaw;
            roll = part.roll;
            visible = part.visible;
        }
    }

    public static boolean isPartSpecial(String part) {
        return part.equals(VANILLA_CAPE) || part.equals(VANILLA_LEFT_EAR) || part.equals(VANILLA_RIGHT_EAR);
    }
}
