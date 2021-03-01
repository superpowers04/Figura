package net.blancworks.figura.models.parsers;

import com.google.gson.*;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockbenchModelDeserializer implements JsonDeserializer<CustomModel> {


    @Override
    public CustomModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        CustomModel retModel = new CustomModel();

        JsonObject root = json.getAsJsonObject();
        JsonObject meta = root.get("meta").getAsJsonObject();
        String name = root.get("name").getAsString();
        JsonObject resolution = root.get("resolution").getAsJsonObject();
        JsonArray elements = root.get("elements").getAsJsonArray();
        JsonArray outliner = root.get("outliner").getAsJsonArray();
        JsonArray textures = root.get("textures").getAsJsonArray();

        HashMap<UUID, JsonObject> elementsByUuid = sortElements(elements);
        HashMap<UUID, JsonObject> groupsByUuid = sortElements(outliner);
        HashMap<UUID, CustomModelPart> parsedParts = new HashMap<UUID, CustomModelPart>();

        parseGroupsAndElements(retModel, parsedParts, groupsByUuid, elementsByUuid);

        return retModel;
    }

    //Parses out all the groups and elements into the custom model.
    public void parseGroupsAndElements(CustomModel model, HashMap<UUID, CustomModelPart> parsedParts, HashMap<UUID, JsonObject> groups, HashMap<UUID, JsonObject> elements) {

        ArrayList<UUID> rootParts = new ArrayList<UUID>();

        //Foreach group, make a custom model part for it.
        for (Map.Entry<UUID, JsonObject> group : groups.entrySet()) {
            JsonObject groupObj = group.getValue();
            UUID id = UUID.fromString(groupObj.get("uuid").getAsString());
            parsedParts.put(id, parseGroup(groupObj));
            rootParts.add(id);
        }

        for (Map.Entry<UUID, JsonObject> element : elements.entrySet()) {
            JsonObject elementObject = element.getValue();
            UUID id = UUID.fromString(elementObject.get("uuid").getAsString());
            parsedParts.put(id, parseElement(elementObject));
            rootParts.add(id);
        }

        for (Map.Entry<UUID, CustomModelPart> entry : parsedParts.entrySet()) {

            //Get stuff from entry
            UUID id = entry.getKey();
            CustomModelPart part = entry.getValue();

            //If this part is a group
            if(groups.containsKey(id)){

            }
        }

    }

    public CustomModelPart parseGroup(JsonObject groupObject) {
        CustomModelPart groupPart = new CustomModelPart();

        if(groupObject.has("name")) groupPart.name = groupObject.get("name").getAsString();
        if(groupObject.has("visibility")) groupPart.visible = groupObject.get("visibility").getAsBoolean();
        if(groupObject.has("origin")) groupPart.pos = v3fFromJArray(groupObject.get("origin").getAsJsonArray());
        if(groupObject.has("rotation")) groupPart.pos = v3fFromJArray(groupObject.get("rotation").getAsJsonArray());

        return groupPart;
    }

    public CustomModelPart parseElement(JsonObject elementObject){
        CustomModelPart elementPart = new CustomModelPart();

        if(elementObject.has("name")) elementPart.name = elementObject.get("name").getAsString();
        if(elementObject.has("visibility")) elementPart.visible = elementObject.get("visibility").getAsBoolean();

        Vector3f from = v3fFromJArray(elementObject.get("from").getAsJsonArray());
        Vector3f to = v3fFromJArray(elementObject.get("to").getAsJsonArray());
        if(elementObject.has("origin")) elementPart.pivot = v3fFromJArray(elementObject.get("origin").getAsJsonArray());
        if(elementObject.has("rotation")) elementPart.rot = v3fFromJArray(elementObject.get("rotation").getAsJsonArray());



        Vector3f size = to.copy();
        size.subtract(from);

        if(elementObject.has("uv_offset")) {
            elementPart.uOffset = elementObject.get("uv_offset").getAsJsonArray().get(0).getAsInt();
            elementPart.vOffset = elementObject.get("uv_offset").getAsJsonArray().get(1).getAsInt();
        }

        JsonObject facesObject = elementObject.get("faces").getAsJsonObject();

        //North
        {
            JsonObject faceData = facesObject.get("north").getAsJsonObject();
            generateFace(elementPart,
                    new Vector3f(from.getX(), from.getY(), from.getZ()),
                    new Vector3f(to.getX(), from.getY(), from.getZ()),
                    new Vector3f(to.getX(), to.getY(), from.getZ()),
                    new Vector3f(from.getX(), to.getY(), from.getZ()),
                    v4fFromJArray(faceData.get("uv").getAsJsonArray())
            );
        }


        return elementPart;
    }

    public void generateFace(CustomModelPart part, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector4f uv){
        Vector3f nA = b.copy();
        nA.subtract(a);
        Vector3f nB = c.copy();
        nB.subtract(a);
        nA.cross(nB);
        nA.normalize();

        part.addVertex(a, uv.getX(), uv.getY(), nA);
        part.addVertex(b, uv.getZ(), uv.getY(), nA);
        part.addVertex(c, uv.getZ(), uv.getW(), nA);
        part.addVertex(d, uv.getX(), uv.getW(), nA);
    }

    public Vector3f v3fFromJArray(JsonArray array){
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(),array.get(2).getAsFloat());
    }

    public Vector4f v4fFromJArray(JsonArray array){
        return new Vector4f(array.get(0).getAsFloat(), array.get(1).getAsFloat(),array.get(2).getAsFloat(),array.get(3).getAsFloat());
    }

    //Sorts out all the things in a json array out by UUID.
    public HashMap<UUID, JsonObject> sortElements(JsonArray elementContainer) {
        HashMap<UUID, JsonObject> objects = new HashMap<>();
        for (JsonElement jsonElement : elementContainer) {
            if (jsonElement.isJsonObject() == false)
                continue;
            JsonObject obj = jsonElement.getAsJsonObject();

            if (obj.has("uuid") == false)
                continue;
            objects.put(UUID.fromString(obj.get("uuid").getAsString()), obj);


            if (obj.has("children")) {
                JsonElement children = obj.get("children");
                if (children.isJsonArray() == true) {
                    JsonArray childrenArray = children.getAsJsonArray();

                    objects.putAll(sortElements(childrenArray));
                }
            }
        }
        return objects;
    }
}
