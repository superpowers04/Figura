package net.blancworks.figura.lua.api.model;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.NamePlateData;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
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
            this.data = script.nameplate;
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

            ret.set("setChatText", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        data.chatText = "%n";
                        return NIL;
                    }
                    data.chatText = arg.checkjstring();
                    return NIL;
                }
            });
            ret.set("getChatText", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(data.chatText);
                }
            });

            ret.set("setListText", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        data.listText = "%n";
                        return NIL;
                    }
                    data.listText = arg.checkjstring();
                    return NIL;
                }
            });
            ret.set("getListText", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(data.listText);
                }
            });


            ret.set("setColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        data.RGB = -1;
                        return NIL;
                    }
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg.checktable());
                    data.RGB = ((Math.round(fas.getFloat(0) * 255) & 0xFF) << 16) | ((Math.round(fas.getFloat(1) * 255) & 0xFF) << 8) | (Math.round(fas.getFloat(2) * 255) & 0xFF);
                    return NIL;
                }
            });
            ret.set("getColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable val = new LuaTable();
                    val.set("r", (data.RGB >> 16 & 0xFF) / 255.0f);
                    val.set("g", (data.RGB >> 8 & 0xFF) / 255.0f);
                    val.set("b", (data.RGB & 0xFF) / 255.0f);
                    return val;
                }
            });

            ret.set("setChatColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        data.chatRGB = -1;
                        return NIL;
                    }
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg.checktable());
                    data.chatRGB = ((Math.round(fas.getFloat(0) * 255) & 0xFF) << 16) | ((Math.round(fas.getFloat(1) * 255) & 0xFF) << 8) | (Math.round(fas.getFloat(2) * 255) & 0xFF);
                    return NIL;
                }
            });
            ret.set("getChatColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable val = new LuaTable();
                    val.set("r", (data.chatRGB >> 16 & 0xFF) / 255.0f);
                    val.set("g", (data.chatRGB >> 8 & 0xFF) / 255.0f);
                    val.set("b", (data.chatRGB & 0xFF) / 255.0f);
                    return val;
                }
            });

            ret.set("setListColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        data.listRGB = -1;
                        return NIL;
                    }
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg.checktable());
                    data.listRGB = ((Math.round(fas.getFloat(0) * 255) & 0xFF) << 16) | ((Math.round(fas.getFloat(1) * 255) & 0xFF) << 8) | (Math.round(fas.getFloat(2) * 255) & 0xFF);
                    return NIL;
                }
            });
            ret.set("getListColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable val = new LuaTable();
                    val.set("r", (data.listRGB >> 16 & 0xFF) / 255.0f);
                    val.set("g", (data.listRGB >> 8 & 0xFF) / 255.0f);
                    val.set("b", (data.listRGB & 0xFF) / 255.0f);
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
                    data.textProperties = arg.tobyte();
                    return NIL;
                }
            });
            ret.set("getProperties", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(data.textProperties);
                }
            });

            ret.set("setChatProperties", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (!arg.isnumber()) {
                        return NIL;
                    }
                    data.chatTextProperties = arg.tobyte();
                    return NIL;
                }
            });
            ret.set("getChatProperties", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(data.chatTextProperties);
                }
            });

            ret.set("setListProperties", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (!arg.isnumber()) {
                        return NIL;
                    }
                    data.listTextProperties = arg.tobyte();
                    return NIL;
                }
            });
            ret.set("getListProperties", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(data.listTextProperties);
                }
            });


            return ret;
        }
    }
}
