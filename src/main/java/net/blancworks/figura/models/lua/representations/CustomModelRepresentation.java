package net.blancworks.figura.models.lua.representations;

import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.models.CustomModelPart;
import net.blancworks.figura.models.lua.CustomScript;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.Vector3f;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

//Mostly identical to the default model representation, but with extended capabilities.
public class CustomModelRepresentation extends LuaRepresentation {
    public HashMap<String, ModelPartRepresentation> modelPartData = new HashMap<String, ModelPartRepresentation>();
    private int total_parts = 1; //Darn lua, starting at 1.

    public CustomModelRepresentation(CustomScript script) {
        super(script);

        total_parts = 1;
        LuaTable modelTable = new LuaTable();

        for (CustomModelPart part : script.playerData.model.all_parts) {
            generateForModelPart(modelTable, part);
        }

        scriptGlobals.set("custom_model", modelTable);
    }

    public void generateForModelPart(LuaTable table, CustomModelPart originalPart) {
        ModelPartRepresentation partData = new ModelPartRepresentation();
        partData.id = total_parts++;
        partData.targetPart = originalPart;
        LuaTable partTable = partData.getLuaTable();

        //Push data up
        //table.set(originalPart.name, partTable);
        table.set(partData.id, partTable);
        //modelPartData.put(originalPart.name, partData);
    }

    public static class ModelPartRepresentation {
        public CustomModelPart targetPart;
        public int id;

        public LuaTable getLuaTable() {
            LuaTable outTable = new LuaTable();

            outTable.set("setPos", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue self, LuaValue newPos) {
                    return setPos_lua(self, newPos);
                }
            });

            outTable.set("setRot", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue self, LuaValue newRot) {
                    return setRot_lua(self, newRot);
                }
            });

            outTable.set("setEnabled", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue newEnabled) {
                    return setEnabled_lua(arg1, newEnabled);
                }
            });

            outTable.set("getPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue self) {
                    return getPos_lua(self);
                }
            });

            outTable.set("getRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue self) {
                    return getRot_lua(self);
                }
            });

            outTable.set("getEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue self) {
                    return getEnabled_lua(self);
                }
            });

            return outTable;
        }


        public void setPos(float x, float y, float z) {
            targetPart.pos = new Vector3f(x,y,z);
        }

        public void setRot(float x, float y, float z) {
            targetPart.rot = new Vector3f(x,y,z);
        }

        public void setEnabled(boolean val) {
            targetPart.visible = val;
        }

        public LuaValue setPos_lua(LuaValue self, LuaValue newPos) {
            if (self.istable() == false || newPos.istable() == false)
                return LuaValue.NIL;

            LuaTable newPosTable = newPos.checktable();

            //If new position table is wrong size
            if (newPosTable.length() != 3)
                return LuaValue.NIL;

            setPos(
                    newPosTable.get(1).tofloat(),
                    newPosTable.get(2).tofloat(),
                    newPosTable.get(3).tofloat()
            );

            //Return 1 for success
            return LuaNumber.valueOf(1);
        }

        public LuaValue setRot_lua(LuaValue self, LuaValue newRot) {
            //Incorrect args.
            if (self.istable() == false || newRot.istable() == false)
                return LuaValue.NIL;

            LuaTable newRotTable = newRot.checktable();

            //If new position table is wrong size
            if (newRotTable.length() != 3)
                return LuaValue.NIL;

            setRot(
                    newRotTable.get(1).checknumber().tofloat(),
                    newRotTable.get(2).checknumber().tofloat(),
                    newRotTable.get(3).checknumber().tofloat()
            );

            //Return 1 for success
            return LuaNumber.valueOf(1);
        }

        public LuaValue setEnabled_lua(LuaValue self, LuaValue newEnabled) {
            //Self must be a table.
            if (self.istable() == false || newEnabled.isboolean() == false)
                return LuaValue.NIL;

            setEnabled(newEnabled.checkboolean());
            return LuaNumber.valueOf(1);
        }

        public LuaValue getPos_lua(LuaValue self) {
            LuaTable retTable = new LuaTable();
            retTable.set(1, LuaNumber.valueOf(targetPart.pos.getX()));
            retTable.set(2, LuaNumber.valueOf(targetPart.pos.getY()));
            retTable.set(3, LuaNumber.valueOf(targetPart.pos.getZ()));
            return retTable;
        }

        public LuaValue getRot_lua(LuaValue self) {
            LuaTable retTable = new LuaTable();
            retTable.set(1, LuaNumber.valueOf(targetPart.rot.getX()));
            retTable.set(2, LuaNumber.valueOf(targetPart.rot.getY()));
            retTable.set(3, LuaNumber.valueOf(targetPart.rot.getZ()));
            return retTable;
        }

        public LuaValue getEnabled_lua(LuaValue self) {
            return LuaBoolean.valueOf(targetPart.visible);
        }
    }
}
