package net.blancworks.figura.parsers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.blancworks.figura.models.CustomModel;

import java.lang.reflect.Type;

@Deprecated
public class BedrockModelDeserializer implements JsonDeserializer<CustomModel> {

    @Override
    public CustomModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
/*
        CustomModel model = new CustomModel();

        JsonObject obj = json.getAsJsonObject();
        JsonArray geo = obj.get("minecraft:geometry").getAsJsonArray();

        for (int i = 0; i < geo.size(); i++) {
            JsonObject part = geo.get(i).getAsJsonObject();
            JsonObject description = part.get("description").getAsJsonObject();
            JsonArray groups = part.get("bones").getAsJsonArray();
            
            int textureWidth = description.get("texture_width").getAsInt();
            int textureHeight = description.get("texture_height").getAsInt();

            HashMap<String, CustomModelPart> group_map = new HashMap<String, CustomModelPart>();

            for (int j = 0; j < groups.size(); j++) {
                JsonElement groupEl = groups.get(j);
                JsonObject group = groupEl.getAsJsonObject();
                String name = group.get("name").getAsString();
                JsonArray pivot = group.get("pivot").getAsJsonArray();
                JsonArray cubes = group.getAsJsonArray("cubes");

                JsonArray rotation = null;
                if (group.has("rotation"))
                    rotation = group.get("rotation").getAsJsonArray();


                CustomModelPart group_part = null;
                
                if (name.startsWith("MESH_")) {
                    String fileName = name.substring(5);

                    Path meshFilePath = FabricLoader.getInstance().getGameDir().getParent().resolve("model_files").resolve(fileName + ".obj");

                    if (Files.exists(meshFilePath)) {
                        group_part = CustomModelPartMesh.loadFromObj(meshFilePath);
                    }
                } else {
                    group_part = new CustomModelPart();
                }

                //If, somehow, a mesh file doesn't exist, we set it to be a normal model part.
                if(group_part == null)
                    group_part = new CustomModelPart();

                group_part.setTextureSize(textureWidth, textureHeight);

                group_part.name = name;

                if(name.contains("NOLOAD"))
                    group_part.visible = false;
                
                group_part.setPivot(
                        pivot.get(0).getAsFloat(),
                        -pivot.get(1).getAsFloat(),
                        pivot.get(2).getAsFloat()
                );
                if (rotation != null) {
                    group_part.pitch = (float) Math.toRadians(MathHelper.wrapDegrees(rotation.get(0).getAsFloat()));
                    group_part.yaw = (float) Math.toRadians(MathHelper.wrapDegrees(rotation.get(1).getAsFloat()));
                    group_part.roll = (float) Math.toRadians(MathHelper.wrapDegrees(rotation.get(2).getAsFloat()));
                }

                //Cubes
                if (cubes != null) {
                    for (int k = 0; k < cubes.size(); k++) {
                        JsonObject cube = cubes.get(k).getAsJsonObject();
                        JsonArray origin = cube.get("origin").getAsJsonArray();
                        JsonArray size = cube.get("size").getAsJsonArray();
                        JsonArray uv = cube.get("uv").getAsJsonArray();
                        float inflate = 0;
                        if (cube.has("inflate"))
                            inflate = cube.get("inflate").getAsFloat();

                        CustomModelPart cuboidPart = new CustomModelPart();
                        cuboidPart.setTextureSize(textureWidth, textureHeight);

                        cuboidPart.setPivot(
                                origin.get(0).getAsFloat() - group_part.pivotX,
                                -origin.get(1).getAsFloat() - size.get(1).getAsFloat() - group_part.pivotY,
                                origin.get(2).getAsFloat() - group_part.pivotZ
                        );

                        cuboidPart.setTextureOffset(
                                uv.get(0).getAsInt(),
                                uv.get(1).getAsInt()
                        );

                        cuboidPart.addCuboid(
                                0, 0, 0,
                                size.get(0).getAsFloat(),
                                size.get(1).getAsFloat(),
                                size.get(2).getAsFloat(),
                                inflate, inflate, inflate
                        );

                        group_part.addChild(cuboidPart);
                    }
                }

                group_map.put(name, group_part);
                if (group.has("parent")) {
                    CustomModelPart parent_part = group_map.get(group.get("parent").getAsString());
                    parent_part.addChild(group_part);
                } else {
                    model.all_parts.add(group_part);

                    if (name.contains("HEAD")) {
                        group_part.parentType = CustomModelPart.ParentType.Head;
                    } else if (name.contains("TORSO")) {
                        group_part.parentType = CustomModelPart.ParentType.Torso;
                    } else if (name.contains("LEFT_LEG")) {
                        group_part.parentType = CustomModelPart.ParentType.LeftLeg;
                    } else if (name.contains("RIGHT_LEG")) {
                        group_part.parentType = CustomModelPart.ParentType.RightLeg;
                    } else if (name.contains("LEFT_ARM")) {
                        group_part.parentType = CustomModelPart.ParentType.LeftArm;
                    } else if (name.contains("RIGHT_ARM")) {
                        group_part.parentType = CustomModelPart.ParentType.RightArm;
                    }
                }
            }
        }

        return model;*/
        return null;
    }
}
