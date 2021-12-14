package net.blancworks.figura.models.animations;

import java.util.List;

public class Animation {
    public final float id;
    public final String name;
    public final List<KeyFrame> keyFrames;
    public final float length;
    public final float snapping;
    public final LoopMode loopMode;

    //todo maybe unnecessary? and why they are strings??
    public final String animationTimeUpdate;
    public final String blendWeight;
    public final String startDelay;
    public final String loopDelay;

    public Animation(float id, String name, List<KeyFrame> keyFrames, float length, float snapping, LoopMode loopMode, String animationTimeUpdate, String blendWeight, String startDelay, String loopDelay) {
        this.id = id;
        this.name = name;
        this.keyFrames = keyFrames;
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
}
