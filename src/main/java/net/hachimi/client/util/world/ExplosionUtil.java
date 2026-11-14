package net.hachimi.client.util.world;

import java.util.Set;
import java.util.function.BiFunction;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.player.EnchantmentUtil;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.ArmorItem.Type;
import org.apache.commons.lang3.mutable.MutableInt;

public class ExplosionUtil implements Globals {
   public static double getDamageTo(Entity entity, Vec3d explosion, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, false, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, boolean ignoreTerrain, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, 0, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, boolean ignoreTerrain, int extrapolationTicks, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, extrapolationTicks, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, IgnoreTerrain ignoreTerrain, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, 0, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, IgnoreTerrain ignoreTerrain, int extrapolationTicks, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, extrapolationTicks, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, IgnoreTerrain ignoreTerrain, float power, int extrapolationTicks, boolean assumeBestArmor) {
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      Vec3d vec3d2 = Vec3d.ZERO;
      if (extrapolationTicks != 0) {
         double ox = (x - entity.prevX) * (double)extrapolationTicks;
         double oy = (y - entity.prevY) * (double)extrapolationTicks * 0.3;
         double oz = (z - entity.prevZ) * (double)extrapolationTicks;
         x += ox;
         y += oy;
         z += oz;
         vec3d2 = new Vec3d(ox, oy, oz);
      }

      Vec3d vec3d = new Vec3d(x, y, z);
      double d = Math.sqrt(vec3d.squaredDistanceTo(explosion));
      double ab = (double)getExposure(explosion, entity.getBoundingBox().offset(vec3d2), ignoreTerrain);
      double w = d / (double)power;
      double ac = ((double)1.0F - w) * ab;
      double dmg = (double)((float)((int)((ac * ac + ac) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F)));
      dmg = getReduction(entity, mc.world.getDamageSources().explosion((Explosion)null), dmg, assumeBestArmor);
      return Math.max((double)0.0F, dmg);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, boolean ignoreTerrain, Set<BlockPos> ignoreBlocks, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, ignoreBlocks, 0, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, boolean ignoreTerrain, Set<BlockPos> ignoreBlocks, int extrapolationTicks, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, ignoreBlocks, extrapolationTicks, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, boolean ignoreTerrain, float power, Set<BlockPos> ignoreBlocks, int extrapolationTicks, boolean assumeBestArmor) {
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      Vec3d vec3d2 = Vec3d.ZERO;
      if (extrapolationTicks != 0) {
         double ox = (x - entity.prevX) * (double)extrapolationTicks;
         double oy = (y - entity.prevY) * (double)extrapolationTicks * 0.3;
         double oz = (z - entity.prevZ) * (double)extrapolationTicks;
         x += ox;
         y += oy;
         z += oz;
         vec3d2 = new Vec3d(ox, oy, oz);
      }

      Vec3d vec3d = new Vec3d(x, y, z);
      double d = Math.sqrt(vec3d.squaredDistanceTo(explosion));
      double ab = (double)getExposure(explosion, entity.getBoundingBox().offset(vec3d2), ignoreTerrain ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE, ignoreBlocks);
      double w = d / (double)power;
      double ac = ((double)1.0F - w) * ab;
      double dmg = (double)((float)((int)((ac * ac + ac) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F)));
      dmg = getReduction(entity, mc.world.getDamageSources().explosion((Explosion)null), dmg, assumeBestArmor);
      return Math.max((double)0.0F, dmg);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks, boolean assumeBestArmor) {
      return getDamageTo(entity, explosion, ignoreTerrain, 12.0F, ignoreBlocks, 0, assumeBestArmor);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, IgnoreTerrain ignoreTerrain, float power, Set<BlockPos> ignoreBlocks, int extrapolationTicks, boolean assumeBestArmor) {
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      Vec3d vec3d2 = Vec3d.ZERO;
      if (extrapolationTicks != 0) {
         double ox = (x - entity.prevX) * (double)extrapolationTicks;
         double oy = (y - entity.prevY) * (double)extrapolationTicks * 0.3;
         double oz = (z - entity.prevZ) * (double)extrapolationTicks;
         x += ox;
         y += oy;
         z += oz;
         vec3d2 = new Vec3d(ox, oy, oz);
      }

      Vec3d vec3d = new Vec3d(x, y, z);
      double d = Math.sqrt(vec3d.squaredDistanceTo(explosion));
      double ab = (double)getExposure(explosion, entity.getBoundingBox().offset(vec3d2), ignoreTerrain, ignoreBlocks);
      double w = d / (double)power;
      double ac = ((double)1.0F - w) * ab;
      double dmg = (double)((float)((int)((ac * ac + ac) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F)));
      dmg = getReduction(entity, mc.world.getDamageSources().explosion((Explosion)null), dmg, assumeBestArmor);
      return Math.max((double)0.0F, dmg);
   }

