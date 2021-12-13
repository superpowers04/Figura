package net.blancworks.figura.lua.api;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import java.util.ArrayList;
import java.util.List;

public class LuaEvent extends LuaTable {


    public String defaultFunctionName;
    private final List<LuaFunction> subscribedFunctions = new ArrayList<>();

    public LuaEvent(String defaultFunctionName) {
        this.defaultFunctionName = defaultFunctionName;

        super.rawset("subscribe", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                subscribe(arg.checkfunction());
                return NIL;
            }
        });

        super.rawset("unsubscribe", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                unsubscribe(arg.checkfunction());
                return NIL;
            }
        });
    }

    //Not allowed to get.
    @Override
    public LuaValue get(LuaValue key) {
        return NIL;
    }

    @Override
    public LuaValue call() {
        for (LuaFunction function : subscribedFunctions) {
            function.call();
        }
        return NIL;
    }

    @Override
    public LuaValue call(LuaValue arg) {
        for (LuaFunction function : subscribedFunctions) {
            function.call(arg);
        }
        return NIL;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        for (LuaFunction function : subscribedFunctions) {
            function.call(arg1, arg2);
        }
        return NIL;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        for (LuaFunction function : subscribedFunctions) {
            function.call(arg1, arg2, arg3);
        }
        return NIL;
    }

    //Subscribes a function to be called with this event
    public void subscribe(LuaFunction function) {
        if (!subscribedFunctions.contains(function))
            subscribedFunctions.add(function);
    }

    //Unsubscribes a function from this event
    public void unsubscribe(LuaFunction function) {
        subscribedFunctions.remove(function);
    }
}
