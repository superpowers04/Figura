package net.blancworks.figura.models.parsers;

import com.google.gson.*;
import net.blancworks.figura.models.CustomModelPart.ParentType;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec3f;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockbenchModelDeserializer {

    private static final Map<String, PartData> NAME_PARENT_TYPE_TAGS = new LinkedHashMap<>() {{
            put("HEAD", new PartData(ParentType.Head));
            put("TORSO", new PartData(ParentType.Torso));
            put("LEFT_ARM", new PartData(ParentType.LeftArm));
            put("RIGHT_ARM", new PartData(ParentType.RightArm));
            put("LEFT_LEG", new PartData(ParentType.LeftLeg));
            put("RIGHT_LEG", new PartData(ParentType.RightLeg));
            put("NO_PARENT", new PartData(ParentType.WORLD));
            put("LEFT_HELD_ITEM", new PartData(ParentType.LeftItemOrigin));
            put("RIGHT_HELD_ITEM", new PartData(ParentType.RightItemOrigin));
            put("LEFT_ELYTRA_ORIGIN", new PartData(ParentType.LeftElytraOrigin));   //those needs to be checked first, otherwise "LEFT_ELYTRA" and
            put("RIGHT_ELYTRA_ORIGIN", new PartData(ParentType.RightElytraOrigin)); //"RIGHT_ELYTRA" will be checked first, giving the wrong entry
            put("LEFT_PARROT", new PartData(ParentType.LeftParrotOrigin));
            put("RIGHT_PARROT", new PartData(ParentType.RightParrotOrigin));
            put("LEFT_ELYTRA", new PartData(ParentType.LeftElytra));
            put("RIGHT_ELYTRA", new PartData(ParentType.RightElytra));
            put("LEFT_SPYGLASS", new PartData(ParentType.LeftSpyglass));
            put("RIGHT_SPYGLASS", new PartData(ParentType.RightSpyglass));
            put("CAMERA", new PartData(ParentType.Camera));
            put("SKULL", new PartData(ParentType.Skull));
            put("HUD", new PartData(ParentType.Hud));
    }};
    private static final Map<String, PartData> NAME_MIMIC_TYPE_TAGS = new LinkedHashMap<>() {{
            put("MIMIC_HEAD", new PartData(ParentType.Head, true));
            put("MIMIC_TORSO", new PartData(ParentType.Torso, true));
            put("MIMIC_LEFT_ARM", new PartData(ParentType.LeftArm, true));
            put("MIMIC_RIGHT_ARM", new PartData(ParentType.RightArm, true));
            put("MIMIC_LEFT_LEG", new PartData(ParentType.LeftLeg, true));
            put("MIMIC_RIGHT_LEG", new PartData(ParentType.RightLeg, true));
    }};
    private static final Map<String, PartData> PLAYER_SKIN_REMAPS = new LinkedHashMap<>() {{
        put("Head", new PartData(ParentType.Head, new Vec3f(0, -24, 0)));
        put("Body", new PartData(ParentType.Torso, new Vec3f(0, -24, 0)));
        put("RightArm", new PartData(ParentType.RightArm, new Vec3f(-5, -22, 0)));
        put("LeftArm", new PartData(ParentType.LeftArm, new Vec3f(5, -22, 0)));
        put("RightLeg", new PartData(ParentType.RightLeg, new Vec3f(-2, -12, 0)));
        put("LeftLeg", new PartData(ParentType.LeftLeg, new Vec3f(2, -12, 0)));
    }};

    private static class PartData {
        public static final PartData DEFAULT_PARENT = new PartData(ParentType.Model);
        public final String parentType;

        public Vec3f offset;
        public boolean mimic = false;

        public PartData(ParentType parentType) {
            this.parentType = parentType.name();
        }

        public PartData(ParentType parentType, Vec3f offset) {
            this(parentType);
            this.offset = offset;
        }

        public PartData(ParentType parentType, boolean mimic) {
            this(parentType);
            this.mimic = mimic;
        }
    }

    public static NbtCompound deserialize(String json, boolean isPlayerModel) throws JsonParseException {
        NbtCompound retModel = new NbtCompound();

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject meta = root.get("meta").getAsJsonObject();
        JsonObject resolution = root.get("resolution").getAsJsonObject();
        JsonArray elements = root.get("elements").getAsJsonArray();
        JsonArray outliner = root.get("outliner").getAsJsonArray();

        //player_model format
        isPlayerModel = isPlayerModel || (meta.has("model_format") && meta.get("model_format").getAsString().equals("skin"));

        //animations
        Map<String, NbtList> animationMap = new HashMap<>();
        if (root.has("animations")) {
            JsonArray animations = root.get("animations").getAsJsonArray();

            //parse animations
            try {
                NbtList animationsNbt = parseAnimations(animations, animationMap);
                retModel.put("anim", animationsNbt);
            } catch (Exception e) {
                System.out.println("Failed to load animations");
                e.printStackTrace();
            }
        }

        //texture
        retModel.put("uv", new NbtList() {{
            add(NbtFloat.of(resolution.get("width").getAsFloat()));
            add(NbtFloat.of(resolution.get("height").getAsFloat()));
        }});

        //sort parts
        Map<String, JsonObject> elementMap = sortElements(elements);

        //parse outliner, which also parse the parts and finishes the model loading
        retModel.put("parts", buildElements(outliner, elementMap, isPlayerModel, new Vec3f(0f, 0f, 0f), animationMap));

        return retModel;
    }

    public static Map<String, JsonObject> sortElements(JsonArray elementContainer) {
        Map<String, JsonObject> objects = new HashMap<>();

        for (JsonElement jsonElement : elementContainer) {
            if (!jsonElement.isJsonObject())
                continue;

            JsonObject obj = jsonElement.getAsJsonObject();
            if (!obj.has("uuid"))
                continue;

            objects.put(obj.get("uuid").getAsString(), obj);
        }

        return objects;
    }

    public static NbtList parseAnimations(JsonArray animations, Map<String, NbtList> animationMap) {
        NbtList anims = new NbtList();

        for (JsonElement jsonElement : animations) {
            if (!jsonElement.isJsonObject())
                continue;

            JsonObject obj = jsonElement.getAsJsonObject();
            NbtCompound anim = new NbtCompound();

            //animation properties
            NbtString name = NbtString.of(obj.get("name").getAsString());
            anim.put("nm", name);
            anim.put("loop", NbtString.of(obj.get("loop").getAsString()));
            anim.put("len", NbtFloat.of(obj.get("length").getAsFloat()));

            if (obj.has("anim_time_update"))
                anim.put("time", NbtFloat.of(tryGetFloat(obj.get("anim_time_update"))));
            if (obj.has("blend_weight")) {
                float f;
                try {
                    f = obj.get("blend_weight").getAsFloat();
                } catch (Exception ignored) {
                    f = 1f;
                }
                anim.put("bld", NbtFloat.of(f));
            }
            if (obj.has("start_delay"))
                anim.put("sdel", NbtFloat.of(tryGetFloat(obj.get("start_delay"))));
            if (obj.has("loop_delay"))
                anim.put("ldel", NbtFloat.of(tryGetFloat(obj.get("loop_delay"))));

            //animators
            if (obj.has("animators")) {
                JsonObject animators = obj.getAsJsonObject("animators");
                for (Map.Entry<String, JsonElement> animator : animators.entrySet()) {
                    if (animator.getKey().length() < 36) continue;

                    JsonObject animObj = animator.getValue().getAsJsonObject();
                    NbtCompound animNbt = new NbtCompound();

                    //animator properties
                    NbtList keyFrames = new NbtList();
                    for (JsonElement keyFrameElement : animObj.get("keyframes").getAsJsonArray()) {
                        JsonObject keyFrameObj = keyFrameElement.getAsJsonObject();
                        NbtCompound keyFrame = new NbtCompound();

                        //keyframe properties
                        keyFrame.put("type", NbtString.of(keyFrameObj.get("channel").getAsString()));
                        keyFrame.put("int", NbtString.of(keyFrameObj.get("interpolation").getAsString()));
                        keyFrame.put("time", NbtFloat.of(keyFrameObj.get("time").getAsFloat()));

                        //keyframe pos/scale/rot
                        JsonObject dataPoints = keyFrameObj.getAsJsonArray("data_points").get(0).getAsJsonObject();
                        NbtList data = new NbtList();
                        data.add(NbtFloat.of(tryGetFloat(dataPoints.get("x"))));
                        data.add(NbtFloat.of(tryGetFloat(dataPoints.get("y"))));
                        data.add(NbtFloat.of(tryGetFloat(dataPoints.get("z"))));

                        keyFrame.put("data", data);
                        keyFrames.add(keyFrame);
                    }

                    animNbt.put("keyf", keyFrames);
                    animNbt.put("id", name);

                    String uuid = animator.getKey();
                    if (animationMap.containsKey(uuid)) {
                        NbtList list = animationMap.get(uuid);
                        list.add(animNbt);
                    } else {
                        animationMap.put(animator.getKey(), new NbtList() {{
                            add(animNbt);
                        }});
                    }
                }
            }

            anims.add(anim);
        }

        return anims;
    }

    public static NbtList buildElements(JsonArray group, Map<String, JsonObject> elementMap, boolean overrideAsPlayerModel, Vec3f offset, Map<String, NbtList> animationMap) {
        NbtList parts = new NbtList();
        for (JsonElement jsonElement : group) {
            NbtCompound nbt = null;

            //if the element is a json object, it's a group, otherwise its a part
            if (jsonElement.isJsonObject()) {
                try {
                    nbt = buildGroup(jsonElement.getAsJsonObject(), elementMap, overrideAsPlayerModel, offset, animationMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    nbt = buildPart(elementMap.get(jsonElement.getAsString()), offset);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (nbt != null)
                parts.add(nbt);
        }

        return parts;
    }

    public static NbtCompound buildGroup(JsonObject group, Map<String, JsonObject> elementMap, boolean playerModel, Vec3f offset, Map<String, NbtList> animationMap) {
        if (!group.has("name"))
            return null;

        NbtCompound groupNbt = new NbtCompound();

        //name
        String name = group.get("name").getAsString();
        groupNbt.put("nm", NbtString.of(name));

        //visibility
        if (group.has("visibility") && !group.get("visibility").getAsBoolean())
            groupNbt.put("vb", NbtByte.of(false));

        //parent type
        if (!group.has("ignoreKeyword") || !group.get("ignoreKeyword").getAsBoolean()) {
            PartData parent = getParentType(name, playerModel);

            if (parent.mimic) groupNbt.put("mmc", NbtByte.of(true));
            if (parent.offset != null) offset = parent.offset.copy();

            groupNbt.put("ptype", NbtString.of(parent.parentType));
        }

        //pivot
        if (group.has("origin")) {
            Vec3f corrected = v3fFromJArray(group.get("origin").getAsJsonArray());
            corrected.set(corrected.getX(), corrected.getY(), -corrected.getZ());

            Vec3f thisOffset = offset.copy();
            thisOffset.add(corrected);

            groupNbt.put("piv", vec3fToNbt(thisOffset));
        }

        //rotation
        if (group.has("rotation"))
            groupNbt.put("rot", vec3fToNbt(v3fFromJArray(group.get("rotation").getAsJsonArray())));

        //animations
        String uuid = group.get("uuid").getAsString();
        if (animationMap.containsKey(uuid))
            groupNbt.put("anims", animationMap.get(uuid));

        //children
        if (group.has("children")) {
            JsonArray children = group.get("children").getAsJsonArray();
            NbtList child = buildElements(children, elementMap, playerModel, offset, animationMap);
            if (child.size() > 0) groupNbt.put("chld", child);
        }

        return groupNbt;
    }

    public static PartData getParentType(String name, boolean playerModel) {
        //test for mimics
        for (Map.Entry<String, PartData> entry : NAME_MIMIC_TYPE_TAGS.entrySet()) {
            if (name.contains(entry.getKey()))
                return entry.getValue();
        }

        //test for parent parts
        for (Map.Entry<String, PartData> entry : NAME_PARENT_TYPE_TAGS.entrySet()) {
            if (name.contains(entry.getKey()))
                return entry.getValue();
        }

        //test for player model parts
        if (playerModel) {
            for (Map.Entry<String, PartData> entry : PLAYER_SKIN_REMAPS.entrySet()) {
                if (name.contains(entry.getKey()))
                    return entry.getValue();
            }
        }

        //if part not found returns default
        return PartData.DEFAULT_PARENT;
    }

    public static NbtCompound buildPart(JsonObject part, Vec3f offset) {
        if (!part.has("name"))
            return null;

        boolean mesh = false;
        String partType;

        if (part.has("type")) {
            partType = part.get("type").getAsString();

            if (partType.equals("null_object"))
                return null;

            mesh = partType.equals("mesh");
        }

        NbtCompound partNbt = new NbtCompound();

        //name
        partNbt.put("nm", NbtString.of(part.get("name").getAsString()));

        //visibility
        if (part.has("visibility") && !part.get("visibility").getAsBoolean())
            partNbt.put("vb", NbtByte.of(false));

        //pivot
        if (part.has("origin")) {
            Vec3f corrected = v3fFromJArray(part.get("origin").getAsJsonArray());
            corrected.set(corrected.getX(), corrected.getY(), -corrected.getZ());
            corrected.add(offset);
            partNbt.put("piv", vec3fToNbt(corrected));
        }

        //rotation
        if (part.has("rotation"))
            partNbt.put("rot", vec3fToNbt(v3fFromJArray(part.get("rotation").getAsJsonArray())));

        if (mesh) buildMesh(part, partNbt);
        else buildCuboid(part, partNbt, offset);

        return partNbt;
    }

    public static void buildCuboid(JsonObject partJson, NbtCompound partNbt, Vec3f offset) {
        //part type
        partNbt.put("pt", NbtString.of("cub"));

        //faces
        JsonObject facesObject = partJson.get("faces").getAsJsonObject();
        NbtCompound properties = new NbtCompound();

        if (partJson.has("inflate"))
            properties.put("inf", NbtFloat.of(partJson.get("inflate").getAsFloat()));

        Vec3f from = v3fFromJArray(partJson.get("from").getAsJsonArray());
        Vec3f to = v3fFromJArray(partJson.get("to").getAsJsonArray());

        from.add(offset);
        to.add(offset);

        properties.put("f", vec3fToNbt(from));
        properties.put("t", vec3fToNbt(to));

        properties.put("n", getFaceData(facesObject.get("north")));
        properties.put("s", getFaceData(facesObject.get("south")));
        properties.put("e", getFaceData(facesObject.get("east")));
        properties.put("w", getFaceData(facesObject.get("west")));
        properties.put("u", getFaceData(facesObject.get("up")));
        properties.put("d", getFaceData(facesObject.get("down")));

        partNbt.put("props", properties);
    }

    public static NbtElement getFaceData(JsonElement element) {
        JsonObject faceObj = element.getAsJsonObject();
        NbtCompound face = new NbtCompound();

        //uv
        JsonArray uv = faceObj.getAsJsonArray("uv");
        NbtList uvList = new NbtList() {{
           add(NbtFloat.of(uv.get(0).getAsFloat()));
           add(NbtFloat.of(uv.get(1).getAsFloat()));
           add(NbtFloat.of(uv.get(2).getAsFloat()));
           add(NbtFloat.of(uv.get(3).getAsFloat()));
        }};

        //texture
        JsonElement texture = faceObj.get("texture");
        if (texture != null && !texture.isJsonNull())
            face.put("texture", NbtFloat.of(texture.getAsFloat()));

        //rotation
        if (faceObj.has("rotation"))
            face.put("rotation", NbtFloat.of(faceObj.get("rotation").getAsFloat()));

        face.put("uv", uvList);
        return face;
    }

    public static void buildMesh(JsonObject partJson, NbtCompound partNbt) {
        //part type
        partNbt.put("pt", NbtString.of("msh"));

        //faces
        JsonObject facesObject = partJson.get("faces").getAsJsonObject();
        JsonObject verticesObject = partJson.get("vertices").getAsJsonObject();
        NbtCompound properties = new NbtCompound();

        NbtCompound verticesList = new NbtCompound();
        HashMap<String, String> verticesMap = new HashMap<>();

        long i = 0;
        for (Map.Entry<String, JsonElement> entry : verticesObject.entrySet()) {
            Vec3f pos = v3fFromJArray(entry.getValue().getAsJsonArray());
            NbtList vertexPos = new NbtList();
            vertexPos.add(NbtFloat.of(-pos.getX()));
            vertexPos.add(NbtFloat.of(-pos.getY()));
            vertexPos.add(NbtFloat.of(pos.getZ()));

            String key = Long.toHexString(i);
            verticesList.put(key, vertexPos);
            verticesMap.put(entry.getKey(), key);
            i++;
        }

        properties.put("vertices", verticesList);

        //create data for each face of the mesh
        NbtList meshFacesList = new NbtList();

        facesObject.entrySet().forEach(entry -> {
            JsonObject faceObject = entry.getValue().getAsJsonObject();
            NbtList curFaceTag = new NbtList();

            //build vertex -> uv map
            NbtCompound uvs = new NbtCompound();
            faceObject.getAsJsonObject("uv").entrySet().forEach(uvEntry -> {
                String vertexName = verticesMap.get(uvEntry.getKey());
                JsonArray uvEntries = uvEntry.getValue().getAsJsonArray();

                NbtList uvList = new NbtList();
                uvList.add(NbtFloat.of(uvEntries.get(0).getAsFloat()));
                uvList.add(NbtFloat.of(uvEntries.get(1).getAsFloat()));
                uvs.put(vertexName, uvList);
            });

            //read vertex data then pack id and uv and add to this face nbt
            faceObject.getAsJsonArray("vertices").forEach(element -> {
                NbtCompound vertex = new NbtCompound();
                String key = verticesMap.get(element.getAsString());

                vertex.put("id", NbtString.of(key));
                vertex.put("uv", uvs.get(key));

                curFaceTag.add(vertex);
            });

            //add this face to the faces list
            meshFacesList.add(curFaceTag);
        });

        properties.put("faces", meshFacesList);
        partNbt.put("props", properties);
    }

    public static Vec3f v3fFromJArray(JsonArray array) {
        return new Vec3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    public static NbtList vec3fToNbt(Vec3f vec) {
        NbtList nbt = new NbtList();

        nbt.add(NbtFloat.of(vec.getX()));
        nbt.add(NbtFloat.of(vec.getY()));
        nbt.add(NbtFloat.of(vec.getZ()));

        return nbt;
    }

    public static float tryGetFloat(JsonElement element) {
        try {
            return element.getAsFloat();
        } catch (Exception ignored) {
            return 0f;
        }
    }
}
