package net.blancworks.figura.trust;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

//Contains trust settings and permissions
//Used in players and trust groups
public class TrustContainer {
    public String name;
    public boolean locked = false;
    public boolean expanded = true;
    public Identifier parentID;

    private final Map<Trust, Integer> trustSettings;

    public enum Trust {
        INIT_INST("maxinitinstructions", 0, 1024 * 17, 256),
        TICK_INST("maxtickinstructions", 0, 1024 * 11, 256),
        RENDER_INST("maxrenderinstructions", 0, 1024 * 11, 256),
        COMPLEXITY("maxcomplexity", 0, 24 * 12 * 16, 24),
        PARTICLES("maxparticles", 0, 65, 1),
        SOUNDS("maxsfx", 0, 65, 1),
        VANILLA_MODEL_EDIT("allowvanillaedit"),
        NAMEPLATE_EDIT("allownameplateedit"),
        OFFSCREEN_RENDERING("allowoffscreenrendering"),
        CUSTOM_RENDER_LAYER("allowcustomrenderlayers"),
        CUSTOM_SOUNDS("allowcustomsounds");

        public final String id;
        public final boolean isBool;

        public final Integer min;
        public final Integer max;
        public final Integer step;
        Trust(String id) {
            this.id = id;
            this.isBool = true;
            this.min = -1;
            this.max = -1;
            this.step = -1;
        }

        Trust(String id, int min, int max, int step) {
            this.id = id;
            this.isBool = false;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public String getValueText(int value) {
            if (value >= max) return "INFINITY";
            return String.format("%d", value);
        }
    }

    public TrustContainer(String name, Identifier parentID, NbtCompound nbt) {
        this.name = name;
        this.parentID = parentID;

        this.trustSettings = new HashMap<>();
        setTrustFromNbt(nbt);
    }

    public TrustContainer(String name, Identifier parentID, Map<Trust, Integer> trust) {
        this.name = name;
        this.parentID = parentID;
        this.trustSettings = new HashMap<>(trust);
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.put("name", NbtString.of(this.name));
        nbt.put("locked", NbtByte.of(this.locked));
        nbt.put("expanded", NbtByte.of(this.expanded));
        nbt.put("parent", NbtString.of(this.parentID.toString()));

        NbtCompound trust = new NbtCompound();
        this.trustSettings.forEach((key, value) -> trust.put(key.name(), NbtInt.of(value)));

        nbt.put("trust", trust);
    }

    private void setTrustFromNbt(NbtCompound nbt) {
        for (Trust setting : Trust.values()) {
            String trustName = setting.name();

            if (nbt.contains(trustName))
                trustSettings.put(setting, nbt.getInt(trustName));
        }
    }

    public Integer getTrust(Trust trust) {
        Integer setting = this.trustSettings.get(trust);
        if (setting != null) return setting;
        if (parentID != null && PlayerTrustManager.getContainer(parentID) != null) return PlayerTrustManager.getContainer(parentID).getTrust(trust);
        return 0;
    }

    public boolean isTrustEmpty() {
        return this.trustSettings.isEmpty();
    }

    public void resetAllTrust() {
        this.trustSettings.clear();
    }

    public void resetTrust(Trust trust) {
        this.trustSettings.remove(trust);
    }

    public void setTrust(Trust trust, Integer value) {
        this.trustSettings.put(trust, value);
    }

    public boolean contains(Trust trust) {
        return this.trustSettings.containsKey(trust);
    }
}
