package net.blancworks.figura.trust;

import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.blancworks.figura.trust.settings.PermissionStringSetting;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

//Contains trust settings and permissions
//Used in players and trust groups
public class TrustContainer {

    public Text nameText;

    private Identifier parentIdentifier;
    private final Identifier identifier;
    //The set of permissions contained/modified by this trust container.
    public HashMap<Identifier, PermissionSetting<?>> permissionSet = new HashMap<>();

    //--UI Related--
    public boolean isHidden = false;
    public boolean displayChildren = true;
    public boolean isLocked = false;

    public TrustContainer(Identifier id, Text nameText) {
        this(id, nameText, null);
    }

    public TrustContainer(Identifier id, Text nameText, @Nullable Identifier parent) {
        identifier = id;
        this.nameText = nameText;
        parentIdentifier = parent;
    }

    /**
     * Gets a setting
     * @deprecated
     * Use the helper functions for types instead of this function.
    **/
    @Deprecated
    public PermissionSetting<?> getSetting(Identifier id) {
        if (permissionSet.containsKey(id))
            return permissionSet.get(id).getCopy();

        if (parentIdentifier == null)
            return null;

        TrustContainer parent = PlayerTrustManager.getContainer(parentIdentifier);

        if (parent != null && parent != this) {
            PermissionSetting<?> ps = parent.getSetting(id);
            return ps.getCopy();
        }

        return null;
    }

    public void setParent(Identifier id) {
        if (identifier != id) {
            parentIdentifier = id;
        }
    }

    public Identifier getParentIdentifier() {
        return parentIdentifier;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public void setSetting(PermissionSetting<?> setting) {
        if (isLocked)
            return;

        PermissionSetting<?> currSetting = getSetting(setting.id);

        if (currSetting != null && currSetting.isDifferent(setting))
            permissionSet.put(setting.id, setting);
    }

    public void resetAll() {
        if (isLocked)
            return;

        permissionSet.clear();
    }

    public void reset(Identifier id) {
        if (isLocked)
            return;

        permissionSet.remove(id);
    }

    public void fromNbt(CompoundTag tag) {

        if (tag.contains("pid")) parentIdentifier = Identifier.tryParse(tag.getString("pid"));

        if (tag.contains("hd"))
            isHidden = tag.getBoolean("hd");
        else
            isHidden = false;
        if (tag.contains("dc"))
            displayChildren = tag.getBoolean("dc");
        else
            displayChildren = true;
        if (tag.contains("lck"))
            isLocked = tag.getBoolean("lck");
        else
            isLocked = false;

        CompoundTag permTag = (CompoundTag) tag.get("perms");

        if (permTag != null) {
            for (Map.Entry<Identifier, PermissionSetting<?>> settingEntry : PlayerTrustManager.permissionSettings.entrySet()) {
                if (permTag.contains(settingEntry.getKey().getPath())) {
                    PermissionSetting<?> newSetting = settingEntry.getValue().getCopy();
                    newSetting.fromNBT(permTag);
                    permissionSet.put(newSetting.id, newSetting);
                }
            }
        }
    }

    public void toNbt(CompoundTag tag) {

        tag.put("id", StringTag.of(identifier.toString()));
        if (parentIdentifier != null)
            tag.put("pid", StringTag.of(parentIdentifier.toString()));

        if (isHidden) tag.put("hd", ByteTag.of((byte) 1));
        if (!displayChildren) tag.put("dc", ByteTag.of((byte) 0));
        if (isLocked) tag.put("lck", ByteTag.of((byte) 1));

        CompoundTag permissions = new CompoundTag();

        for (Map.Entry<Identifier, PermissionSetting<?>> entry : permissionSet.entrySet()) {
            entry.getValue().toNBT(permissions);
        }

        tag.put("perms", permissions);
    }

    //---Helper functions---

    public float getFloatSetting(Identifier id){
        PermissionSetting<?> setting = getSetting(id);

        if(setting instanceof PermissionFloatSetting){
            return ((PermissionFloatSetting) setting).value;
        }

        return 0;
    }
    
    public int getIntSetting(Identifier id) {
        return (int) getFloatSetting(id);
    }

    public boolean getBoolSetting(Identifier id){
        PermissionSetting<?> setting = getSetting(id);

        if (setting instanceof PermissionBooleanSetting) {
            return ((PermissionBooleanSetting) setting).value;
        }

        return false;
    }

    public String getStringSetting(Identifier id){
        PermissionSetting<?> setting = getSetting(id);

        if(setting instanceof PermissionStringSetting){
            return ((PermissionStringSetting) setting).value;
        }

        return "";
    }
}
