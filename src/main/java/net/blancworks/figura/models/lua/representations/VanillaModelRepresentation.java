package net.blancworks.figura.models.lua.representations;


import net.blancworks.figura.access.ModelPartAccess;
import net.blancworks.figura.models.lua.CustomScript;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.Vector3f;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

//The representation lua-side for the vanilla model.
//Tracks things like enable/disable state, positions and rotations, that sort.
public class VanillaModelRepresentation extends LuaRepresentation {

    public HashMap<Integer, ModelPartData> modelPartData = new HashMap<Integer, ModelPartData>();

    public VanillaModelRepresentation(CustomScript script) {
        super(script);

        LuaTable vanillaModelTable = new LuaTable();

        generateForModelPart(0, "HEAD", vanillaModelTable, script.playerData.vanillaModel.head);
        generateForModelPart(1, "TORSO", vanillaModelTable, script.playerData.vanillaModel.torso);
        generateForModelPart(2, "LEFT_ARM", vanillaModelTable, script.playerData.vanillaModel.leftArm);
        generateForModelPart(3, "RIGHT_ARM", vanillaModelTable, script.playerData.vanillaModel.rightArm);
        generateForModelPart(4, "LEFT_LEG", vanillaModelTable, script.playerData.vanillaModel.leftLeg);
        generateForModelPart(5, "RIGHT_LEG", vanillaModelTable, script.playerData.vanillaModel.rightLeg);
        
        scriptGlobals.set("vanilla_model", vanillaModelTable);
    }

    public void applyModelTransforms(PlayerEntityModel model) {
        modelPartData.get(0).copyIntoPart();
        modelPartData.get(1).copyIntoPart();
        modelPartData.get(2).copyIntoPart();
        modelPartData.get(3).copyIntoPart();
        modelPartData.get(4).copyIntoPart();
        modelPartData.get(5).copyIntoPart();
    }

    public void generateForModelPart(int id, String name, LuaTable table, ModelPart originalPart) {
        ModelPartData partData = new ModelPartData();
        partData.originModel = originalPart;
        LuaTable partTable = partData.getLuaTable();
        table.set(name, partTable);
        modelPartData.put(id, partData);
    }

    public static class ModelPartData {
        public Vector3f pos = null;
        public Vector3f rot = null;
        public Boolean enabled = null;
        
        public ModelPart originModel;

        public void copyIntoPart() {
            ModelPartAccess access = (ModelPartAccess)(Object)originModel;
            
            access.setAdditionalPos(pos);
            access.setAdditionalRot(rot);
        }

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

            outTable.set("getOriginPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue self) {
                    return getOriginPos_lua(self);
                }
            });
            outTable.set("getOriginRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue self) {
                    return getOriginRot_lua(self);
                }
            });

            return outTable;
        }


        public void setPos(float x, float y, float z) {
            pos = new Vector3f(x, y, z);
        }

        public void setRot(float x, float y, float z) {
            rot = new Vector3f(x, y, z);
        }

        public LuaTable getPos() {
            LuaTable retVal = new LuaTable();

            return retVal;
        }

        public LuaTable getRot() {
            LuaTable retVal = new LuaTable();

            return retVal;
        }

        public void setEnabled(boolean val) {
            enabled = val;
        }
        
        public LuaValue setPos_lua(LuaValue self, LuaValue newPos) {
            if (self.istable() == false)
                return LuaValue.NIL;

            //If setting the rot to null
            if (newPos.isnil()) {
                pos = null;
                return LuaNumber.valueOf(1);
            }

            //If the new rotation isn't a table, we ignore it.
            if (newPos.istable() == false)
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
            if (self.istable() == false)
                return LuaValue.NIL;

            //If setting the rot to null
            if (newRot.isnil()) {
                rot = null;
                return LuaNumber.valueOf(1);
            }

            //If the new rotation isn't a table, we ignore it.
            if (newRot.istable() == false)
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
            if (self.istable() == false)
                return LuaValue.NIL;
            
            if(newEnabled.isnil()){
                enabled = null;
                return LuaNumber.valueOf(1);
            } else if(newEnabled.isboolean()){
                enabled = newEnabled.checkboolean();
                return LuaNumber.valueOf(1);
            }

            return LuaValue.NIL;
        }

        public LuaValue getPos_lua(LuaValue self) {
            LuaTable retTable = new LuaTable();
            retTable.set(1, LuaNumber.valueOf(pos.getX()));
            retTable.set(2, LuaNumber.valueOf(pos.getY()));
            retTable.set(3, LuaNumber.valueOf(pos.getZ()));
            return retTable;
        }

        public LuaValue getRot_lua(LuaValue self) {
            LuaTable retTable = new LuaTable();
            retTable.set(1, LuaNumber.valueOf(rot.getX()));
            retTable.set(2, LuaNumber.valueOf(rot.getY()));
            retTable.set(3, LuaNumber.valueOf(rot.getZ()));
            return retTable;
        }

        public LuaValue getEnabled_lua(LuaValue self) {
            if(enabled == null){

            }
            
            return LuaValue.NIL;
        }

        //Gets the position of the piece without the additional bonus modified from the script.
        public LuaValue getOriginPos_lua(LuaValue self) {
            LuaTable retTable = new LuaTable();
            retTable.set(1, LuaNumber.valueOf(originModel.pivotX));
            retTable.set(2, LuaNumber.valueOf(originModel.pivotY));
            retTable.set(3, LuaNumber.valueOf(originModel.pivotZ));
            return retTable;
        }

        //Gets the rotation of the piece without the additional bonus modified from the script.
        public LuaValue getOriginRot_lua(LuaValue self) {
            LuaTable retTable = new LuaTable();
            retTable.set(1, LuaNumber.valueOf(originModel.pitch));
            retTable.set(2, LuaNumber.valueOf(originModel.yaw));
            retTable.set(3, LuaNumber.valueOf(originModel.roll));
            return retTable;
        }
    }
}
