package net.blancworks.figura.models.animations;

import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;

public class Animation {
    //animation data
    public final String name;
    public final ArrayList<KeyFrame> keyFrames = new ArrayList<>();
    public final float length;
    public final float snapping;
    public final LoopMode loopMode;

    //todo maybe unnecessary? and why they are strings??
    public final String animationTimeUpdate;
    public final String blendWeight;
    public final String startDelay;
    public final String loopDelay;

    //animation status
    public int tick = 0;
    public PlayState playState = PlayState.stopped;

    public Animation(String name, float length, float snapping, LoopMode loopMode, String animationTimeUpdate, String blendWeight, String startDelay, String loopDelay) {
        this.name = name;
        this.length = length;
        this.snapping = snapping;
        this.loopMode = loopMode;

        this.animationTimeUpdate = animationTimeUpdate;
        this.blendWeight = blendWeight;
        this.startDelay = startDelay;
        this.loopDelay = loopDelay;
    }

    public enum LoopMode {
        hold,
        loop,
        once
    }

    public enum PlayState {
        stopped,
        playing,
        paused
    }

    public void tick() {
        
    }

    public static Animation fromNbt(NbtCompound animTag) {
        String name = animTag.getString("nm");
        float length = animTag.getFloat("len");
        float snapping = animTag.getFloat("snp");
        Animation.LoopMode loopMode = Animation.LoopMode.valueOf(animTag.getString("loop"));

        //TODO - i think you got it
        String animationTimeUpdate = animTag.contains("time") ? animTag.getString("time") : "";
        String blendWeight = animTag.contains("bld") ? animTag.getString("bld") : "";
        String startDelay = animTag.contains("sdel") ? animTag.getString("sdel") : "";
        String loopDelay = animTag.contains("ldel") ? animTag.getString("ldel") : "";

        return new Animation(name, length, snapping, loopMode, animationTimeUpdate, blendWeight, startDelay, loopDelay);
    }
}
