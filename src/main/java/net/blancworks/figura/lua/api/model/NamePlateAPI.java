package net.blancworks.figura.lua.api.model;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.NamePlateData;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class NamePlateAPI {
    public static Identifier getID() {
        return new Identifier("default", "nameplate");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new NamePlateTable(script));
    }
    private static class NamePlateTable extends ScriptLocalAPITable {
        NamePlateData data;
        public NamePlateTable(CustomScript script) {
            super(script);
            this.data = script.playerData.nameplate;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();
            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaUtils.getTableFromVector3f(data.position);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    data.position = new Vector3f(
                            fas.getFloat(0),
                            fas.getFloat(1),
                            fas.getFloat(2)
                    );

                    return NIL;
                }
            });
            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(data.enabled);
                }
            });
            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        data.enabled = false;
                        return NIL;
                    }

                    data.enabled = arg.checkboolean();

                    return NIL;
                }
            });
            ret.set("setText", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        data.text = "%n";
                        return NIL;
                    }
                    data.text = arg.checkjstring();
                    return NIL;
                }
            });
            ret.set("getText", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(data.text);
                }
            });
            ret.set("setColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        data.rgb = -1;
                        return NIL;
                    }
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg.checktable());
                    data.rgb = ((Math.round(fas.getFloat(0)) << 16) & 0xFF) | ((Math.round(fas.getFloat(1)) << 8) & 0xFF) | (Math.round(fas.getFloat(2)) & 0xFF);
                    return NIL;
                }
            });
            ret.set("getColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable val = new LuaTable();
                    val.set("r", data.rgb >> 16 & 0xFF);
                    val.set("g", data.rgb >> 8 & 0xFF);
                    val.set("b", data.rgb & 0xFF);
                    return val;
                }
            });
            // TODO: use a custom type instead. This is less user-friendly, but it works.
            ret.set("setProperties", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (!arg.isnumber()) {
                        return NIL;
                    }
                    byte val = arg.tobyte();
                    data.bold = (val & 0b00000001) == 0b000000001;
                    data.italic = (val & 0b00000010) == 0b000000010;
                    data.underlined = (val & 0b00000100) == 0b000000100;
                    data.strikethrough = (val & 0b00001000) == 0b000001000;
                    data.obfuscated = (val & 0b00010000) == 0b00010000;
                    data.decorations_disabled = (val & 0b10000000) == 0b10000000;
                    return NIL;
                }
            });
            ret.set("getProperties", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf((data.decorations_disabled ? 0b10000000 : 0) |
                            (data.obfuscated ? 0b00010000 : 0) |
                            (data.strikethrough ? 0b00001000 : 0) |
                            (data.underlined ? 0b00000100 : 0) |
                            (data.italic ? 0b00000010 : 0) |
                            (data.bold ? 0b00000001 : 0));
                }
            });
            return ret;
        }
    }
}
