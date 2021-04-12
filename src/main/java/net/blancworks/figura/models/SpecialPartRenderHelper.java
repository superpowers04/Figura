package net.blancworks.figura.models;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.ArrayList;

//Helps rendering parts in special ways, like in world space, on elytra or the nametag, or in held item position.
public class SpecialPartRenderHelper {
    public static final ArrayList<SpecialPartRenderCall> worldParts = new ArrayList<>(); //Parts to render in world-space.
    
    public static class SpecialPartRenderCall {
        public ModelPart part;
        public AbstractClientPlayerEntity targetEntity;
        
        //Extra data
        //Stuff like, left/right elytra wing, for example.
        public String extraData;
    }
}
