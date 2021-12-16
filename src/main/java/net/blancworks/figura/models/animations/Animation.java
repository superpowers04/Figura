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
    public final float length;
    public final LoopMode loopMode;

    //todo maybe unnecessary? and why they are strings??
    public final String animationTimeUpdate;
    public final String blendWeight;
    public final String startDelay;
    public final String loopDelay;

    //keyframes
    public HashMap<CustomModelPartGroup, KeyFrame> currentKeyFrame = new HashMap<>();

    //animation status
    public int tick = 0;
    public PlayState playState = PlayState.stopped;

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

        currentKeyFrame.forEach((group, keyFrame) -> {
            if (time > keyFrame.nextKeyFrame.time) {
                currentKeyFrame.put(group, keyFrame.nextKeyFrame);
                System.out.println(time);
            }
        });

        tick++;
    }

    public void render(float delta) {
        float time = (tick - 1 + delta) / 20f;

        currentKeyFrame.forEach((group, keyFrame) -> {
            float timeNow = time - keyFrame.time;

            if (keyFrame.pos != null) {
                KeyFrame next = keyFrame.getNext(KeyFrame.DataType.position);
                float realDelta = timeNow / (next.time - keyFrame.time);

                group.animPos.add(lerpVec3f(keyFrame.pos.offset(), next.pos.offset(), realDelta));
                //System.out.println(tick + " ||| " + timeNow + " ||| " + keyFrame.time + " ||| " + next.time + " ||| " + realDelta);
            }

            if (keyFrame.rot != null) {
                KeyFrame next = keyFrame.getNext(KeyFrame.DataType.rotation);
                float realDelta = timeNow / (next.time - keyFrame.time);

                group.animRot.add(lerpVec3f(keyFrame.rot.offset(), next.rot.offset(), realDelta));
            }

            if (keyFrame.scale != null) {
                KeyFrame next = keyFrame.getNext(KeyFrame.DataType.scale);
                float realDelta = timeNow / (next.time - keyFrame.time);

                group.animScale.add(lerpVec3f(keyFrame.scale.offset(), next.scale.offset(), realDelta));
            }
        });
    }

    public void stop() {
        this.tick = 0;
        this.playState = PlayState.stopped;
        this.currentKeyFrame.forEach((group, keyFrame) -> currentKeyFrame.put(group, keyFrame.head));
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
