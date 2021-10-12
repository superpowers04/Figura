package net.blancworks.figura.utils;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Quaternion;

public class MathUtils {

    //yoinked from wikipedia
    public static Vector3f quaternionToEulerXYZ(Quaternion q) {
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

        return new Vector3f((float) Math.toDegrees(roll), (float) Math.toDegrees(pitch), (float) Math.toDegrees(yaw));
    }
}