   public static double getDamageTo(Entity entity, Vec3d explosion, boolean ignoreTerrain, float power, int extrapolationTicks, boolean assumeBestArmor) {
      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();
      Vec3d vec3d2 = Vec3d.ZERO;
      if (extrapolationTicks != 0) {
         double ox = (x - entity.prevX) * (double)extrapolationTicks;
         double oy = (y - entity.prevY) * (double)extrapolationTicks * 0.3;
         double oz = (z - entity.prevZ) * (double)extrapolationTicks;
         x += ox;
         y += oy;
         z += oz;
         vec3d2 = new Vec3d(ox, oy, oz);
      }

      Vec3d vec3d = new Vec3d(x, y, z);
      double d = Math.sqrt(vec3d.squaredDistanceTo(explosion));
      double ab = (double)getExposure(explosion, entity.getBoundingBox().offset(vec3d2), ignoreTerrain ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE);
      double w = d / (double)power;
      double ac = ((double)1.0F - w) * ab;
      double dmg = (double)((float)((int)((ac * ac + ac) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F)));
      dmg = getReduction(entity, mc.world.getDamageSources().explosion((Explosion)null), dmg, assumeBestArmor);
      return Math.max((double)0.0F, dmg);
   }

   public static double getDamageToPos(Vec3d pos, Entity entity, Vec3d explosion, boolean ignoreTerrain, boolean assumeBestArmor) {
      Box bb = entity.getBoundingBox();
      double dx = pos.getX() - bb.minX;
      double dy = pos.getY() - bb.minY;
      double dz = pos.getZ() - bb.minZ;
      Box box = bb.offset(dx, dy, dz);
      RaycastFactory raycastFactory = getRaycastFactory(ignoreTerrain ? ExplosionUtil.IgnoreTerrain.BLAST : ExplosionUtil.IgnoreTerrain.NONE);
      double ab = (double)getExposure(explosion, box, raycastFactory);
      double w = Math.sqrt(pos.squaredDistanceTo(explosion)) / (double)12.0F;
      double ac = ((double)1.0F - w) * ab;
      double dmg = (double)((float)((int)((ac * ac + ac) / (double)2.0F * (double)7.0F * (double)12.0F + (double)1.0F)));
      dmg = getReduction(entity, mc.world.getDamageSources().explosion((Explosion)null), dmg, assumeBestArmor);
      return Math.max((double)0.0F, dmg);
   }

   private static double getReduction(Entity entity, DamageSource damageSource, double damage, boolean assumeBestArmor) {
      if (damageSource.isScaledWithDifficulty()) {
         switch (mc.world.getDifficulty()) {
            case EASY -> damage = Math.min(damage / (double)2.0F + (double)1.0F, damage);
            case HARD -> damage *= (double)1.5F;
         }
      }

      if (entity instanceof LivingEntity livingEntity) {
         damage = (double)DamageUtil.getDamageLeft(livingEntity, (float)damage, damageSource, getArmor(livingEntity), (float)livingEntity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
         damage = getResistanceReduction(livingEntity, damage);
         damage = (double)getProtectionReduction(livingEntity, damage, damageSource, assumeBestArmor);
      }

      return Math.max(damage, (double)0.0F);
   }

   private static float getArmor(LivingEntity entity) {
      return (float)Math.floor(entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR));
   }

   private static float getProtectionReduction(Entity player, double damage, DamageSource source, boolean assumeBestArmor) {
      if (player instanceof LivingEntity livingEntity) {
         float protLevel = getProtectionAmount(livingEntity.getArmorItems(), assumeBestArmor);
         return DamageUtil.getInflictedDamage((float)damage, protLevel);
      } else {
         return 0.0F;
      }
   }

