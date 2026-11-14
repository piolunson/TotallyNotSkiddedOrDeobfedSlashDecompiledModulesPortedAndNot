package net.hachimi.client.util.player;

import net.hachimi.client.util.Globals;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class RotationUtil implements Globals {
   public static float[] getRotationsTo(Vec3d src, Vec3d dest) {
      float yaw = (float)(Math.toDegrees(Math.atan2(dest.subtract(src).z, dest.subtract(src).x)) - (double)90.0F);
      float pitch = (float)Math.toDegrees(-Math.atan2(dest.subtract(src).y, Math.hypot(dest.subtract(src).x, dest.subtract(src).z)));
      return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
   }

   public static float[] smooth(float[] target, float[] previous, float rotationSpeed) {
      float speed = (1.0F - MathHelper.clamp(rotationSpeed / 100.0F, 0.1F, 0.9F)) * 10.0F;
      float[] rotations = new float[2];
      rotations[0] = previous[0] + (float)(-getAngleDifference(previous[0], target[0]) / (double)speed);
      rotations[1] = previous[1] + -(previous[1] - target[1]) / speed;
      rotations[1] = MathHelper.clamp(rotations[1], -90.0F, 90.0F);
      return rotations;
   }

   public static double getAngleDifference(float client, float yaw) {
      return ((double)(client - yaw) % (double)360.0F + (double)540.0F) % (double)360.0F - (double)180.0F;
   }

   public static double getAnglePitchDifference(float client, float pitch) {
      return ((double)(client - pitch) % (double)180.0F + (double)270.0F) % (double)180.0F - (double)90.0F;
   }

   public static Vec3d getRotationVector(float pitch, float yaw) {
      float f = pitch * ((float)Math.PI / 180F);
      float g = -yaw * ((float)Math.PI / 180F);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
   }
}
