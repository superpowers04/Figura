package net.blancworks.figura.models.animations;

import net.blancworks.figura.models.CustomModelPartGroup;
import net.blancworks.figura.utils.MathUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
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

    public boolean override;

    public float blendTime = 1f / 20f; //1 tick
    public boolean replace = false;
    public int priority = 0;

    //keyframes
    public HashMap<CustomModelPartGroup, List<TreeMap<Float, KeyFrame>>> keyFrames = new HashMap<>();

    //animation status
    public float speed = 1f;
    public boolean inverted = false;

    public PlayState playState = PlayState.STOPPED;
    private PlayState lastState = PlayState.STOPPED;
    private boolean wasStarting = false;

    private float time = 0f;
    private float newTime = 0f;
    private float lastTime = 0f;

    public Animation(String name, float length, LoopMode loopMode, float startOffset, float blendWeight, float startDelay, float loopDelay, boolean override) {
        this.name = name;
        this.length = length;
        this.loopMode = loopMode;

        this.startOffset = startOffset;
        this.blendWeight = blendWeight;
        this.startDelay = startDelay;
        this.loopDelay = loopDelay;
        this.override = override;
    }

    public enum LoopMode {
        HOLD,
        LOOP,
        ONCE
    }

    public enum PlayState {
        STOPPED,  //not playing
        PLAYING,  //is playing
        PAUSED,   //don't animate but render
        ENDED,    //hold on last frame
        STOPPING, //blend back to default
        STARTING  //blend from previous anim
    }

    //render cuntions
    public int render(int renderCount, int renderLimit) {
        //start/end blend
        if (this.playState == PlayState.STARTING || this.playState == PlayState.STOPPING || (this.playState == PlayState.PAUSED && (this.lastState == PlayState.STARTING || this.lastState == PlayState.STOPPING))) {
            return renderBlend(this.playState == PlayState.STOPPING || (this.playState == PlayState.PAUSED && this.lastState == PlayState.STOPPING), renderCount, renderLimit);
        }

        //if running, store current time
        if (this.playState != PlayState.PAUSED && this.playState != PlayState.ENDED)
            newTime = Util.getMeasuringTimeMs();

        //get keyframe time from current time
        float kfTime = ((newTime - time) / 1000f) * speed;

        //offset
        kfTime += startOffset;

        //process loop
        if (kfTime >= this.length) {
            switch (loopMode) {
                case HOLD -> playState = PlayState.ENDED;
                case ONCE -> stop();
                case LOOP -> {
                    //loop delay
                    if (kfTime >= this.length + loopDelay) {
                        cease();
                        start();
                    }
                }
            }
        }

        //keyframe interpolation
        lastTime = inverted ? length - kfTime : kfTime;
        for (Map.Entry<CustomModelPartGroup, List<TreeMap<Float, KeyFrame>>> entry : keyFrames.entrySet()) {
            CustomModelPartGroup group = entry.getKey();
            List<TreeMap<Float, KeyFrame>> data = entry.getValue();

            //priority check
            if (this.priority < group.lastPriority)
                continue;

            boolean replace = this.priority > group.lastPriority;

            //get interpolated data
            Vec3f pos = processKeyFrame(data.get(0), lastTime);
            Vec3f rot = processKeyFrame(data.get(1), lastTime);
            Vec3f scale = processKeyFrame(data.get(2), lastTime);

            //apply data, if not null
            if (pos != null) {
                if (override) {
                    if (replace) group.animPosOverride = pos;
                    else group.animPosOverride.add(pos);
                } else {
                    if (replace) group.animPos = pos;
                    else group.animPos.add(pos);
                }
                renderCount++;
            }
            if (rot != null) {
                if (replace) group.animRot = rot;
                else group.animRot.add(rot);
                renderCount++;
            }
            if (scale != null) {
                if (replace) group.animScale = scale;
                else group.animScale.multiplyComponentwise(scale.getX(), scale.getY(), scale.getZ());
                renderCount++;
            }

            //group vars
            group.wasAnimated = pos != null || rot != null || scale != null;
            group.replaced = group.replaced || this.replace;
            group.lastPriority = this.priority;

            if (renderCount > renderLimit)
                break;
        }

        return renderCount;
    }

    private int renderBlend(boolean ending, int renderCount, int renderLimit) {
        if (this.playState != PlayState.PAUSED)
            newTime = Util.getMeasuringTimeMs();

        //get keyframe time from current time
        float kfTime = ((newTime - time) / 1000f) * speed;

        //delay
        if (!ending) {
            kfTime -= startDelay;
            if (kfTime < 0f) return renderCount;
        }

        //end
        if (kfTime >= this.blendTime) {
            if (ending) cease();
            else start();
        }

        //process keyframes
        if (!ending) lastTime = kfTime;
        for (Map.Entry<CustomModelPartGroup, List<TreeMap<Float, KeyFrame>>> entry : keyFrames.entrySet()) {
            CustomModelPartGroup group = entry.getKey();
            List<TreeMap<Float, KeyFrame>> data = entry.getValue();

            //priority check
            if (this.priority < group.lastPriority)
                continue;

            boolean replace = this.priority > group.lastPriority;

            //get interpolated data
            Vec3f pos = ending && !wasStarting ? processKeyFrame(data.get(0), lastTime) : getKeyFrameData(data.get(0), startOffset, inverted);
            Vec3f rot = ending && !wasStarting ? processKeyFrame(data.get(1), lastTime) : getKeyFrameData(data.get(1), startOffset, inverted);
            Vec3f scale = ending && !wasStarting ? processKeyFrame(data.get(2), lastTime) : getKeyFrameData(data.get(2), startOffset, inverted);

            //apply data, if not null
            float delta = MathHelper.clamp(kfTime / blendTime, 0f, 1f);
            if (pos != null) {
                if (ending) {
                    if (wasStarting)
                        pos = MathUtils.lerpVec3f(Vec3f.ZERO, pos, lastTime / blendTime);
                    pos = MathUtils.lerpVec3f(pos, Vec3f.ZERO, delta);
                } else {
                    pos = MathUtils.lerpVec3f(Vec3f.ZERO, pos, delta);
                }

                if (override) {
                    if (replace) group.animPosOverride = pos;
                    else group.animPosOverride.add(pos);
                } else {
                    if (replace) group.animPos = pos;
                    else group.animPos.add(pos);
                }
                renderCount++;
            }
            if (rot != null) {
                if (ending) {
                    if (wasStarting)
                        rot = MathUtils.lerpVec3f(Vec3f.ZERO, rot, lastTime / blendTime);
                    rot = MathUtils.lerpVec3f(rot, Vec3f.ZERO, delta);
                } else {
                    rot = MathUtils.lerpVec3f(Vec3f.ZERO, rot, delta);
                }

                if (replace) group.animRot = rot;
                else group.animRot.add(rot);
                renderCount++;
            }
            if (scale != null) {
                if (ending) {
                    if (wasStarting)
                        scale = MathUtils.lerpVec3f(MathUtils.Vec3f_ONE, scale, lastTime / blendTime);
                    scale = MathUtils.lerpVec3f(scale, MathUtils.Vec3f_ONE, delta);
                } else {
                    scale = MathUtils.lerpVec3f(MathUtils.Vec3f_ONE, scale, delta);
                }

                if (replace) group.animScale = scale;
                else group.animScale.multiplyComponentwise(scale.getX(), scale.getY(), scale.getZ());
                renderCount++;
            }

            //group vars
            group.wasAnimated = pos != null || rot != null || scale != null;
            group.replaced = group.replaced || this.replace;
            group.lastPriority = this.priority;

            if (renderCount > renderLimit)
                break;
        }

        return renderCount;
    }

    //state functions
    public void play() {
        if (this.playState == PlayState.ENDED || this.playState == PlayState.STOPPING)
            cease();

        long offset = Util.getMeasuringTimeMs();
        if (this.playState == PlayState.PAUSED) {
            offset -= newTime - time;
            this.playState = this.lastState;
        }

        if (this.playState == PlayState.STOPPED)
            this.playState = PlayState.STARTING;

        this.time = offset;
    }

    public void start() {
        this.time = Util.getMeasuringTimeMs();
        this.playState = PlayState.PLAYING;
    }

    public void stop() {
        if (this.playState != PlayState.STOPPED && this.playState != PlayState.STOPPING) {
            this.time = Util.getMeasuringTimeMs();
            this.wasStarting = this.playState == PlayState.STARTING;
            this.playState = PlayState.STOPPING;
        }
    }

    public void cease() {
        this.time = 0f;
        this.playState = PlayState.STOPPED;
    }

    public void pause() {
        if (this.playState != PlayState.ENDED && this.playState != PlayState.PAUSED && this.playState != PlayState.STOPPED) {
            this.lastState = this.playState;
            this.playState = PlayState.PAUSED;
        }
    }

    public boolean isPlaying() {
        return playState == PlayState.PLAYING || playState == PlayState.STARTING || playState == PlayState.STOPPING;
    }

    //keyframe functions
    public void clearAnimData() {
        for (CustomModelPartGroup group : keyFrames.keySet()) {
            if (group.wasAnimated) {
                group.animRot = Vec3f.ZERO.copy();
                group.animPos = Vec3f.ZERO.copy();
                group.animPosOverride = Vec3f.ZERO.copy();
                group.animScale = MathUtils.Vec3f_ONE.copy();
                group.wasAnimated = false;
                group.replaced = false;
            }
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
            else delta = MathHelper.clamp((time - curr.time) / (next.time - curr.time), 0f, 1f);

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

    public Vec3f getKeyFrameData(TreeMap<Float, KeyFrame> map, float offset, boolean inverted) {
        //get keyframes
        Map.Entry<Float, KeyFrame> floor = map.floorEntry(offset);
        Map.Entry<Float, KeyFrame> ceil = map.ceilingEntry(offset);

        if (floor == null && ceil == null)
            return null;

        //set current and next keyframes
        KeyFrame curr = Objects.requireNonNullElse(floor, ceil).getValue();
        KeyFrame next = Objects.requireNonNullElse(ceil, floor).getValue();

        //weight
        Vec3f ret = inverted ? next.data.copy() : curr.data.copy();
        ret.scale(blendWeight);

        return ret;
    }

    //nbt parser
    public static Animation fromNbt(NbtCompound animTag) {
        String name = animTag.getString("nm");
        float length = animTag.getFloat("len");
        Animation.LoopMode loopMode = Animation.LoopMode.valueOf(animTag.getString("loop").toUpperCase());

        float startOffset = animTag.contains("time") ? animTag.getFloat("time") : 0f;
        float blendWeight = animTag.contains("bld") ? animTag.getFloat("bld") : 1f;
        float startDelay = animTag.contains("sdel") ? animTag.getFloat("sdel") : 0f;
        float loopDelay = animTag.contains("ldel") ? animTag.getFloat("ldel") : 0f;

        boolean override = animTag.contains("ovr") && animTag.getBoolean("ovr");

        return new Animation(name, length, loopMode, startOffset, blendWeight, startDelay, loopDelay, override);
    }
}
