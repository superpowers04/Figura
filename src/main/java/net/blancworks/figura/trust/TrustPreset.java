package net.blancworks.figura.trust;

import net.blancworks.figura.trust.settings.PermissionSetting;

import java.util.HashMap;
import java.util.Map;

public class TrustPreset {
    public String name;
    public HashMap<String, PermissionSetting> permissions = new HashMap<String, PermissionSetting>();
    
    //True if the UI in the trust menu should render users in this category.
    public boolean displayList = true;
    
    public TrustPreset getCopy(){
        TrustPreset newSet = new TrustPreset();
        newSet.name = name + " - COPY";

        for (Map.Entry<String, PermissionSetting> entry : permissions.entrySet()) {
            newSet.permissions.put(entry.getKey(), entry.getValue().getCopy(null));
        }
        return newSet;
    }
}
