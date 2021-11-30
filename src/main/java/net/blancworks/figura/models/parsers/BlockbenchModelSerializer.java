package net.blancworks.figura.models.parsers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.blancworks.figura.FiguraMod;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class BlockbenchModelSerializer {

    public static void toBlockbench(CompoundTag nbt) {
        FiguraMod.doTask(() -> {
            try {
                String fileName = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(new Date());
                Path rootFolder = FiguraMod.getModContentDirectory().resolve("model_files/[§9Figura§r] Saved Models");
                Path dest = rootFolder.resolve(new File(fileName).toPath());

                boolean hasTexture = false;
                int stuff = 0;
                int success = 0;

                if (!Files.exists(dest))
                    Files.createDirectories(dest);

                //script
                try {
                    if (nbt.contains("script")) {
                        stuff++;

                        Path script = dest.resolve("script.lua");
                        CompoundTag tag = nbt.getCompound("script");

                        String source;

                        Set<String> keys = tag.getKeys();
                        if (keys.size() <= 1) {
                            source = tag.getString("src");
                        } else {
                            StringBuilder src = new StringBuilder();
                            for (int i = 0; i < keys.size(); i++) {
                                src.append(tag.getString("src_" + i));
                            }
                            source = src.toString();
                        }

                        Files.write(script, source.getBytes());
                        success++;
                    }
                } catch (Exception e) {
                    FiguraMod.LOGGER.error(e);
                }

                //sounds
                try {
                    if (nbt.contains("sounds")) {
                        stuff++;

                        Path sounds = dest.resolve("sounds.json");
                        Path soundFolder = dest.resolve("sounds");

                        CompoundTag tag = nbt.getCompound("sounds");
                        JsonArray soundsList = new JsonArray();

                        if (!Files.exists(soundFolder))
                            Files.createDirectories(soundFolder);

                        Set<String> keys = tag.getKeys();
                        for (String s : keys) {
                            soundsList.add(s);

                            Path sound = soundFolder.resolve(s + ".ogg");
                            Files.write(sound, tag.getByteArray(s));
                        }

                        //save sounds json
                        String jsonString = new GsonBuilder().create().toJson(soundsList);
                        Files.write(sounds, jsonString.getBytes());

                        success++;
                    }
                } catch (Exception e) {
                    FiguraMod.LOGGER.error(e);
                }

                //texture
                try {
                    if (nbt.contains("texture")) {
                        stuff++;

                        CompoundTag tag = nbt.getCompound("texture");

                        String type = tag.getString("type");
                        if (type.equals("color")) type = "";

                        Path texture = dest.resolve("texture" + type + ".png");
                        Files.write(texture, tag.getByteArray("img2"));

                        hasTexture = true;
                        success++;
                    }
                } catch (Exception e) {
                    FiguraMod.LOGGER.error(e);
                }

                //extra textures
                try {
                    if (nbt.contains("exTexs")) {
                        stuff++;

                        ListTag list = nbt.getList("exTexs", NbtType.COMPOUND);

                        for (Tag tag : list) {
                            String type = ((CompoundTag) tag).getString("type");
                            if (type.equals("color")) type = "";

                            Path texture = dest.resolve("texture" + type + ".png");
                            Files.write(texture, ((CompoundTag) tag).getByteArray("img2"));
                        }

                        success++;
                    }
                } catch (Exception e) {
                    FiguraMod.LOGGER.error(e);
                }

                //model
                try {
                    if (nbt.contains("model")) {
                        stuff++;

                        JsonObject model = new JsonObject();
                        ListTag list = nbt.getCompound("model").getList("parts", NbtType.COMPOUND);

                        //header
                        JsonObject meta = new JsonObject();
                        meta.addProperty("format_version", "4.0");
                        meta.addProperty("creation_time", System.currentTimeMillis());
                        meta.addProperty("model_format", "free");
                        meta.addProperty("box_uv", false);

                        model.add("meta", meta);
                        model.addProperty("name", "");
                        model.addProperty("geometry_name", "");

                        JsonArray box = new JsonArray();
                        box.add(1); box.add(1); box.add(0);
                        model.add("visible_box", box);

                        //texture
                        if (hasTexture) {
                            JsonArray textures = new JsonArray();
                            JsonObject texture = new JsonObject();
                            byte[] data = nbt.getCompound("texture").getByteArray("img2");

                            texture.addProperty("path", dest.resolve("texture.png").toString());
                            texture.addProperty("name", "texture.png");
                            texture.addProperty("folder", fileName);
                            texture.addProperty("namespace", "");
                            texture.addProperty("id", "0");
                            texture.addProperty("particle", false);
                            texture.addProperty("render_mode", "normal");
                            texture.addProperty("visible", true);
                            texture.addProperty("mode", "bitmap");
                            texture.addProperty("saved", true);
                            texture.addProperty("uuid", UUID.randomUUID().toString());
                            texture.addProperty("relative_path", "../texture.png");
                            texture.addProperty("source", "data:image/png;base64," + Base64.getEncoder().encodeToString(data));
                            textures.add(texture);

                            model.add("textures", textures);
                        }

                        //texture size
                        JsonObject resolution = new JsonObject();
                        ListTag uv = nbt.getCompound("model").getList("uv", NbtType.FLOAT);
                        resolution.addProperty("width", uv.getFloat(0));
                        resolution.addProperty("height", uv.getFloat(1));

                        model.add("resolution", resolution);

                        //the model
                        boolean isPlayer = false;
                        JsonArray outliner = new JsonArray();
                        JsonArray elements = new JsonArray();
                        for (Tag tagElement : list) {
                            isPlayer = parseModelPart((CompoundTag) tagElement, elements, outliner, new Vector3f(0f, 0f, 0f)) || isPlayer;
                        }

                        model.add("elements", elements);
                        model.add("outliner", outliner);

                        //write model
                        String jsonString = new GsonBuilder().serializeNulls().create().toJson(model);
                        Files.write(dest.resolve((isPlayer ? "player_model" : "model") + ".bbmodel"), jsonString.getBytes());

                        success++;
                    }
                } catch (Exception e) {
                    FiguraMod.LOGGER.error(e);
                }

                FiguraMod.sendToast("done", success + " / " + stuff);
            } catch (Exception e) {
                FiguraMod.LOGGER.error(e);
                FiguraMod.sendToast("oopsie", "Something went really wrong");
            }
        });
    }

    public static final HashMap<String, Vector3f> PLAYER_MODEL_OFFSETS = new HashMap<String, Vector3f>() {{
        put("Head", new Vector3f(0, 24, 0));
        put("Body", new Vector3f(0, 24, 0));
        put("LeftArm", new Vector3f(-5, 22, 0));
        put("RightArm", new Vector3f(5, 22, 0));
        put("LeftLeg", new Vector3f(-2, 12, 0));
        put("RightLeg", new Vector3f(2, 12, 0));
    }};

    public static boolean parseModelPart(CompoundTag tag, JsonArray elements, JsonArray outliner, Vector3f offset) {
        boolean player = false;
        JsonObject part = new JsonObject();

        String name = tag.contains("nm") ? tag.getString("nm") : "element";
        part.addProperty("name", name);
        part.addProperty("rescale", false);
        part.addProperty("locked", false);
        part.addProperty("autouv", 0);
        part.addProperty("color", (int) (Math.random() * 7) + 1);

        //pivot
        if (tag.contains("piv")) {
            JsonArray pivot = FloatTagListToJson(tag.getList("piv", NbtType.FLOAT));
            pivot.set(2, new JsonPrimitive(-pivot.get(2).getAsFloat()));
            applyArrayOffset(pivot, offset);
            part.add("origin", pivot);
        }

        //rotation
        if (tag.contains("rot"))
            part.add("rotation", FloatTagListToJson(tag.getList("rot", NbtType.FLOAT)));

        //uuid
        String uuid = UUID.randomUUID().toString();
        part.addProperty("uuid", uuid);

        //part type
        String pt = tag.contains("pt") ? tag.getString("pt") : "na";
        switch (pt) {
            case "cub":
                CompoundTag props = tag.getCompound("props");
                JsonObject faces = new JsonObject();

                //inflate
                if (props.contains("inf"))
                    part.addProperty("inflate", props.getFloat("inf"));

                //from to
                JsonArray from = FloatTagListToJson(props.getList("f", NbtType.FLOAT));
                applyArrayOffset(from, offset);
                part.add("from", from);

                JsonArray to = FloatTagListToJson(props.getList("t", NbtType.FLOAT));
                applyArrayOffset(to, offset);
                part.add("to", to);

                //face data
                faces.add("north", buildFaceData(props.getCompound("n")));
                faces.add("south", buildFaceData(props.getCompound("s")));
                faces.add("west", buildFaceData(props.getCompound("w")));
                faces.add("east", buildFaceData(props.getCompound("e")));
                faces.add("up", buildFaceData(props.getCompound("u")));
                faces.add("down", buildFaceData(props.getCompound("d")));

                part.add("faces", faces);
                elements.add(part);
                outliner.add(uuid);
                break;
            case "msh":
                CompoundTag props2 = tag.getCompound("props");

                //mesh properties
                part.addProperty("visibility", true);
                part.addProperty("type", "mesh");

                //vertices
                JsonObject vertices = new JsonObject();
                CompoundTag vertList = props2.getCompound("vertices");

                for (String s : vertList.getKeys()) {
                    JsonArray vertex = FloatTagListToJson(vertList.getList(s, NbtType.FLOAT));
                    vertex.set(0, new JsonPrimitive(-vertex.get(0).getAsFloat()));
                    vertex.set(1, new JsonPrimitive(-vertex.get(1).getAsFloat()));
                    //applyArrayOffset(vertex, offset);
                    vertices.add(s, vertex);
                }

                //faces
                JsonObject faces2 = new JsonObject();
                ListTag faceList = props2.getList("faces", NbtType.LIST);
                int i = 0;
                for (Tag faceNbt : faceList) {
                    JsonObject face = new JsonObject();
                    face.addProperty("texture", 0f);

                    JsonArray vert = new JsonArray();
                    JsonObject uv = new JsonObject();

                    for (Tag vertexNbt : ((ListTag) faceNbt)) {
                        CompoundTag vertex = (CompoundTag) vertexNbt;
                        String id = vertex.getString("id");

                        uv.add(id, FloatTagListToJson(vertex.getList("uv", NbtType.FLOAT)));
                        vert.add(id);
                    }

                    face.add("vertices", vert);
                    face.add("uv", uv);
                    faces2.add(i++ + "", face);
                }

                part.add("vertices", vertices);
                part.add("faces", faces2);

                elements.add(part);
                outliner.add(uuid);
                break;
            case "na":
                //fix pivot offset
                Vector3f newOffset = offset.copy();

                for (Map.Entry<String, Vector3f> entry : PLAYER_MODEL_OFFSETS.entrySet()) {
                    if (name.contains(entry.getKey())) {
                        newOffset = entry.getValue();
                        JsonArray pivot;

                        if (tag.contains("piv")) {
                            pivot = FloatTagListToJson(tag.getList("piv", NbtType.FLOAT));
                            pivot.set(2, new JsonPrimitive(-pivot.get(2).getAsFloat()));
                            applyArrayOffset(pivot, newOffset);
                        } else {
                            pivot = new JsonArray();
                            pivot.add(newOffset.getX());
                            pivot.add(newOffset.getY());
                            pivot.add(newOffset.getZ());
                        }

                        part.add("origin", pivot);
                        player = true;
                        break;
                    }
                }

                //group properties
                part.addProperty("visibility", true);
                part.addProperty("export", true);
                part.addProperty("isOpen", true);

                JsonArray children = new JsonArray();
                if (tag.contains("chld")) {
                    ListTag childList = tag.getList("chld", NbtType.COMPOUND);

                    for (Tag Tag : childList) {
                        player = parseModelPart((CompoundTag) Tag, elements, children, newOffset) || player;
                    }
                }

                part.add("children", children);
                outliner.add(part);
            break;
        }

        return player;
    }

    public static void applyArrayOffset(JsonArray array, Vector3f offset) {
        array.set(0, new JsonPrimitive(array.get(0).getAsFloat() + offset.getX()));
        array.set(1, new JsonPrimitive(array.get(1).getAsFloat() + offset.getY()));
        array.set(2, new JsonPrimitive(array.get(2).getAsFloat() + offset.getZ()));
    }

    public static JsonArray FloatTagListToJson(ListTag list) {
        JsonArray json = new JsonArray();

        for (Tag Tag : list) {
            float val = ((FloatTag) Tag).getFloat();
            json.add(val);
        }

        return json;
    }

    public static JsonObject buildFaceData(CompoundTag nbt) {
        JsonObject face = new JsonObject();
        JsonArray uv = FloatTagListToJson(nbt.getList("uv", NbtType.FLOAT));

        if (nbt.contains("texture"))
            face.addProperty("texture", 0f);
        else
            face.add("texture", null);

        if (nbt.contains("rotation"))
            face.addProperty("rotation", nbt.getFloat("rotation"));

        face.add("uv", uv);
        return face;
    }
}
