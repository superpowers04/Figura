package net.blancworks.figura.models.animations;

import net.blancworks.figura.models.CustomModelPartGroup;
import net.blancworks.figura.utils.MathUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class Animation {
    //animation data
    public final String name;
    public float length;
    public LoopMode loopMode;

    public float startOffset;
    public float blendWeight;
    public float startDelay;
    public float loopDelay;

    public float blendTime = 1f;

    //keyframes
    public HashMap<CustomModelPartGroup, List<TreeMap<Float, KeyFrame>>> keyFrames = new HashMap<>();

    //animation status
    public float speed = 1f;
    public boolean inverted = false;
    public PlayState playState = PlayState.STOPPED;

    private float time = 0f;
    private float newTime = 0f;
    private float blendEndTime = -1f;

    public Animation(String name, float length, LoopMode loopMode, float startOffset, float blendWeight, float startDelay, float loopDelay) {
        this.name = name;
        this.length = length;
        this.loopMode = loopMode;

        this.startOffset = startOffset;
        this.blendWeight = blendWeight;
        this.startDelay = startDelay;
        this.loopDelay = loopDelay;
    }

    public enum LoopMode {
        HOLD,
        LOOP,
        ONCE
    }

    public enum PlayState {
        STOPPED, //not playing
        PLAYING, //is playing
        PAUSED,  //is on hold
        ENDED,   //hold on last frame
        STOPPING //is blending backing to default
    }

    public void render() {
        //if running, store current time
        if (this.playState != PlayState.PAUSED && this.playState != PlayState.ENDED)
            newTime = Util.getMeasuringTimeMs();

        //get keyframe time from current time
        float kfTime = ((newTime - time) / 1000f) * speed;

        //delay
        kfTime -= startDelay;
        if (kfTime < 0f) return;

        //offset
        kfTime += startOffset;

        //process loop
        if (playState != PlayState.STOPPING && kfTime >= this.length) {
            switch (loopMode) {
                case HOLD -> playState = PlayState.ENDED;
                case ONCE -> playState = PlayState.STOPPING;
                case LOOP -> {
                    //loop delay
                    if (kfTime >= this.length + loopDelay) {
                        stop();
                        play();
                    }
                }
            }
        }

        //blend
        if (playState == PlayState.STOPPING) {
            if (blendEndTime == -1f)
                blendEndTime = (kfTime + blendTime) * speed;

            if (kfTime >= blendEndTime) {
                stop();
                return;
            }
        }

        //keyframe interpolation
        float finalTime = inverted ? length - kfTime : kfTime;
        keyFrames.forEach((group, data) -> {
            //get interpolated data
            Vec3f pos = processKeyFrame(data.get(0), finalTime);
            Vec3f rot = processKeyFrame(data.get(1), finalTime);
            Vec3f scale = processKeyFrame(data.get(2), finalTime);

            //blending
            if (playState == PlayState.STOPPING) {
                float blendStartTime = blendEndTime - (blendTime * speed);
                float delta = (finalTime - blendStartTime) / (blendEndTime - blendStartTime);

                if (pos != null) pos = MathUtils.lerpVec3f(pos, Vec3f.ZERO, delta);
                if (rot != null) rot = MathUtils.lerpVec3f(rot, Vec3f.ZERO, delta);
                if (scale != null) scale = MathUtils.lerpVec3f(scale, MathUtils.Vec3f_ONE, delta);
            }

            //apply data, if not null
            if (pos != null) group.animPos.add(pos);
            if (rot != null) group.animRot.add(rot);
            if (scale != null) group.animScale.multiplyComponentwise(scale.getX(), scale.getY(), scale.getZ());
        });
    }

    public void play() {
        if (this.playState == PlayState.ENDED || this.playState == PlayState.STOPPING)
            stop();

        long offset = Util.getMeasuringTimeMs();
        if (this.playState == PlayState.PAUSED)
            offset -= newTime - time;

        this.playState = PlayState.PLAYING;
        this.time = offset;
    }

    public void stop() {
        this.blendEndTime = -1f;
        this.time = 0f;
        this.playState = PlayState.STOPPED;
    }

    public void clearAnimData() {
        for (CustomModelPartGroup group : keyFrames.keySet()) {
            group.animRot = Vec3f.ZERO.copy();
            group.animPos = Vec3f.ZERO.copy();
            group.animScale = MathUtils.Vec3f_ONE.copy();
        }
    }

    public Vec3f processKeyFrame(TreeMap<Float, KeyFrame> map, float time) {
        try {
            //get keyframes
            Map.Entry<Float, KeyFrame> floor = map.floorEntry(time);
            Map.Entry<Float, KeyFrame> ceil = map.ceilingEntry(time);

            if (floor == null && ceil == null)
                return null;

            //set current and next keyframes
            KeyFrame curr = Objects.requireNonNullElse(floor, ceil).getValue();
            KeyFrame next = Objects.requireNonNullElse(ceil, floor).getValue();

            //get keyframe data
            Vec3f start = curr.data.copy();
            Vec3f end = next.data.copy();

            //set blend weight
            start.scale(blendWeight);
            end.scale(blendWeight);

            //get delta
            float delta;
            if (next.time == curr.time) delta = 1f;
            else delta = (time - curr.time) / (next.time - curr.time);

            //return interpolated keyframe
            switch (curr.interpolation) {
                case LINEAR -> {
                    return MathUtils.lerpVec3f(start, end, delta);
                }
                case CATMULLROM -> {
                    //get "before" and "after" keyframes
                    Map.Entry<Float, KeyFrame> beforeFloor = map.lowerEntry(curr.time);
                    Map.Entry<Float, KeyFrame> afterCeil = map.higherEntry(next.time);

                    Vec3f bef = beforeFloor != null ? beforeFloor.getValue().data.copy() : curr.data.copy();
                    Vec3f aft = afterCeil != null ? afterCeil.getValue().data.copy() : next.data.copy();

                    //set blend weight
                    bef.scale(blendWeight);
                    aft.scale(blendWeight);

                    return MathUtils.catmullRomVec3f(bef, start, end, aft, delta);
                }
                default -> { //also STEP
                    return start;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Animation fromNbt(NbtCompound animTag) {
        String name = animTag.getString("nm");
        float length = animTag.getFloat("len");
        Animation.LoopMode loopMode = Animation.LoopMode.valueOf(animTag.getString("loop").toUpperCase());

        float startOffset = animTag.contains("time") ? animTag.getFloat("time") : 0f;
        float blendWeight = animTag.contains("bld") ? animTag.getFloat("bld") : 1f;
        float startDelay = animTag.contains("sdel") ? animTag.getFloat("sdel") : 0f;
        float loopDelay = animTag.contains("ldel") ? animTag.getFloat("ldel") : 0f;

        return new Animation(name, length, loopMode, startOffset, blendWeight, startDelay, loopDelay);
    }
}
