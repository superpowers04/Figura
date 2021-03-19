package net.blancworks.figura.models.lua.representations;

import net.blancworks.figura.models.lua.CustomScript;
import org.luaj.vm2.Globals;

///A base class used to encompass most systems that allow a lua script to interface with java code
public class LuaRepresentation {
    
    public CustomScript script;
    public Globals scriptGlobals;
    public LuaRepresentation(CustomScript targetScript){
        script = targetScript;
        scriptGlobals = targetScript.scriptGlobals;
    }
    
    public void tick(){}
    public void onRender(){}
}
