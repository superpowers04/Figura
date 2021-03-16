package net.blancworks.figura.trust;

import net.blancworks.figura.trust.settings.PermissionSetting;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

//Contains trust settings and permissions
//Used in players and trust groups
public class TrustContainer {

    public Text nameText;

    private Identifier parentIdentifier = null;
    private Identifier identifier;
    //The set of permissions contained/modified by this trust container.
    public HashMap<Identifier, PermissionSetting> permissionSet = new HashMap<Identifier, PermissionSetting>();

    //Used for UI
    public boolean displayChildren = true;


    public TrustContainer(Identifier id, Text nameText) {
        this(id, nameText, null);
    }

    public TrustContainer(Identifier id, Text nameText, @Nullable Identifier parent){
        identifier = id;
        this.nameText = nameText;
        parentIdentifier = parent;
    }

    public PermissionSetting getSetting(Identifier id){
        if(permissionSet.containsKey(id))
            return permissionSet.get(id).getCopy();

        if(parentIdentifier == null)
            return null;

        TrustContainer parent = PlayerTrustManager.getContainer(parentIdentifier);

        if(parent != null && parent != this)
            return parent.getSetting(id).getCopy();

        return null;
    }

    public void setParent(Identifier id) {
        if (identifier != id) {
            parentIdentifier = id;
        }
    }

    public Identifier getParentIdentifier(){
        return parentIdentifier;
    }

    public Identifier getIdentifier(){
        return identifier;
    }

    public void setSetting(PermissionSetting setting){
        PermissionSetting currSetting = getSetting(setting.id);

        if(currSetting.isDifferent(setting))
            permissionSet.put(setting.id, setting);
    }

    public void reset(){
        permissionSet.clear();
    }

}
