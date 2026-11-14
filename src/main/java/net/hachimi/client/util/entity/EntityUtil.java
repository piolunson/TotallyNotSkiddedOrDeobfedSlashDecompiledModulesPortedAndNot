package net.hachimi.client.util.entity;

import net.hachimi.client.util.Globals;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.passive.BeeEntity;

public class EntityUtil implements Globals {
   public static BlockPos getRoundedBlockPos(Entity entity) {
      return new BlockPos(entity.getBlockX(), (int)Math.round(entity.getY()), entity.getBlockZ());
   }

   public static float getHealth(Entity entity) {
      if (entity instanceof LivingEntity e) {
         return e.getHealth() + e.getAbsorptionAmount();
      } else {
         return 0.0F;
      }
   }

   public static boolean isMonster(Entity e) {
      return e instanceof Monster && !isNeutralInternal(e);
   }

   private static boolean isNeutralInternal(Entity e) {
      boolean var10000;
      label41: {
         if (e instanceof EndermanEntity enderman) {
            if (!enderman.isAttacking()) {
               break label41;
            }
         }

         if (e instanceof ZombifiedPiglinEntity piglin) {
            if (!piglin.isAttacking()) {
               break label41;
            }
         }

         if (e instanceof WolfEntity wolf) {
            if (!wolf.isAttacking()) {
               break label41;
            }
         }

         if (e instanceof IronGolemEntity ironGolem) {
            if (!ironGolem.isAttacking()) {
               break label41;
            }
         }

         if (e instanceof BeeEntity bee) {
            if (!bee.isAttacking()) {
               break label41;
            }
         }

         var10000 = false;
         return var10000;
      }

      var10000 = true;
      return var10000;
   }

   public static boolean isNeutral(Entity e) {
      return e instanceof EndermanEntity || e instanceof ZombifiedPiglinEntity || e instanceof WolfEntity || e instanceof IronGolemEntity;
   }

   public static boolean isPassive(Entity e) {
      return e instanceof PassiveEntity || e instanceof AmbientEntity || e instanceof SquidEntity;
   }

   public static boolean isVehicle(Entity e) {
      return e instanceof BoatEntity || e instanceof MinecartEntity || e instanceof FurnaceMinecartEntity || e instanceof ChestMinecartEntity;
   }
}