   private static float getProtectionAmount(Iterable<ItemStack> equipment, boolean assumeBestArmor) {
      MutableInt mutableInt = new MutableInt();
      equipment.forEach((i) -> {
         if (assumeBestArmor && EnchantmentUtil.isFakeEnchant2b2t(i)) {
            byte var10001;
            label16: {
               Item patt0$temp = i.getItem();
               if (patt0$temp instanceof net.minecraft.item.ArmorItem) {
                  net.minecraft.item.ArmorItem armorItem = (net.minecraft.item.ArmorItem)patt0$temp;
                  if (armorItem.getType() == net.minecraft.item.ArmorItem.Type.LEGGINGS) {
                     var10001 = 8;
                     break label16;
                  }
               }

               var10001 = 4;
            }

            mutableInt.add(var10001);
         } else {
            int modifierBlast = EnchantmentHelper.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BLAST_PROTECTION).get(), i);
            int modifier = EnchantmentHelper.getLevel((RegistryEntry)mc.world.getRegistryManager().get(Enchantments.PROTECTION.getRegistryRef()).getEntry(Enchantments.PROTECTION).get(), i);
            mutableInt.add(modifierBlast * 2 + modifier);
         }

      });
      return (float)mutableInt.intValue();
   }

   private static double getResistanceReduction(LivingEntity player, double damage) {
      StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
      if (resistance != null) {
         int lvl = resistance.getAmplifier() + 1;
         damage *= (double)(1.0F - (float)lvl * 0.2F);
      }

      return Math.max(damage, (double)0.0F);
   }

   private static <T extends LivingEntity> DefaultAttributeContainer getDefaultForEntity(T entity) {
      return DefaultAttributeRegistry.get((net.minecraft.entity.EntityType)entity.getType());
   }

   private static float getExposure(Vec3d source, Box box, IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks) {
      RaycastFactory raycastFactory = getRaycastFactory(ignoreTerrain, ignoreBlocks);
      return getExposure(source, box, raycastFactory);
   }

   private static float getExposure(Vec3d source, Box box, IgnoreTerrain ignoreTerrain) {
      RaycastFactory raycastFactory = getRaycastFactory(ignoreTerrain);
      return getExposure(source, box, raycastFactory);
   }

   private static float getExposure(Vec3d source, Box box, RaycastFactory raycastFactory) {
      double xDiff = box.maxX - box.minX;
      double yDiff = box.maxY - box.minY;
      double zDiff = box.maxZ - box.minZ;
      double xStep = (double)1.0F / (xDiff * (double)2.0F + (double)1.0F);
      double yStep = (double)1.0F / (yDiff * (double)2.0F + (double)1.0F);
      double zStep = (double)1.0F / (zDiff * (double)2.0F + (double)1.0F);
      if (xStep > (double)0.0F && yStep > (double)0.0F && zStep > (double)0.0F) {
         int misses = 0;
         int hits = 0;
         double xOffset = ((double)1.0F - Math.floor((double)1.0F / xStep) * xStep) * (double)0.5F;
         double zOffset = ((double)1.0F - Math.floor((double)1.0F / zStep) * zStep) * (double)0.5F;
         xStep *= xDiff;
         yStep *= yDiff;
         zStep *= zDiff;
         double startX = box.minX + xOffset;
         double startY = box.minY;
         double startZ = box.minZ + zOffset;
         double endX = box.maxX + xOffset;
         double endY = box.maxY;
         double endZ = box.maxZ + zOffset;

         for(double x = startX; x <= endX; x += xStep) {
            for(double y = startY; y <= endY; y += yStep) {
               for(double z = startZ; z <= endZ; z += zStep) {
                  Vec3d position = new Vec3d(x, y, z);
                  if (raycast(new ExposureRaycastContext(position, source), raycastFactory) == null) {
                     ++misses;
                  }

                  ++hits;
               }
            }
         }

         return (float)misses / (float)hits;
      } else {
         return 0.0F;
      }
   }

   private static RaycastFactory getRaycastFactory(IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks) {
      if (ignoreTerrain == ExplosionUtil.IgnoreTerrain.BLAST) {
         return (context, blockPos) -> {
            if (ignoreBlocks.contains(blockPos)) {
               return null;
            } else {
               BlockState blockState = mc.world.getBlockState(blockPos);
               return blockState.getBlock().getBlastResistance() < 600.0F ? null : blockState.getCollisionShape(mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
            }
         };
      } else {
         return ignoreTerrain == ExplosionUtil.IgnoreTerrain.ALL ? (context, blockPos) -> null : (context, blockPos) -> {
            if (ignoreBlocks.contains(blockPos)) {
               return null;
            } else {
               BlockState blockState = mc.world.getBlockState(blockPos);
               return blockState.getCollisionShape(mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
            }
         };
      }
   }

   private static RaycastFactory getRaycastFactory(IgnoreTerrain ignoreTerrain) {
      if (ignoreTerrain == ExplosionUtil.IgnoreTerrain.BLAST) {
         return (context, blockPos) -> {
            BlockState blockState = mc.world.getBlockState(blockPos);
            return blockState.getBlock().getBlastResistance() < 600.0F ? null : blockState.getCollisionShape(mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
         };
      } else {
         return ignoreTerrain == ExplosionUtil.IgnoreTerrain.ALL ? (context, blockPos) -> null : (context, blockPos) -> {
            BlockState blockState = mc.world.getBlockState(blockPos);
            return blockState.getCollisionShape(mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
         };
      }
   }

   private static BlockHitResult raycast(ExposureRaycastContext context, RaycastFactory raycastFactory) {
      return (BlockHitResult)BlockView.raycast(context.start, context.end, context, raycastFactory, (ctx) -> null);
   }

   public static record ExposureRaycastContext(Vec3d start, Vec3d end) {
   }

   public static enum IgnoreTerrain {
      ALL,
      BLAST,
      NONE;

      // $FF: synthetic method
      private static IgnoreTerrain[] $values() {
         return new IgnoreTerrain[]{ALL, BLAST, NONE};
      }
   }

   @FunctionalInterface
   public interface RaycastFactory extends BiFunction<ExposureRaycastContext, BlockPos, BlockHitResult> {
   }
}
