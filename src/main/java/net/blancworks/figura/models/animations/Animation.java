package net.blancworks.figura.models.animations;

import net.blancworks.figura.models.CustomModelPartGroup;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;

import java.util.ArrayList;
import java.util.HashMap;

public class Animation {
    //animation data
    public final String name;
    public final HashMap<CustomModelPartGroup, ArrayList<KeyFrame>> keyFrames = new HashMap<>();
    public final float length;
    public final LoopMode loopMode;

    //todo maybe unnecessary? and why they are strings??
    public final String animationTimeUpdate;
    public final String blendWeight;
    public final String startDelay;
    public final String loopDelay;

    //animation status
    public int tick = 0;
    public PlayState playState = PlayState.stopped;

    //animation smooth
    public HashMap<CustomModelPartGroup, KeyFrame> startPosition = new HashMap<>();
    public HashMap<CustomModelPartGroup, KeyFrame> startRotation = new HashMap<>();
    public HashMap<CustomModelPartGroup, KeyFrame> startScale = new HashMap<>();

    public HashMap<CustomModelPartGroup, KeyFrame> endPosition = new HashMap<>();
    public HashMap<CustomModelPartGroup, KeyFrame> endRotation = new HashMap<>();
    public HashMap<CustomModelPartGroup, KeyFrame> endScale = new HashMap<>();

    public Animation(String name, float length, LoopMode loopMode, String animationTimeUpdate, String blendWeight, String startDelay, String loopDelay) {
        this.name = name;
        this.length = length;
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
        float time = tick / 20f;

        //animation end
        if (time > this.length) {
            switch (loopMode) {
                case hold -> playState = PlayState.stopped;
                case once -> stop();
                case loop -> {
                    stop();
                    playState = PlayState.playing;
                }
            }
            return;
        }

        //get current keyframes
        keyFrames.forEach((part, keyFrames) -> {
            KeyFrame startPosition = null;
            KeyFrame startRotation = null;
            KeyFrame startScale = null;

            KeyFrame endPosition = null;
            KeyFrame endRotation = null;
            KeyFrame endScale = null;

            for (KeyFrame keyFrame : keyFrames) {
                float theTime = keyFrame.time;
                switch (keyFrame.type) {
                    case position -> {
                        if (theTime <= time && (startPosition == null || theTime > startPosition.time))
                            startPosition = keyFrame;

                        if (theTime > time && (endPosition == null || theTime <= endPosition.time))
                            endPosition = keyFrame;
                    }
                    case rotation -> {
                        if (theTime <= time && (startRotation == null || theTime > startRotation.time))
                            startRotation = keyFrame;

                        if (theTime > time && (endRotation == null || theTime <= endRotation.time))
                            endRotation = keyFrame;
                    }
                    case scale -> {
                        if (theTime <= time && (startScale == null || theTime > startScale.time))
                            startScale = keyFrame;

                        if (theTime > time && (endScale == null || theTime <= endScale.time))
                            endScale = keyFrame;
                    }
                }
            }

            if (startPosition != null)
                this.startPosition.put(part, startPosition);
            if (startRotation != null)
                this.startRotation.put(part, startRotation);
            if (startScale != null)
                this.startScale.put(part, startScale);

            if (endPosition != null)
                this.endPosition.put(part, endPosition);
            if (endRotation != null)
                this.endRotation.put(part, endRotation);
            if (endScale != null)
                this.endScale.put(part, endScale);
        });

        tick++;
    }

    public void render(float delta) {
        float timeNow = (tick + delta) / 20f;

        for (CustomModelPartGroup part : keyFrames.keySet()) {
            KeyFrame sPos = startPosition.get(part);
            KeyFrame sRot = startRotation.get(part);
            KeyFrame sScale = startScale.get(part);

            KeyFrame ePos = endPosition.get(part);
            KeyFrame eRot = endRotation.get(part);
            KeyFrame eScale = endScale.get(part);

            if (sPos != null && ePos != null) {
                if (sPos == ePos) part.animPos.add(sPos.data);
                else {
                    float realDelta = (timeNow - sPos.time) / (ePos.time - sPos.time);
                    part.animPos.add(lerpVec3f(sPos.data, ePos.data, realDelta));
                }
            }
            if (sRot != null && eRot != null) {
                if (sRot == eRot) part.animRot.add(sRot.data);
                else {
                    float realDelta = (timeNow - sRot.time) / (eRot.time - sRot.time);
                    part.animRot.add(lerpVec3f(sRot.data, eRot.data, realDelta));
                }
            }
            if (sScale != null && eScale != null) {
                if (sScale == eScale) part.animScale.add(sScale.data);
                else {
                    float realDelta = (timeNow - sScale.time) / (eScale.time - sScale.time);
                    part.animScale.add(lerpVec3f(sScale.data, eScale.data, realDelta));
                }
            }
        }
    }

    public void stop() {
        this.playState = PlayState.stopped;
        this.tick = 0;
    }

    public static Animation fromNbt(NbtCompound animTag) {
        String name = animTag.getString("nm");
        float length = animTag.getFloat("len");
        Animation.LoopMode loopMode = Animation.LoopMode.valueOf(animTag.getString("loop"));

        //TODO - i think you got it
        String animationTimeUpdate = animTag.contains("time") ? animTag.getString("time") : "";
        String blendWeight = animTag.contains("bld") ? animTag.getString("bld") : "";
        String startDelay = animTag.contains("sdel") ? animTag.getString("sdel") : "";
        String loopDelay = animTag.contains("ldel") ? animTag.getString("ldel") : "";

        return new Animation(name, length, loopMode, animationTimeUpdate, blendWeight, startDelay, loopDelay);
    }

    public static Vec3f lerpVec3f(Vec3f start, Vec3f end, float delta) {
        float x = MathHelper.lerp(delta, start.getX(), end.getX());
        float y = MathHelper.lerp(delta, start.getY(), end.getY());
        float z = MathHelper.lerp(delta, start.getZ(), end.getZ());

        return new Vec3f(x, y, z);
    }

    //TODO
    //TODO
    //TODO
    //TODO
    //TODO
    public static Vec3f catmullRomVec3f(Vec3f start, Vec3f end, float delta) {
        return lerpVec3f(start, end, delta);
    }
}
