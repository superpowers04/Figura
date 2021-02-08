package net.blancworks.figura.models;

import net.blancworks.figura.FiguraMod;

public class DebugModel extends CustomModel{
    public DebugModel() {
        CustomModelPart part = new CustomModelPart();
        for(int i = 0; i < 15; i++)
            part.addCuboid(i*10,0,0,8,8,8);
        
        all_parts.add(part);
    }

    @Override
    public int getMaxRenderAmount() {
        return FiguraMod.curr_player.inventory.selectedSlot;
    }
}
