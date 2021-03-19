package net.blancworks.figura.models.lua.representations.world.entity;

import net.blancworks.figura.models.lua.CustomScript;
import net.blancworks.figura.models.lua.representations.item.ItemStackRepresentation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import java.util.UUID;


public class PlayerRepresentation extends LivingEntityRepresentation<PlayerEntity> {

    PlayerInventory inv;

    public PlayerRepresentation(CustomScript customScript) {
        super(customScript);
    }

    @Override
    public String getDefaultTableKey() {
        return "player";
    }

    @Override
    public void fillLuaTable(LuaTable table) {
        super.fillLuaTable(table);

        table.set("getHeldItem", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (arg == null)
                    arg = LuaNumber.valueOf(1);

                if (!arg.isnumber())
                    return NIL;

                int hand = arg.checkint();
                ItemStack targetStack = null;

                if (hand == 1)
                    targetStack = targetEntity.getMainHandStack();
                else if (hand == 2)
                    targetStack = targetEntity.getOffHandStack();
                else
                    return NIL;

                LuaTable getItemRepresentation = ItemStackRepresentation.getTable(targetStack);
                return getItemRepresentation;
            }
        });
    }

    @Override
    public void getReferences() {
        super.getReferences();

        targetEntity = MinecraftClient.getInstance().world.getPlayerByUuid(script.playerData.playerId);

        if (targetEntity == null) return;
        inv = targetEntity.inventory;
    }

    public PlayerEntity getPlayerEntity() {
        return script.playerData.getEntityIfLoaded();
    }
}
