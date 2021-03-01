package net.blancworks.figura.models.parsers;

import com.google.gson.*;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;

import java.lang.reflect.Type;
import java.util.*;

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

        retModel.texWidth = resolution.get("width").getAsFloat();
        retModel.texHeight = resolution.get("height").getAsFloat();

        HashMap<UUID, JsonObject> elementsByUuid = sortElements(elements);
        HashMap<UUID, CustomModelPart> parsedParts = new HashMap<UUID, CustomModelPart>();


        //Parse out custom model parts from json objects.
        for (Map.Entry<UUID, JsonObject> entry : elementsByUuid.entrySet()) {
            UUID id = entry.getKey();
            JsonObject obj = entry.getValue();

            parsedParts.put(id, parseElement(obj, retModel));
        }

        for (JsonElement element : outliner) {
            if (element.isJsonObject()) {
                //If the element is a json object, it's a group, so parse the group.
                buildGroup(element.getAsJsonObject(), retModel, parsedParts, null);
            } else {
                //If the element is a string, it's an element, so just add it to the children.
                String s = element.getAsString();

                if (s != null)
                    retModel.all_parts.add(parsedParts.get(UUID.fromString(s)));
            }
        }

        return retModel;
    }

    //Builds out a group from a JsonObject that specifies the group in the outline.
    public void buildGroup(JsonObject group, CustomModel target, HashMap<UUID, CustomModelPart> allParts, CustomModelPart parent) {
        CustomModelPart groupPart = new CustomModelPart();


        if (group.has("name")) {
            groupPart.name = group.get("name").getAsString();

            //Find parent type.
            for (Map.Entry<String, CustomModelPart.ParentType> entry : nameParentTypeTags.entrySet()) {
                if(groupPart.name.contains(entry.getKey())) {
                    groupPart.parentType = entry.getValue();
                    break;
                }
            }
        }
        if (group.has("visibility")) groupPart.visible = group.get("visibility").getAsBoolean();
        if (group.has("origin")) groupPart.pivot = v3fFromJArray(group.get("origin").getAsJsonArray());
        if (group.has("rotation")) groupPart.rot = v3fFromJArray(group.get("rotation").getAsJsonArray());

        JsonArray children = group.get("children").getAsJsonArray();

        for (JsonElement child : children) {
            if (child.isJsonObject()) {
                //If the element is a json object, it's a group, so parse the group.
                buildGroup(child.getAsJsonObject(), target, allParts, groupPart);
            } else {
                //If the element is a string, it's an element, so just add it to the children.
                String s = child.getAsString();

                if (s != null)
                    groupPart.children.add(allParts.get(UUID.fromString(s)));
            }
        }

        //Add part.
        if (parent == null)
            target.all_parts.add(groupPart);
        else
            parent.children.add(groupPart);
    }

    public static final HashMap<String, CustomModelPart.ParentType> nameParentTypeTags = new HashMap<String, CustomModelPart.ParentType>(){{
        put("HEAD", CustomModelPart.ParentType.Head);
        put("TORSO", CustomModelPart.ParentType.Torso);
        put("LEFT_ARM", CustomModelPart.ParentType.LeftArm);
        put("RIGHT_ARM", CustomModelPart.ParentType.RightArm);
        put("LEFT_LEG", CustomModelPart.ParentType.LeftLeg);
        put("RIGHT_LEG", CustomModelPart.ParentType.RightLeg);
    }};
    
    public CustomModelPart parseElement(JsonObject elementObject, CustomModel target) {
        CustomModelPart elementPart = new CustomModelPart();

        if (elementObject.has("name")) {
            elementPart.name = elementObject.get("name").getAsString();
        }
        if (elementObject.has("visibility")) elementPart.visible = elementObject.get("visibility").getAsBoolean();

        Vector3f from = v3fFromJArray(elementObject.get("from").getAsJsonArray());
        Vector3f to = v3fFromJArray(elementObject.get("to").getAsJsonArray());
        if (elementObject.has("origin"))
            elementPart.pivot = v3fFromJArray(elementObject.get("origin").getAsJsonArray());
        if (elementObject.has("rotation"))
            elementPart.rot = v3fFromJArray(elementObject.get("rotation").getAsJsonArray());


        Vector3f size = to.copy();
        size.subtract(from);

        if (elementObject.has("uv_offset")) {
            elementPart.uOffset = elementObject.get("uv_offset").getAsJsonArray().get(0).getAsInt();
            elementPart.vOffset = elementObject.get("uv_offset").getAsJsonArray().get(1).getAsInt();
        }

        JsonObject facesObject = elementObject.get("faces").getAsJsonObject();

        //North
        {
            JsonObject faceData = facesObject.get("north").getAsJsonObject();
            generateFace(elementPart,
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    v4fFromJArray(faceData.get("uv").getAsJsonArray()),
                    target.texWidth, target.texHeight
            );
        }

        //South
        {
            JsonObject faceData = facesObject.get("south").getAsJsonObject();
            generateFace(elementPart,
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    v4fFromJArray(faceData.get("uv").getAsJsonArray()),
                    target.texWidth, target.texHeight
            );
        }

        //East
        {
            JsonObject faceData = facesObject.get("east").getAsJsonObject();
            generateFace(elementPart,
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    v4fFromJArray(faceData.get("uv").getAsJsonArray()),
                    target.texWidth, target.texHeight
            );
        }

        //West
        {
            JsonObject faceData = facesObject.get("west").getAsJsonObject();
            generateFace(elementPart,
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    v4fFromJArray(faceData.get("uv").getAsJsonArray()),
                    target.texWidth, target.texHeight
            );
        }

        //Top
        {
            JsonObject faceData = facesObject.get("up").getAsJsonObject();
            generateFace(elementPart,
                    new Vector3f(-to.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), to.getZ()),
                    new Vector3f(-from.getX(), -to.getY(), from.getZ()),
                    new Vector3f(-to.getX(), -to.getY(), from.getZ()),
                    v4fFromJArray(faceData.get("uv").getAsJsonArray()),
                    target.texWidth, target.texHeight
            );
        }

        //Bottom
        {
            JsonObject faceData = facesObject.get("down").getAsJsonObject();
            generateFace(elementPart,
                    new Vector3f(-to.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), from.getZ()),
                    new Vector3f(-from.getX(), -from.getY(), to.getZ()),
                    new Vector3f(-to.getX(), -from.getY(), to.getZ()),
                    v4fFromJArray(faceData.get("uv").getAsJsonArray()),
                    target.texWidth, target.texHeight
            );
        }


        return elementPart;
    }

    public void generateFace(CustomModelPart part, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector4f uv, float texWidth, float texHeight) {
        Vector3f nA = b.copy();
        nA.subtract(a);
        Vector3f nB = c.copy();
        nB.subtract(a);
        nA.cross(nB);
        nA.normalize();

        part.addVertex(b, uv.getX() / texWidth, uv.getW() / texHeight, nA);
        part.addVertex(a, uv.getZ() / texWidth, uv.getW() / texHeight, nA);
        part.addVertex(d, uv.getZ() / texWidth, uv.getY() / texHeight, nA);
        part.addVertex(c, uv.getX() / texWidth, uv.getY() / texHeight, nA);
    }

    public Vector3f v3fFromJArray(JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    public Vector4f v4fFromJArray(JsonArray array) {
        return new Vector4f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat());
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
