package net.blancworks.figura.utils;

import net.blancworks.figura.access.GameRendererAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.*;

public class MathUtils {

    //yoinked from wikipedia
    public static Vec3f quaternionToEulerXYZ(Quaternion q) {
        double pitch, yaw, roll;

        float x = q.getX();
        float y = q.getY();
        float z = q.getZ();
        float w = q.getW();

        // roll (x-axis rotation)
        double sinr_cosp = 2 * (w * x + y * z);
        double cosr_cosp = 1 - 2 * (x * x + y * y);
        roll = Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = 2 * (w * y - z * x);
        if (Math.abs(sinp) >= 1)
            pitch = Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            pitch = Math.asin(sinp);

        // yaw (z-axis rotation)
        double siny_cosp = 2 * (w * z + x * y);
        double cosy_cosp = 1 - 2 * (y * y + z * z);
        yaw = Math.atan2(siny_cosp, cosy_cosp);

        return new Vec3f((float) Math.toDegrees(roll), (float) Math.toDegrees(pitch), (float) Math.toDegrees(yaw));
    }

    /**
     * Calculates the screen space from a point in the world, with W being the distance from the camera to that point.
     * @param worldSpace position in the world
     * @return screenSpaceAndDistance
     */
    public static Vector4f worldToScreenSpace(Vec3f worldSpace) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Matrix3f transformMatrix = new Matrix3f(camera.getRotation());
        transformMatrix.invert();
        Vec3f camSpace = new Vec3f(worldSpace.getX(), worldSpace.getY(), worldSpace.getZ());
        camSpace.subtract(new Vec3f(camera.getPos()));
        camSpace.transform(transformMatrix);
        double dist = Math.sqrt(camSpace.dot(camSpace));

        Vector4f projectiveCamSpace = new Vector4f(camSpace);
        Matrix4f projMat = client.gameRenderer.getBasicProjectionMatrix(((GameRendererAccess) client.gameRenderer).figura$getFov(camera, true));
        projectiveCamSpace.transform(projMat);
        float x = projectiveCamSpace.getX();
        float y = projectiveCamSpace.getY();
        float z = projectiveCamSpace.getZ();
        float w = projectiveCamSpace.getW();
        return new Vector4f(x / w, y / w, z / w, (float) dist);
    }

    public static Vec3f lerpVec3f(Vec3f start, Vec3f end, float delta) {
        Vec3f ret = start.copy();
        ret.lerp(end, delta);
        return ret;
    }

    public static Vec3f catmullRomVec3f(Vec3f before, Vec3f start, Vec3f end, Vec3f after, float delta) {
        Vec3d a = new Vec3d(before);
        Vec3d b = new Vec3d(start);
        Vec3d c = new Vec3d(end);
        Vec3d d = new Vec3d(after);

        return new Vec3f(MathHelper.method_34946(a, b, c, d, delta));
    }
}
