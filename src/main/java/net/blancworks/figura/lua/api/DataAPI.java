package net.blancworks.figura.lua.api;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.util.Identifier;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DataAPI {
    public static Identifier getID() {
        return new Identifier("default", "data");
    }

    public static Path getContentDirectory() {
        return FiguraMod.getModContentDirectory().resolve("stored_vars");
    }

    public static LuaTable getForScript(CustomScript script) {
        final boolean isHost = script.avatarData == AvatarDataManager.localPlayer;

        return new LuaTable() {{
            //script name
            set("setName", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String name = arg.checkjstring();

                    try {
                        Path root = getContentDirectory().toAbsolutePath();
                        Path test = root.resolve(Paths.get(name + ".json")).toAbsolutePath();

                        if (root.compareTo(test.getParent()) != 0)
                            throw new Exception("Folder access is forbidden");
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new LuaError(e.getMessage());
                    }

                    script.scriptName = name;
                    return NIL;
                }
            });

            set("getName", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.scriptName);
                }
            });

            //world.getPlayers() tracking
            set("allowTracking", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.canBeTracked = arg.checkboolean();
                    return NIL;
                }
            });

            set("hasTracking", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.canBeTracked);
                }
            });

            //store a value
            set("save", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    if (!isHost) return NIL;

                    if (arg2.isfunction()) throw new LuaError("Cannot save functions - sowwy!");

                    saveElement(script, arg1.checkjstring(), arg2);
                    return NIL;
                }
            });

            //load a value
            set("load", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    return isHost ? loadElement(script, arg1.checkjstring()) : NIL;
                }
            });

            //load all values
            set("loadAll", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (!isHost) return NIL;

                    Map<String, LuaValue> values = loadAllElements(script);
                    if (values.isEmpty()) return NIL;

                    LuaTable tbl = new LuaTable();
                    values.forEach(tbl::set);
                    return tbl;
                }
            });

            //remove a value
            set("remove", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (isHost) removeElement(script, arg.checkjstring());
                    return NIL;
                }
            });

            //completely nuke the file
            set("deleteFile", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (isHost) deleteFile(script);
                    return NIL;
                }
            });
        }};
    }

    private static JsonObject getOrCreateJsonFromFile(CustomScript script) {
        try {
            //create file
            Path contentDirectory = getContentDirectory();
            if (!Files.exists(contentDirectory))
                Files.createDirectories(contentDirectory);

            Path file = contentDirectory.resolve(script.scriptName + ".json");
            if (!Files.exists(file)) {
                Files.createFile(file);
                return new JsonObject();
            } else {
                //load file
                BufferedReader br = new BufferedReader(new FileReader(file.toFile()));
                JsonElement json = JsonParser.parseReader(br);
                br.close();

                if (json != null && json.isJsonObject())
                    return json.getAsJsonObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //failsafe
        return new JsonObject();
    }

    private static void saveElementEntry(JsonObject json, String key, LuaValue value) {
        //do not add functions
        if (value.isfunction()) return;

        //add to json
        if (value instanceof LuaTable tbl) {
            JsonObject object = new JsonObject();
            object.addProperty("type", "TABLE");

            JsonObject entries = new JsonObject();
            LuaValue[] keys = tbl.keys();

            for (LuaValue luaKey : keys) {
                saveElementEntry(entries, luaKey.tojstring(), tbl.get(luaKey));
            }

            object.add("entries", entries);
            json.add(key, object);
        }
        else if (value instanceof LuaVector vec) {
            JsonObject object = new JsonObject();
            object.addProperty("type", "VECTOR");

            object.addProperty("x", vec.x());
            object.addProperty("y", vec.y());
            object.addProperty("z", vec.z());
            object.addProperty("w", vec.w());
            object.addProperty("t", vec.t());
            object.addProperty("h", vec.h());
            json.add(key, object);
        }
        else {
            //isnumber() returns true from "strings numbers" but we dont really want it to be converted
            if (value instanceof LuaNumber)
                json.addProperty(key, value.todouble());
            else if (value instanceof LuaBoolean)
                json.addProperty(key, value.toboolean());
            else
                json.addProperty(key, value.toString());
        }
    }

    private static void saveElement(CustomScript script, String key, LuaValue value) {
        try {
            //create or load file
            JsonObject json = getOrCreateJsonFromFile(script);

            //save values
            saveElementEntry(json, key, value);

            //save it
            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(json);

            //no need to check if file exists because we already did it earlier
            FileWriter fileWriter = new FileWriter(getContentDirectory().resolve(script.scriptName + ".json").toFile());
            fileWriter.write(jsonString);
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static LuaValue loadElementEntry(JsonElement element) {
        //default
        if (!element.isJsonObject()) return LuaValue.valueOf(element.getAsJsonPrimitive().getAsString());

        JsonElement object = element.getAsJsonObject().get("type");
        if (object == null) return LuaValue.NIL;

        //table
        if (object.getAsJsonPrimitive().getAsString().equals("TABLE")) {
            JsonObject table = element.getAsJsonObject().get("entries").getAsJsonObject();
            LuaTable luaTable = new LuaTable();

            for (Map.Entry<String, JsonElement> entry : table.entrySet()) {
                luaTable.set(LuaValue.valueOf(entry.getKey()), loadElementEntry(entry.getValue()));
            }

            return luaTable;
        }
        //vector
        else if (object.getAsJsonPrimitive().getAsString().equals("VECTOR")) {
            float x = element.getAsJsonObject().get("x").getAsJsonPrimitive().getAsFloat();
            float y = element.getAsJsonObject().get("y").getAsJsonPrimitive().getAsFloat();
            float z = element.getAsJsonObject().get("z").getAsJsonPrimitive().getAsFloat();
            float w = element.getAsJsonObject().get("w").getAsJsonPrimitive().getAsFloat();
            float t = element.getAsJsonObject().get("t").getAsJsonPrimitive().getAsFloat();
            float h = element.getAsJsonObject().get("h").getAsJsonPrimitive().getAsFloat();

            return new LuaVector(x, y, z, w, t, h);
        }

        return LuaValue.NIL;
    }

    private static LuaValue loadElement(CustomScript script, String key) {
        //create or load file
        JsonObject json = getOrCreateJsonFromFile(script);
        if (!json.has(key)) return LuaValue.NIL;

        try {
            //load contents
            JsonElement element = json.get(key);
            return loadElementEntry(element);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return LuaValue.NIL;
    }

    private static Map<String, LuaValue> loadAllElements(CustomScript script) {
        //create or load file
        JsonObject json = getOrCreateJsonFromFile(script);

        Map<String, LuaValue> entries = new HashMap<>();
        json.entrySet().forEach(entry -> entries.put(entry.getKey(), loadElement(script, entry.getKey())));
        return entries;
    }

    private static void removeElement(CustomScript script, String key) {
        //create or load file
        JsonObject json = getOrCreateJsonFromFile(script);

        if (!json.has(key)) return;

        try {
            //remove entry
            json.remove(key);

            //save the file
            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(json);

            FileWriter fileWriter = new FileWriter(getContentDirectory().resolve(script.scriptName + ".json").toFile());
            fileWriter.write(jsonString);
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteFile(CustomScript script) {
        try {
            //delete file
            Path contentDirectory = getContentDirectory();
            if (!Files.exists(contentDirectory))
                Files.createDirectories(contentDirectory);
            Path file = contentDirectory.resolve(script.scriptName + ".json");
            Files.deleteIfExists(file);
        } catch (Exception ignored) {}
    }
}
