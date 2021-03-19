package net.blancworks.figura.models.lua.representations;

import net.blancworks.figura.models.lua.CustomScript;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;

///A base class used to encompass most systems that allow a lua script to interface with java code
public class LuaRepresentation {
    
    public CustomScript script;
    public Globals scriptGlobals;
    
    public LuaRepresentation(){
        
    }
    
    public LuaRepresentation(CustomScript targetScript){
        script = targetScript;
        scriptGlobals = targetScript.scriptGlobals;
        
        LuaTable table = new LuaTable();
        fillLuaTable(table);
        scriptGlobals.set(getDefaultTableKey(), table);
    }


    
    //Gets the key for the table entry in the script globals
    public String getDefaultTableKey(){ return "NIL"; }
    //Fills out the provided lua table with the values for this representation
    public void fillLuaTable(LuaTable table){}

    //Gets all the references that this representation needs
    //Happens once per tick, cached to save resources.
    public void getReferences(){}
    
    public void tick(){}
    public void onRender(){}
}
