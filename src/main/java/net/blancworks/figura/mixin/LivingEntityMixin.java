package net.blancworks.figura.mixin;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Unique
    private ArrayList<CustomModelPart> particleSources;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At("HEAD"), method = "spawnItemParticles")
    public void spawnItemParticles(ItemStack stack, int count, CallbackInfo ci) {
        if ((LivingEntity)(Object)this instanceof PlayerEntity player) {
            PlayerData data = PlayerDataManager.getDataForPlayer(player.getUuid());
            if (data != null && data.script != null) {
                this.particleSources = new ArrayList<>(data.model.getSpecialParts(CustomModelPart.ParentType.ParticleSource, p -> p.visible));
            }
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"), method = "spawnItemParticles")
    public void addParticle(World world, ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        if (this.particleSources != null && !this.particleSources.isEmpty()) {
            CustomModelPart target = particleSources.get(world.getRandom().nextInt(particleSources.size()));

            Vec3d pos = new Vec3d(x, y, z);
            pos = pos.subtract(this.getX(), this.getEyeY(), this.getZ());

            Vec3d vel = new Vec3d(velocityX, velocityY, velocityZ);
            if (target.rot != null) {
                pos = pos.rotateX(target.rot.getX());
                pos = pos.rotateY(target.rot.getY());
                pos = pos.rotateZ(target.rot.getZ());
            }
            if (target.pos != null)
                pos = pos.add(new Vec3d(target.pos));

            if (target.scale != null)
                vel = vel.multiply(new Vec3d(target.scale));

            pos = pos.add(this.getX(), this.getEyeY(), this.getZ());

            world.addParticle(parameters,pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
        } else {
            world.addParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
        }
    }
}