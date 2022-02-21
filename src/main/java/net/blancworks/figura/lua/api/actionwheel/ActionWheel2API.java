package net.blancworks.figura.lua.api.actionwheel;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.gui.NewActionWheel;
import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ActionWheel2API {

    public static Identifier getID() {
        return new Identifier("default", "new_action_wheel");
    }

    public static LuaTable getForScript(CustomScript script) {
       return new LuaTable() {{
           set("addSlot", new OneArgFunction() {
               @Override
               public LuaValue call(LuaValue arg) {
                   if (script.newActionWheelSlots.size() >= 32)
                       throw new LuaError("Max 32 slots!");

                   String key = arg.checkjstring();
                   LuaTable val = ActionWheelCustomization.getTableForPart(key, script);

                   set(key, val);
                   script.newActionWheelSlots.put(key, new ActionWheelCustomization());

                   return NIL;
               }
           });

           set("getSize", new ZeroArgFunction() {
               @Override
               public LuaValue call() {
                   return LuaValue.valueOf(script.newActionWheelSlots.size());
               }
           });

           set("clear", new ZeroArgFunction() {
               @Override
               public LuaValue call() {
                   script.newActionWheelSlots.clear();
                   return NIL;
               }
           });

           set("getSelectedSlot", new ZeroArgFunction() {
               @Override
               public LuaValue call() {
                   String selected = NewActionWheel.selectedSlot;
                   return selected == null ? NIL : LuaValue.valueOf(selected);
               }
           });

           set("isOpen", new ZeroArgFunction() {
               @Override
               public LuaValue call() {
                   return LuaValue.valueOf(FiguraMod.ACTION_WHEEL_BUTTON.isPressed() && NewActionWheel.enabled);
               }
           });

           set("runAction", new ZeroArgFunction() {
               @Override
               public LuaValue call() {
                   NewActionWheel.play();
                   return NIL;
               }
           });
       }};
    }
}
