package net.blancworks.figura.trust;

import net.blancworks.figura.trust.settings.PermissionSetting;

import java.util.HashMap;

public class TrustPreset {
    public String name;
    public HashMap<String, PermissionSetting> permissions = new HashMap<String, PermissionSetting>();
}
