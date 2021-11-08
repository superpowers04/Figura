package net.blancworks.figura.trust;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blancworks.figura.FiguraMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerTrustManager {

    public static Map<Identifier, TrustContainer> defaultGroups = new HashMap<>();
    public static Map<Identifier, TrustContainer> groups = new LinkedHashMap<>();
    public static Map<Identifier, TrustContainer> players = new HashMap<>();

    public static void init() {
        //load from presets file first then load from disk
        loadDefaultGroups();
        loadFromDisk();
    }

    public static void loadDefaultGroups() {
        try {
            Path presets = FabricLoader.getInstance().getModContainer("figura").get().getRootPath().resolve("assets/figura/trust/presets.json");
            InputStreamReader fileReader = new InputStreamReader(Files.newInputStream(presets));
            JsonObject rootObject = (JsonObject) new JsonParser().parse(fileReader);

            rootObject.entrySet().forEach(entry -> {
                String name = entry.getKey();

                NbtCompound nbt = new NbtCompound();
                entry.getValue().getAsJsonObject().entrySet().forEach(trust -> nbt.put(trust.getKey(), NbtInt.of(trust.getValue().getAsInt())));

                Identifier parentID = new Identifier("default_group", name);
                TrustContainer parent = new TrustContainer(name, null, nbt);
                TrustContainer container = new TrustContainer(name, parentID, new NbtCompound());

                if (name.equals("local")) {
                    parent.locked = true;
                    container.locked = true;
                }

                defaultGroups.put(parentID, parent);
                groups.put(new Identifier("group", name), container);
            });

            FiguraMod.LOGGER.debug("Loaded presets from assets");
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Could not load presets from assets");
            e.printStackTrace();
        }
    }

    public static TrustContainer getContainer(Identifier id) {
        if (defaultGroups.containsKey(id))
            return defaultGroups.get(id);

        if (groups.containsKey(id))
            return groups.get(id);

        if (players.containsKey(id))
            return players.get(id);

        boolean isLocal = id.getPath().equals(getClientPlayerID());
        Identifier parentID = new Identifier("group", isLocal ? "local" : "untrusted");
        TrustContainer trust =  new TrustContainer(id.getPath(), parentID, new HashMap<>());

        if (isLocal) trust.locked = true;

        players.put(id, trust);
        return trust;
    }

    public static void writeNbt(NbtCompound nbt) {
        NbtList groupList = new NbtList();
        NbtList playerList = new NbtList();

        groups.forEach((key, value) -> {
            NbtCompound container = new NbtCompound();
            value.writeNbt(container);
            groupList.add(container);
        });

        players.forEach((key, value) -> {
            if (!key.getPath().equals(getClientPlayerID()) && !value.parentID.getPath().equals("local") && (!value.isTrustEmpty() || !value.parentID.getPath().equals("untrusted"))) {
                NbtCompound container = new NbtCompound();
                value.writeNbt(container);
                playerList.add(container);
            }
        });

        nbt.put("groups", groupList);
        nbt.put("players", playerList);
    }

    public static void readNbt(NbtCompound nbt) {
        NbtList groupList = nbt.getList("groups", NbtElement.COMPOUND_TYPE);
        NbtList playerList = nbt.getList("players", NbtElement.COMPOUND_TYPE);

        groupList.forEach(value -> {
            NbtCompound compound = (NbtCompound) value;

            String name = compound.getString("name");
            Identifier parentID = new Identifier(compound.getString("parent"));
            TrustContainer container =  new TrustContainer(name, parentID, compound.getCompound("trust"));

            container.locked = name.equals("local") || compound.getBoolean("locked");
            container.expanded = compound.getBoolean("expanded");

            groups.put(new Identifier("group", name), container);
        });

        playerList.forEach(value -> {
            NbtCompound compound = (NbtCompound) value;

            String name = compound.getString("name");
            Identifier parentID = new Identifier(compound.getString("parent"));

            if (!name.equals(getClientPlayerID()) && !parentID.getPath().equals("local")) {
                TrustContainer container = new TrustContainer(name, parentID, compound.getCompound("trust"));
                container.locked = compound.getBoolean("locked");
                container.expanded = compound.getBoolean("expanded");

                players.put(new Identifier("player", name), container);
            }
        });
    }

    public static void saveToDisk() {
        try {
            NbtCompound targetTag = new NbtCompound();
            writeNbt(targetTag);

            Path targetPath = FiguraMod.getModContentDirectory();
            targetPath = targetPath.resolve("trust_settings.nbt");

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
            Path targetPath = FiguraMod.getModContentDirectory().resolve("trust_settings.nbt");

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
        Identifier parentID = tc.parentID;

        int i = 0;
        Identifier nextID = null;
        for (Map.Entry<Identifier, TrustContainer> entry : groups.entrySet()) {
            if (nextID != null) {
                nextID = entry.getKey();
                break;
            }

            if (entry.getKey().equals(parentID))
                nextID = entry.getKey();

            i++;
        }

        if (nextID == null || (nextID.getPath().equals("local") && !tc.name.equals(getClientPlayerID())) || i == groups.size())
            return false;

        tc.parentID = nextID;
        saveToDisk();
        return true;
    }

    public static boolean decreaseTrust(TrustContainer tc) {
        Identifier parentID = tc.parentID;

        int i = 0;
        Identifier prevID = null;
        for (Map.Entry<Identifier, TrustContainer> entry : groups.entrySet()) {
            if (entry.getKey().equals(parentID))
                break;

            prevID = entry.getKey();
            i++;
        }

        if (prevID == null || i == groups.size())
            return false;

        tc.parentID = prevID;
        saveToDisk();
        return true;
    }

    private static String getClientPlayerID() {
        return MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getUuid().toString() : "";
    }
}
