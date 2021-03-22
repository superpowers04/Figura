package net.blancworks.figura.lua.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public class ReadOnlyLuaTable extends LuaTable {
    public ReadOnlyLuaTable(){
        presize(0, 0);
    }
    
    public ReadOnlyLuaTable(LuaValue table) {
        setTable(table);
    }
    
    public void setTable(LuaValue table){
        presize(table.length(), 0);
        for (Varargs n = table.next(LuaValue.NIL); !n.arg1().isnil(); n = table
                .next(n.arg1())) {
            LuaValue key = n.arg1();
            LuaValue value = n.arg(2);
            super.rawset(key, value.istable() ? new ReadOnlyLuaTable(value) : value);
        }
    }
    
    public LuaValue setmetatable(LuaValue metatable) { return error("table is read-only"); }
    public void set(int key, LuaValue value) { error("table is read-only"); }
    public void rawset(int key, LuaValue value) { error("table is read-only"); }
    public void rawset(LuaValue key, LuaValue value) { error("table is read-only"); }
    public LuaValue remove(int pos) { return error("table is read-only"); }
    
    public void javaSet(int key, LuaValue value) {
        super.set(key, value);
    }
    
    public void javaRawSet(int key, LuaValue value) {
        super.rawset(key, value);
    }
    
    public void javaRawSet(LuaValue key, LuaValue value) {
        super.rawset(key, value);
    }
    
    public void javaRemove(int pos) {
        super.remove(pos);
    }
}