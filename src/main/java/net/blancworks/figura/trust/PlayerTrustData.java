package net.blancworks.figura.trust;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;

import java.util.HashMap;
import java.util.function.Supplier;

public class PlayerTrustData {

    public static HashMap<String, Supplier<PermissionSetting>> permissionRegistry = new HashMap<String, Supplier<PermissionSetting>>();
    public static HashMap<String, TrustPreset> allPresets = new HashMap<>();

    public static void registerPermissionSetting(String key, Supplier<PermissionSetting> supplier) {
        permissionRegistry.put(key, supplier);
    }
    
    public TrustPreset preset;
    public HashMap<String, PermissionSetting> permissions = new HashMap<String, PermissionSetting>();


    public void fromNBT(CompoundTag tag) {
        
        if(preset != null){
            tag.put("preset", StringTag.of(preset.name));
        }

    }

    public void toNBT(CompoundTag tag) {
        if(tag.contains("preset") && allPresets.containsKey("preset")){
            preset = allPresets.get(preset);
        }
    }
    
    //Resets the trust data to the current preset.
    public void reset(){
        permissions.clear();
    }
    
    
    public PermissionSetting getPermission(String key){
        //Get from override permissions first
        if(permissions.containsKey(key)){
            return permissions.get(key);
        }
        //Get from preset next
        if(preset.permissions.containsKey(key)){
            return preset.permissions.get(key);
        }
        return null;
    }
    
    public void setPermissions(String key, PermissionSetting setting){
        permissions.put(key, setting);
    }
}
