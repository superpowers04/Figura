package net.blancworks.figura.lua.api.nameplate;

import net.minecraft.client.util.math.Vector3f;

public class NamePlateCustomization {
    public String text = "%n";
    public Vector3f position = new Vector3f(0, 0, 0);
    public Vector3f scale = new Vector3f(1, 1, 1);
    public boolean enabled = true;
    public Integer color;
    public Boolean bold;
    public Boolean italic;
    public Boolean obfuscated;
    public Boolean strikethrough;
    public Boolean underline;
}
