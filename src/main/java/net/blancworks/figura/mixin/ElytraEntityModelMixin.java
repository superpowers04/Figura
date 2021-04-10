package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ElytraEntityModelAccess;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ElytraEntityModel.class)
public class ElytraEntityModelMixin <T extends LivingEntity> extends AnimalModel<T> implements ElytraEntityModelAccess {

    @Shadow @Final private ModelPart field_3365;
    @Shadow @Final private ModelPart field_3364;

    @Override
    @Shadow
    protected Iterable<ModelPart> getHeadParts() {
        return null;
    }

    @Override
    @Shadow
    protected Iterable<ModelPart> getBodyParts() {
        return null;
    }

    @Override
    @Shadow
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }

    public ModelPart getLeftWing(){
        return field_3365;
    }
    
    public ModelPart getRightWing(){
        return field_3364;
    }
}
