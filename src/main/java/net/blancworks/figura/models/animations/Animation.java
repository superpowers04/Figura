package net.blancworks.figura.models.animations;

import net.blancworks.figura.models.CustomModelPartGroup;
import net.blancworks.figura.models.animations.KeyFrame.AnimationType;
import net.blancworks.figura.models.animations.KeyFrame.Interpolation;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3f;

import java.util.ArrayList;
import java.util.HashMap;

public class Animation {
    //animation data
    public final String name;
    public float length;
    public LoopMode loopMode;

    //todo maybe unnecessary? and why they are strings??
    public final String animationTimeUpdate;
    public final String blendWeight;
    public final String startDelay;
    public final String loopDelay;

    //keyframes
    public HashMap<CustomModelPartGroup, KeyFrame> currentKeyFrame = new HashMap<>();

    //animation status
    public float tick = 0f;
    public float speed = 1f;
    public PlayState playState = PlayState.STOPPED;

    public static final float STEP = 1 / 20f;

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
        HOLD,
        LOOP,
        ONCE
    }

    public enum PlayState {
        STOPPED,
        PLAYING,
        PAUSED,
        ENDED
    }

    public void tick() {
        float time = tick / 20f;

        //animation end
        if (time > this.length) {
            switch (loopMode) {
                case HOLD -> {
                    playState = PlayState.ENDED;
                    return;
                }
                case ONCE -> {
                    stop();
                    return;
                }
                case LOOP -> {
                    stop();
                    time = 0f;
                    playState = PlayState.PLAYING;
                }
            }
        } else {
            tick += speed;
        }

        float finalTime = time;
        currentKeyFrame.forEach((group, keyFrame) -> {
            if (finalTime >= keyFrame.nextKeyFrame.time && keyFrame.time <= keyFrame.nextKeyFrame.time)
                currentKeyFrame.put(group, keyFrame.nextKeyFrame);
        });
    }

    public void render(float delta) {
        float time = (tick - 1 + delta) / 20f;

        currentKeyFrame.forEach((group, keyFrame) -> {
            float timeNow = time - keyFrame.time;

            if (keyFrame.pos != null) {
                KeyFrame next = keyFrame.getNext(AnimationType.POSITION);

                if (next.time < keyFrame.time || next == keyFrame) {
                    group.animPos.add(keyFrame.pos.offset());
                } else {
                    float realDelta = timeNow / (next.time - keyFrame.time);

                    Vec3f pos;
                    if (keyFrame.pos.lerpMode() == Interpolation.LINEAR)
                        pos = lerpVec3f(keyFrame.pos.offset(), next.pos.offset(), realDelta);
                    else
                        pos = catmullRomVec3f(
                                keyFrame.getPrevious(AnimationType.POSITION).pos.offset(),
                                keyFrame.pos.offset(), next.pos.offset(),
                                next.getNext(AnimationType.POSITION).pos.offset(),
                                realDelta
                        );
                    group.animPos.add(pos);
                }
            }

            if (keyFrame.rot != null) {
                KeyFrame next = keyFrame.getNext(AnimationType.ROTATION);

                if (next.time < keyFrame.time || next == keyFrame) {
                    group.animRot.add(keyFrame.rot.offset());
                } else {
                    float realDelta = timeNow / (next.time - keyFrame.time);

                    Vec3f rot;
                    if (keyFrame.rot.lerpMode() == Interpolation.LINEAR)
                        rot = lerpVec3f(keyFrame.rot.offset(), next.rot.offset(), realDelta);
                    else
                        rot = catmullRomVec3f(
                                keyFrame.getPrevious(AnimationType.ROTATION).rot.offset(),
                                keyFrame.rot.offset(), next.rot.offset(),
                                next.getNext(AnimationType.ROTATION).rot.offset(),
                                realDelta
                        );
                    group.animRot.add(rot);
                }
            }

            if (keyFrame.scale != null) {
                KeyFrame next = keyFrame.getNext(AnimationType.SCALE);

                if (next.time < keyFrame.time || next == keyFrame) {
                    group.animScale.add(keyFrame.scale.offset());
                } else {
                    float realDelta = timeNow / (next.time - keyFrame.time);

                    Vec3f scale;
                    if (keyFrame.scale.lerpMode() == Interpolation.LINEAR)
                        scale = lerpVec3f(keyFrame.scale.offset(), next.scale.offset(), realDelta);
                    else
                        scale = catmullRomVec3f(
                                keyFrame.getPrevious(AnimationType.SCALE).scale.offset(),
                                keyFrame.scale.offset(), next.scale.offset(),
                                next.getNext(AnimationType.SCALE).scale.offset(),
                                realDelta
                        );
                    group.animScale.add(scale);
                }
            }
        });
    }

    public void play() {
        if (this.playState == PlayState.ENDED)
            stop();

        this.playState = PlayState.PLAYING;
    }

    public void stop() {
        this.tick = 0f;
        this.playState = PlayState.STOPPED;
        this.currentKeyFrame.forEach((group, keyFrame) -> currentKeyFrame.put(group, keyFrame.getFirst()));
    }

    public void clearAnimData() {
        currentKeyFrame.forEach((group, keyFrame) -> {
            group.animRot = Vec3f.ZERO.copy();
            group.animPos = Vec3f.ZERO.copy();
            group.animScale = new Vec3f(1f, 1f, 1f);
        });
    }

    public void addKeyFrames(CustomModelPartGroup group, ArrayList<KeyFrame> keyFrames) {
        //start
        KeyFrame head = keyFrames.get(0);

        //iterate body
        for (int i = 1; i < keyFrames.size(); i++) {
            KeyFrame now = keyFrames.get(i);
            now.previousKeyFrame = head;
            head.nextKeyFrame = now;
            head = now;
        }

        //finish tail
        KeyFrame trueHead = keyFrames.get(0);

        trueHead.previousKeyFrame = head;
        head.nextKeyFrame = trueHead;

        //add to head list
        this.currentKeyFrame.put(group, trueHead);
    }

    public static Animation fromNbt(NbtCompound animTag) {
        String name = animTag.getString("nm");
        float length = animTag.getFloat("len");
        Animation.LoopMode loopMode = Animation.LoopMode.valueOf(animTag.getString("loop").toUpperCase());

        if (loopMode == LoopMode.LOOP)
            length -= STEP; //remove last frame on loop

        //TODO - i think you got it
        String animationTimeUpdate = animTag.contains("time") ? animTag.getString("time") : "";
        String blendWeight = animTag.contains("bld") ? animTag.getString("bld") : "";
        String startDelay = animTag.contains("sdel") ? animTag.getString("sdel") : "";
        String loopDelay = animTag.contains("ldel") ? animTag.getString("ldel") : "";

        return new Animation(name, length, loopMode, animationTimeUpdate, blendWeight, startDelay, loopDelay);
    }

    public static Vec3f lerpVec3f(Vec3f start, Vec3f end, float delta) {
        Vec3f ret = start.copy();
        ret.lerp(end, delta);
        return ret;
    }

    //TODO
    public static Vec3f catmullRomVec3f(Vec3f startPrev, Vec3f start, Vec3f end, Vec3f endNext, float delta) {
        return lerpVec3f(start, end, delta);
    }
}
