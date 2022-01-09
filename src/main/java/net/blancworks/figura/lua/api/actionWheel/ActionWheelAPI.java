package net.blancworks.figura.lua.api.actionWheel;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.gui.ActionWheel;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ActionWheelAPI {
    public static final String SLOT_1 = "SLOT_1";
    public static final String SLOT_2 = "SLOT_2";
    public static final String SLOT_3 = "SLOT_3";
    public static final String SLOT_4 = "SLOT_4";
    public static final String SLOT_5 = "SLOT_5";
    public static final String SLOT_6 = "SLOT_6";
    public static final String SLOT_7 = "SLOT_7";
    public static final String SLOT_8 = "SLOT_8";

    public static Identifier getID() {
        return new Identifier("default", "action_wheel");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            set(SLOT_1, ActionWheelCustomization.getTableForPart(SLOT_1, script));
            set(SLOT_2, ActionWheelCustomization.getTableForPart(SLOT_2, script));
            set(SLOT_3, ActionWheelCustomization.getTableForPart(SLOT_3, script));
            set(SLOT_4, ActionWheelCustomization.getTableForPart(SLOT_4, script));
            set(SLOT_5, ActionWheelCustomization.getTableForPart(SLOT_5, script));
            set(SLOT_6, ActionWheelCustomization.getTableForPart(SLOT_6, script));
            set(SLOT_7, ActionWheelCustomization.getTableForPart(SLOT_7, script));
            set(SLOT_8, ActionWheelCustomization.getTableForPart(SLOT_8, script));

            set("setLeftSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int size = arg.checkint();
                    script.actionWheelLeftSize = MathHelper.clamp(size, 1, 4);
                    return NIL;
                }
            });

            set("setRightSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int size = arg.checkint();
                    script.actionWheelRightSize = MathHelper.clamp(size, 1, 4);
                    return NIL;
                }
            });

            set("getLeftSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.actionWheelLeftSize);
                }
            });

            set("getRightSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.actionWheelRightSize);
                }
            });

            set("getSelectedSlot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    int selected = ActionWheel.selectedSlot;
                    return selected == -1 ? NIL : LuaValue.valueOf(selected + 1);
                }
            });

            set("isOpen", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(FiguraMod.ACTION_WHEEL_BUTTON.isPressed() && ActionWheel.enabled);
                }
            });

            set("runAction", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    ActionWheel.play();
                    return NIL;
                }
            });

        }});
    }
}
