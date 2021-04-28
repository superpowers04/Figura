package net.blancworks.figura;

import net.minecraft.client.util.math.Vector3f;

public class NamePlateData {
    public String text;
    public Vector3f position;
    public boolean enabled;
    public int rgb;
    public boolean bold;
    public boolean italic;
    public boolean underlined;
    public boolean strikethrough;
    public boolean obfuscated;
    public boolean decorations_disabled = true;

    public NamePlateData(String text, Vector3f position, boolean enabled, int rgb) {
        this.text = text;
        this.position = position;
        this.enabled = enabled;
        this.rgb = rgb;
    }

    public NamePlateData() {
        this.text = "%n";
        this.position = new Vector3f(0, 2.3F, 0);
        this.enabled = true;
        this.rgb = -1;
    }
}
