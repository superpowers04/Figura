package net.blancworks.figura;

import net.minecraft.client.util.math.Vector3f;

public class NamePlateData {
    public String text;
    public String chatText;
    public Vector3f position;
    public boolean enabled;
    public int RGB = -1;
    public int chatRGB = -1;
    public byte textProperties = 0b1000000;
    public byte chatTextProperties = 0b1000000;

    public NamePlateData(String text, Vector3f position, boolean enabled, int rgb) {
        this.text = text;
        this.chatText = text;
        this.position = position;
        this.enabled = enabled;
        this.RGB = rgb;
        this.chatRGB = rgb;
    }

    public NamePlateData() {
        this.text = "%n";
        this.chatText = "%n";
        this.position = new Vector3f(0, 2.3F, 0);
        this.enabled = true;
    }
}
