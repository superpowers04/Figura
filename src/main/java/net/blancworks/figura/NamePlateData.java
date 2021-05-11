package net.blancworks.figura;

import net.minecraft.client.util.math.Vector3f;

public class NamePlateData {
    public String text;
    public String chatText;
    public String listText;
    public Vector3f position;
    public Vector3f scale;
    public boolean enabled;
    public int RGB = -1;
    public int chatRGB = -1;
    public int listRGB = -1;
    public byte textProperties = 0b1000000;
    public byte chatTextProperties = 0b1000000;
    public byte listTextProperties = 0b1000000;

    public NamePlateData() {
        this.text = "%n";
        this.chatText = "%n";
        this.listText = "%n";
        this.position = new Vector3f(0, 0, 0);
        this.scale = new Vector3f(1, 1, 1);
        this.enabled = true;
    }
}
