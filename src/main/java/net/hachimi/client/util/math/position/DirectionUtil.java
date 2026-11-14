package net.hachimi.client.util.math.position;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class DirectionUtil {
   public static Vec3d getDirectionOffsetPos(BlockPos pos, Direction direction) {
      Vec3d pos1 = pos.toCenterPos();
      Vec3d var10000;
      switch (direction) {
         case UP -> var10000 = pos1.add((double)0.0F, (double)0.5F, (double)0.0F);
         case DOWN -> var10000 = pos1.add((double)0.0F, (double)-0.5F, (double)0.0F);
         case NORTH -> var10000 = pos1.add((double)0.0F, (double)0.0F, (double)-0.5F);
         case SOUTH -> var10000 = pos1.add((double)0.0F, (double)0.0F, (double)0.5F);
         case WEST -> var10000 = pos1.add((double)-0.5F, (double)0.0F, (double)0.0F);
         case EAST -> var10000 = pos1.add((double)0.5F, (double)0.0F, (double)0.0F);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }
}
