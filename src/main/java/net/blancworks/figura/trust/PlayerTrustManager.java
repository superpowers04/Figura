package net.blancworks.figura.trust;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.trust.settings.PermissionBooleanSetting;
import net.blancworks.figura.trust.settings.PermissionFloatSetting;
import net.blancworks.figura.trust.settings.PermissionSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtElement;
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
import java.util.HashMap;
import java.util.Map;

public class PlayerTrustManager {

    public static final Identifier maxInitID = new Identifier("setting", "maxinitinstructions");
    public static final Identifier maxTickID = new Identifier("setting", "maxtickinstructions");
    public static final Identifier maxRenderID = new Identifier("setting", "maxrenderinstructions");
    public static final Identifier maxComplexityID = new Identifier("setting", "maxcomplexity");
    public static final Identifier allowVanillaModID = new Identifier("setting", "allowvanillaedit");
    public static final Identifier maxParticlesID = new Identifier("setting", "maxparticles");

    public static HashMap<Identifier, TrustContainer> allContainers = new HashMap<Identifier, TrustContainer>();
    public static ArrayList<Identifier> allGroups = new ArrayList<Identifier>();
    public static ArrayList<Identifier> defaultGroups = new ArrayList<Identifier>();
    public static HashMap<Identifier, PermissionSetting> permissionSettings = new HashMap<>();
    public static ArrayList<Identifier> permissionDisplayOrder = new ArrayList<Identifier>();

    //Loads all the default groups from the json config file.
    public static void init() {
        registerPermissions();
        loadDefaultGroups();
        
        loadFromDisk();
    }

    public static void registerPermissions() {
        registerPermissionSetting(new PermissionFloatSetting(maxInitID) {{
            min = 0;
            max = 1024 * 17;
            value = 1024 * 16;
            integer = true;
            stepSize = 256;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(maxTickID) {{
            min = 0;
            max = 1024 * 11;
            value = 1024 * 5;
            integer = true;
            stepSize = 256;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(maxRenderID) {{
            min = 0;
            max = 1024 * 11;
            value = 1024 * 2;
            integer = true;
            stepSize = 256;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionFloatSetting(maxComplexityID) {{
            min = 0;
            max = 32 * 65;
            value = 32 * 64;
            integer = true;
            stepSize = 32;
            isSlider = true;
            allowInfinity = true;
        }});
        
        registerPermissionSetting(new PermissionFloatSetting(maxParticlesID){{
            min = 0;
            max = 65;
            value = 5;
            integer = true;
            stepSize = 1;
            isSlider = true;
            allowInfinity = true;
        }});

        registerPermissionSetting(new PermissionBooleanSetting(allowVanillaModID) {{
            value = true;
        }});
    }

    public static void loadDefaultGroups() {

        Path p = FabricLoader.getInstance().getModContainer("figura").get().getRootPath().resolve("presets.json");

        //if (Files.exists(p)) {
            try {
                System.out.println(p);
                InputStream s = Files.newInputStream(p);
                System.out.println(s);
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
                
                if(entry.getKey().equals("hidden")){
                    tc.isHidden = true;
                    continue;
                }

                Identifier settingID = new Identifier("setting", entry.getKey().toLowerCase());
                if (permissionSettings.containsKey(settingID)) {
                    PermissionSetting newSetting = permissionSettings.get(settingID).getCopy();
                    newSetting.fromJson(entry.getValue());
                    tc.permissionSet.put(newSetting.id, newSetting);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void registerPermissionSetting(PermissionSetting baseSetting) {
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
        }

        if (allContainers.containsKey(id))
            return allContainers.get(id);
        return null;
    }

    public static void fromNBT(NbtCompound tag) {

        NbtList list = (NbtList) tag.get("containers");

        for (NbtElement listTag : list) {
            NbtCompound ct = (NbtCompound) listTag;

            String idString = ct.getString("id");
            Identifier id = Identifier.tryParse(idString);

            if (allContainers.containsKey(id)) {
                TrustContainer targetContainer = allContainers.get(id);

                targetContainer.fromNbt(ct);
            }
        }

    }

    public static void toNBT(NbtCompound tag) {
        NbtList containerList = new NbtList();

        for (Map.Entry<Identifier, TrustContainer> entry : allContainers.entrySet()) {

            if (entry.getKey().equals(new Identifier("players", MinecraftClient.getInstance().player.getUuid().toString()))) {
                continue;
            }

            NbtCompound containerTag = new NbtCompound();
            entry.getValue().toNbt(containerTag);
            containerList.add(containerTag);
        }

        tag.put("containers", containerList);
    }


    public static void saveToDisk() {
        try {
            NbtCompound targetTag = new NbtCompound();
            toNBT(targetTag);

            Path targetPath = net.fabricmc.loader.FabricLoader.INSTANCE.getGameDir().resolve("figura");
            Files.createDirectories(targetPath);
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
            Path targetPath = net.fabricmc.loader.FabricLoader.INSTANCE.getGameDir().resolve("figura").resolve("trustSettings.nbt");

            if (!Files.exists(targetPath))
                return;

            FileInputStream fis = new FileInputStream(targetPath.toFile());
            NbtCompound getTag = NbtIo.readCompressed(fis);
            fromNBT(getTag);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
