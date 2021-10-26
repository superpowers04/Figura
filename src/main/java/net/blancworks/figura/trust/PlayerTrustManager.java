package net.blancworks.figura.trust;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.fabricmc.fabric.api.util.NbtType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlayerTrustManager {
    public static final Identifier MAX_INIT_ID = new Identifier("setting", "maxinitinstructions");
    public static final Identifier MAX_TICK_ID = new Identifier("setting", "maxtickinstructions");
    public static final Identifier MAX_RENDER_ID = new Identifier("setting", "maxrenderinstructions");
    public static final Identifier MAX_COMPLEXITY_ID = new Identifier("setting", "maxcomplexity");
    public static final Identifier ALLOW_VANILLA_MOD_ID = new Identifier("setting", "allowvanillaedit");
    public static final Identifier ALLOW_NAMEPLATE_MOD_ID = new Identifier("setting", "allownameplateedit");
    public static final Identifier ALLOW_OFFSCREEN_RENDERING = new Identifier("setting", "allowoffscreenrendering");
    public static final Identifier ALLOW_CUSTOM_RENDERLAYERS = new Identifier("setting", "allowcustomrenderlayers");
    public static final Identifier MAX_PARTICLES_ID = new Identifier("setting", "maxparticles");
    public static final Identifier MAX_SOUND_EFFECTS_ID = new Identifier("setting", "maxsfx");

    public static Map<Identifier, TrustContainer> allContainers = new Object2ObjectOpenHashMap<>();
    public static List<Identifier> allGroups = new ArrayList<>();
    public static List<Identifier> defaultGroups = new ArrayList<>();
    public static Map<Identifier, PermissionSetting<?>> permissionSettings = new Object2ObjectOpenHashMap<>();
    public static List<Identifier> permissionDisplayOrder = new ArrayList<>();

    //Loads all the default groups from the json config file.
    public static void init() {
        registerPermissions();
        loadDefaultGroups();

        loadFromDisk();
    }

    public static void registerPermissions() {
        registerPermissionSetting(new PermissionFloatSetting(MAX_INIT_ID) {{
            min = 0;
            max = 1024 * 17;
            value = 1024 * 16;
            integer = true;
            stepSize = 256;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(MAX_TICK_ID) {{
            min = 0;
            max = 1024 * 11;
            value = 1024 * 5;
            integer = true;
            stepSize = 256;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(MAX_RENDER_ID) {{
            min = 0;
            max = 1024 * 11;
            value = 1024 * 2;
            integer = true;
            stepSize = 256;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(MAX_COMPLEXITY_ID) {{
            min = 0;
            max = 24 * 12 * 4 * 4;
            value = 24 * 12 * 4;
            integer = true;
            stepSize = 24;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(MAX_PARTICLES_ID) {{
            min = 0;
            max = 65;
            value = 5;
            integer = true;
            stepSize = 1;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(MAX_SOUND_EFFECTS_ID) {{
            min = 0;
            max = 65;
            value = 0;
            integer = true;
            stepSize = 1;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionBooleanSetting(ALLOW_VANILLA_MOD_ID) {{
            value = true;
        }});

        registerPermissionSetting(new PermissionBooleanSetting(ALLOW_NAMEPLATE_MOD_ID) {{
            value = true;
        }});

        registerPermissionSetting(new PermissionBooleanSetting(ALLOW_OFFSCREEN_RENDERING) {{
            value = true;
        }});

        registerPermissionSetting(new PermissionBooleanSetting(ALLOW_CUSTOM_RENDERLAYERS) {{
            value = true;
        }});
    }

    public static void loadDefaultGroups() {
        Path p = FabricLoader.getInstance().getModContainer("figura").get().getRootPath().resolve("presets.json");

        //if (Files.exists(p)) {
        try {
            InputStream s = Files.newInputStream(p);
            InputStreamReader fileReader = new InputStreamReader(s);
            JsonParser parser = new JsonParser();
            JsonObject rootObject = (JsonObject) parser.parse(fileReader);

            TrustContainer trueBase = new TrustContainer(new Identifier("group", "base"), new LiteralText("base"));
            JsonObject baseObj = rootObject.get("base").getAsJsonObject();
            allContainers.put(trueBase.getIdentifier(), trueBase);

            trueBase.isHidden = true;
            trueBase.isLocked = true;
            trueBase.displayChildren = false;

            fillOutGroup(trueBase, baseObj);

            for (Map.Entry<String, JsonElement> entry : rootObject.entrySet()) {
                if (entry.getKey().equals("base"))
                    continue;

                String key = entry.getKey();
                JsonObject value = entry.getValue().getAsJsonObject();

                //This is the base container, use as the parent of any given default group to allow them to be properly reset
                TrustContainer baseGroupContainer = new TrustContainer(new Identifier("group", "base" + key), new LiteralText("base" + key));

                fillOutGroup(baseGroupContainer, value);


                baseGroupContainer.setParent(trueBase.getIdentifier());

                allContainers.put(baseGroupContainer.getIdentifier(), baseGroupContainer);

                TrustContainer realContainer = new TrustContainer(new Identifier("group", key), new TranslatableText(key));
                realContainer.setParent(baseGroupContainer.getIdentifier());
                addGroup(realContainer);
                realContainer.isLocked = true;
                realContainer.isHidden = baseGroupContainer.isHidden;

                baseGroupContainer.isHidden = true;
                baseGroupContainer.isLocked = true;
                baseGroupContainer.displayChildren = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //}

        FiguraMod.LOGGER.debug("Loaded presets from assets");
    }

    public static void addGroup(TrustContainer container) {
        allGroups.add(container.getIdentifier());
        defaultGroups.add(container.getIdentifier());
        allContainers.put(container.getIdentifier(), container);
    }

    public static void fillOutGroup(TrustContainer tc, JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            try {
                if (entry.getKey().equals("parent")) {
                    tc.setParent(new Identifier("group", entry.getValue().getAsString()));
                    continue;
                }

                if (entry.getKey().equals("hidden")) {
                    tc.isHidden = true;
                    continue;
                }

                Identifier settingID = new Identifier("setting", entry.getKey().toLowerCase(Locale.ENGLISH));
                if (permissionSettings.containsKey(settingID)) {
                    PermissionSetting<?> newSetting = permissionSettings.get(settingID).getCopy();
                    newSetting.fromJson(entry.getValue());
                    tc.permissionSet.put(newSetting.id, newSetting);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void registerPermissionSetting(PermissionSetting<?> baseSetting) {
        permissionSettings.put(baseSetting.id, baseSetting);
        permissionDisplayOrder.add(baseSetting.id);
    }

    public static TrustContainer getContainer(Identifier id) {
        if (!allContainers.containsKey(id)) {
            TrustContainer newContainer = new TrustContainer(id, Text.of(id.getPath()));

            if (id.equals(new Identifier("players", MinecraftClient.getInstance().player.getUuid().toString()))) {
                newContainer.setParent(new Identifier("group", "local"));
                newContainer.isHidden = true;
            } else {
                newContainer.setParent(new Identifier("group", "untrusted"));
            }
            allContainers.put(id, newContainer);
            return newContainer;
        } else {
            return allContainers.get(id);
        }
    }

    public static void readNbt(NbtCompound nbt) {
        allGroups.clear();
        defaultGroups.clear();
        loadDefaultGroups();

        NbtList containersList = (NbtList) nbt.get("containers");
        if (containersList != null && containersList.getHeldType() == NbtType.COMPOUND) {
            for (NbtElement element : containersList) {
                NbtCompound nbtCompound = (NbtCompound) element;

                String idString = nbtCompound.getString("id");
                Identifier id = Identifier.tryParse(idString);

                if (allContainers.containsKey(id)) {
                    TrustContainer targetContainer = allContainers.get(id);
                    targetContainer.fromNbt(nbtCompound);
                }
            }
        }

        NbtList playersList = (NbtList) nbt.get("players");
        if (playersList != null && playersList.getHeldType() == NbtType.COMPOUND) {
            for (NbtElement element : playersList) {
                NbtCompound nbtCompound = (NbtCompound) element;

                String idString = nbtCompound.getString("id");
                Identifier id = Identifier.tryParse(idString);

                if (allContainers.containsKey(id)) {
                    TrustContainer targetContainer = allContainers.get(id);
                    targetContainer.fromNbt(nbtCompound);
                } else if (id != null) {
                    TrustContainer newContainer = new TrustContainer(id, Text.of(id.getPath()));

                    String pidString = nbtCompound.getString("pid");
                    Identifier pid = Identifier.tryParse(pidString);

                    if (allContainers.containsKey(pid)) {
                        newContainer.setParent(pid);
                        allContainers.put(id, newContainer);
                    }
                }
            }
        }
    }

    public static void writeNbt(NbtCompound nbt) {
        NbtList containerList = new NbtList();
        NbtList playerList = new NbtList();

        for (Map.Entry<Identifier, TrustContainer> entry : allContainers.entrySet()) {
            NbtCompound containerNbt = new NbtCompound();
            entry.getValue().toNbt(containerNbt);

            if (entry.getKey().getNamespace().equals("players")) {
                if (!containerNbt.getString("pid").equals("group:local") && !(containerNbt.getCompound("perms").isEmpty() && containerNbt.getString("pid").equals("group:untrusted")) && !entry.getKey().getPath().equals(MinecraftClient.getInstance().player.getUuid().toString()))
                    playerList.add(containerNbt);
            } else {
                containerList.add(containerNbt);
            }
        }

        nbt.put("containers", containerList);
        nbt.put("players", playerList);
    }

    public static void saveToDisk() {
        try {
            NbtCompound targetTag = new NbtCompound();
            writeNbt(targetTag);

            Path targetPath = FiguraMod.getModContentDirectory();
            targetPath = targetPath.resolve("trustSettings.nbt");

            if (!Files.exists(targetPath))
                Files.createFile(targetPath);

            FileOutputStream fs = new FileOutputStream(targetPath.toFile());
            NbtIo.writeCompressed(targetTag, fs);

            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadFromDisk() {
        try {
            Path targetPath = FiguraMod.getModContentDirectory().resolve("trustSettings.nbt");

            if (!Files.exists(targetPath))
                return;

            FileInputStream fis = new FileInputStream(targetPath.toFile());
            NbtCompound getTag = NbtIo.readCompressed(fis);
            readNbt(getTag);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean increaseTrust(TrustContainer tc) {
        Identifier parentID = tc.getParentIdentifier();

        int index = -1;
        for (int i = 0; i < allGroups.size() - 1; i++) {
            if (allGroups.get(i).equals(parentID))
                index = i;
        }

        if (index != -1 && !getContainer(parentID).isHidden) {
            Identifier nextID = allGroups.get(index + 1);
            if (!getContainer(nextID).isHidden) {
                tc.setParent(nextID);
                saveToDisk();
                return true;
            }
        }

        return false;
    }

    public static boolean decreaseTrust(TrustContainer tc) {
        Identifier parentID = tc.getParentIdentifier();

        int index = -1;
        for (int i = allGroups.size() - 1; i > 0; i--) {
            if (allGroups.get(i).equals(parentID))
                index = i;
        }

        if (index != -1 && !getContainer(parentID).isHidden) {
            Identifier prevID = allGroups.get(index - 1);
            if (!getContainer(prevID).isHidden) {
                tc.setParent(prevID);
                saveToDisk();
                return true;
            }
        }

        return false;
    }
}
