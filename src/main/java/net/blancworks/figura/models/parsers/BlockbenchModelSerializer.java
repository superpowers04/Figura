package net.blancworks.figura.models.parsers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.blancworks.figura.FiguraMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class BlockbenchModelSerializer {

    public static void toBlockbench(NbtCompound nbt) {
        FiguraMod.doTask(() -> {
            try {
                String fileName = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(new Date());
                Path rootFolder = FiguraMod.getModContentDirectory().resolve("model_files/[§9Figura§r] Saved Models");
                Path dest = rootFolder.resolve(Path.of(fileName));

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
                        NbtCompound tag = nbt.getCompound("script");

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

                        Files.writeString(script, source);
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

                        NbtCompound tag = nbt.getCompound("sounds");
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
                        Files.writeString(sounds, jsonString);

                        success++;
                    }
                } catch (Exception e) {
                    FiguraMod.LOGGER.error(e);
                }

                //texture
                try {
                    if (nbt.contains("texture")) {
                        stuff++;

                        NbtCompound tag = nbt.getCompound("texture");

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

                        NbtList list = nbt.getList("exTexs", NbtElement.COMPOUND_TYPE);

                        for (NbtElement tag : list) {
                            String type = ((NbtCompound) tag).getString("type");
                            if (type.equals("color")) type = "";

                            Path texture = dest.resolve("texture" + type + ".png");
                            Files.write(texture, ((NbtCompound) tag).getByteArray("img2"));
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
                        NbtList list = nbt.getCompound("model").getList("parts", NbtElement.COMPOUND_TYPE);

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
                        NbtList uv = nbt.getCompound("model").getList("uv", NbtElement.FLOAT_TYPE);
                        resolution.addProperty("width", uv.getFloat(0));
                        resolution.addProperty("height", uv.getFloat(1));

                        model.add("resolution", resolution);

                        //the model
                        boolean isPlayer = false;
                        JsonArray outliner = new JsonArray();
                        JsonArray elements = new JsonArray();
                        for (NbtElement tagElement : list) {
                            isPlayer = parseModelPart((NbtCompound) tagElement, elements, outliner, Vec3f.ZERO.copy()) || isPlayer;
                        }

                        model.add("elements", elements);
                        model.add("outliner", outliner);

                        //write model
                        String jsonString = new GsonBuilder().serializeNulls().create().toJson(model);
                        Files.writeString(dest.resolve((isPlayer ? "player_model" : "model") + ".bbmodel"), jsonString);

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

    public static final HashMap<String, Vec3f> PLAYER_MODEL_OFFSETS = new HashMap<>() {{
        put("Head", new Vec3f(0, 24, 0));
        put("Body", new Vec3f(0, 24, 0));
        put("LeftArm", new Vec3f(-5, 22, 0));
        put("RightArm", new Vec3f(5, 22, 0));
        put("LeftLeg", new Vec3f(-2, 12, 0));
        put("RightLeg", new Vec3f(2, 12, 0));
    }};

    public static boolean parseModelPart(NbtCompound tag, JsonArray elements, JsonArray outliner, Vec3f offset) {
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
            JsonArray pivot = nbtFloatListToJson(tag.getList("piv", NbtElement.FLOAT_TYPE));
            pivot.set(2, new JsonPrimitive(-pivot.get(2).getAsFloat()));
            applyArrayOffset(pivot, offset);
            part.add("origin", pivot);
        }

        //rotation
        if (tag.contains("rot"))
            part.add("rotation", nbtFloatListToJson(tag.getList("rot", NbtElement.FLOAT_TYPE)));

        //uuid
        String uuid = UUID.randomUUID().toString();
        part.addProperty("uuid", uuid);

        //part type
        String pt = tag.contains("pt") ? tag.getString("pt") : "na";
        switch (pt) {
            case "cub" -> {
                NbtCompound props = tag.getCompound("props");
                JsonObject faces = new JsonObject();

                //inflate
                if (props.contains("inf"))
                    part.addProperty("inflate", props.getFloat("inf"));

                //from to
                JsonArray from = nbtFloatListToJson(props.getList("f", NbtElement.FLOAT_TYPE));
                applyArrayOffset(from, offset);
                part.add("from", from);

                JsonArray to = nbtFloatListToJson(props.getList("t", NbtElement.FLOAT_TYPE));
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
            }
            case "msh" -> {
                NbtCompound props = tag.getCompound("props");

                //mesh properties
                part.addProperty("visibility", true);
                part.addProperty("type", "mesh");

                //vertices
                JsonObject vertices = new JsonObject();
                NbtCompound vertList = props.getCompound("vertices");

                for (String s : vertList.getKeys()) {
                    JsonArray vertex = nbtFloatListToJson(vertList.getList(s, NbtElement.FLOAT_TYPE));
                    vertex.set(0, new JsonPrimitive(-vertex.get(0).getAsFloat()));
                    vertex.set(1, new JsonPrimitive(-vertex.get(1).getAsFloat()));
                    //applyArrayOffset(vertex, offset);
                    vertices.add(s, vertex);
                }

                //faces
                JsonObject faces = new JsonObject();
                NbtList faceList = props.getList("faces", NbtElement.LIST_TYPE);
                int i = 0;
                for (NbtElement faceNbt : faceList) {
                    JsonObject face = new JsonObject();
                    face.addProperty("texture", 0f);

                    JsonArray vert = new JsonArray();
                    JsonObject uv = new JsonObject();

                    for (NbtElement vertexNbt : ((NbtList) faceNbt)) {
                        NbtCompound vertex = (NbtCompound) vertexNbt;
                        String id = vertex.getString("id");

                        uv.add(id, nbtFloatListToJson(vertex.getList("uv", NbtElement.FLOAT_TYPE)));
                        vert.add(id);
                    }

                    face.add("vertices", vert);
                    face.add("uv", uv);
                    faces.add(i++ + "", face);
                }

                part.add("vertices", vertices);
                part.add("faces", faces);

                elements.add(part);
                outliner.add(uuid);
            }
            case "na" -> {
                //fix pivot offset
                Vec3f newOffset = offset.copy();

                for (Map.Entry<String, Vec3f> entry : PLAYER_MODEL_OFFSETS.entrySet()) {
                    if (name.contains(entry.getKey())) {
                        newOffset = entry.getValue();
                        JsonArray pivot;

                        if (tag.contains("piv")) {
                            pivot = nbtFloatListToJson(tag.getList("piv", NbtElement.FLOAT_TYPE));
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
                    NbtList childList = tag.getList("chld", NbtElement.COMPOUND_TYPE);

                    for (NbtElement nbtElement : childList) {
                        player = parseModelPart((NbtCompound) nbtElement, elements, children, newOffset) || player;
                    }
                }

                part.add("children", children);
                outliner.add(part);
            }
        }

        return player;
    }

    public static void applyArrayOffset(JsonArray array, Vec3f offset) {
        array.set(0, new JsonPrimitive(array.get(0).getAsFloat() + offset.getX()));
        array.set(1, new JsonPrimitive(array.get(1).getAsFloat() + offset.getY()));
        array.set(2, new JsonPrimitive(array.get(2).getAsFloat() + offset.getZ()));
    }

    public static JsonArray nbtFloatListToJson(NbtList list) {
        JsonArray json = new JsonArray();

        for (NbtElement nbtElement : list) {
            float val = ((NbtFloat) nbtElement).floatValue();
            json.add(val);
        }

        return json;
    }

    public static JsonObject buildFaceData(NbtCompound nbt) {
        JsonObject face = new JsonObject();
        JsonArray uv = nbtFloatListToJson(nbt.getList("uv", NbtElement.FLOAT_TYPE));

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
