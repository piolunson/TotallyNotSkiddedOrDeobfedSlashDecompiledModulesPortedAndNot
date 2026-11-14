package net.hachimi.client.impl.module.render;

import java.util.HashMap;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.network.packet.Packet;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;

public class KillEffectsModule extends ToggleModule {
   Config<KillEffect> killEffectConfig;
   Config<Integer> strikesConfig;
   private final Map<Entity, Long> lastAttackedEntities;

   public KillEffectsModule() {
      super("KillEffects", "Adds effects to player deaths", ModuleCategory.RENDER);
      this.killEffectConfig = this.register(new EnumConfig("Effect", "The kill effect to apply", KillEffectsModule.KillEffect.LIGHTNING, KillEffectsModule.KillEffect.values()));
      this.strikesConfig = this.register(new NumberConfig("Strikes", "The number of lightning strikes", 1, 1, 5, () -> this.killEffectConfig.getValue() == KillEffectsModule.KillEffect.LIGHTNING));
      this.lastAttackedEntities = new HashMap<>();
   }

   @EventListener
   public void onEntityDeath(EntityDeathEvent event) {
      if (event.getEntity() != mc.player) {
         LivingEntity var3 = event.getEntity();
         if (var3 instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)var3;
            if (this.wasLastAttackedByPlayer(player)) {
               switch (((KillEffect)this.killEffectConfig.getValue()).ordinal()) {
                  case 0:
                     for(int i = 0; i < (Integer)this.strikesConfig.getValue(); ++i) {
                        LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
                        lightningEntity.setPos(player.getX(), player.getY(), player.getZ());
                        mc.world.addEntity(lightningEntity);
                     }
                     break;
                  case 1:
                     this.fireworkExplode(player.getX(), player.getY(), player.getZ(), (double)0.5F, 4);
               }

               return;
            }
         }
      }

   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.world != null && mc.player != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof EntityDamageS2CPacket) {
            EntityDamageS2CPacket packet = (EntityDamageS2CPacket)var3;
            if (packet.sourceCauseId() == mc.player.getId()) {
               this.lastAttackedEntities.entrySet().removeIf((e) -> System.currentTimeMillis() - (Long)e.getValue() > 5000L);
               Entity entity = mc.world.getEntityById(packet.entityId());
               if (entity == null) {
                  return;
               }

               this.lastAttackedEntities.put(entity, System.currentTimeMillis());
            }
         }

      }
   }

   private void fireworkExplode(double x, double y, double z, double size, int amount) {
      double d = x;
      double e = y;
      double f = z;

      for(int i = -amount; i <= amount; ++i) {
         for(int j = -amount; j <= amount; ++j) {
            for(int k = -amount; k <= amount; ++k) {
               double g = (double)j + (RANDOM.nextDouble() - RANDOM.nextDouble()) * (double)0.5F;
               double h = (double)i + (RANDOM.nextDouble() - RANDOM.nextDouble()) * (double)0.5F;
               double l = (double)k + (RANDOM.nextDouble() - RANDOM.nextDouble()) * (double)0.5F;
               double m = Math.sqrt(g * g + h * h + l * l) / size + RANDOM.nextGaussian() * 0.05;
               this.addExplosionParticle(d, e, f, g / m, h / m, l / m);
               if (i != -amount && i != amount && j != -amount && j != amount) {
                  k += amount * 2 - 1;
               }
            }
         }
      }

   }

   private void addExplosionParticle(double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
      if (mc.particleManager != null && mc.world != null) {
         net.minecraft.client.particle.FireworksSparkParticle.Explosion explosion = (net.minecraft.client.particle.FireworksSparkParticle.Explosion)mc.particleManager.addParticle(ParticleTypes.FIREWORK, x, y, z, velocityX, velocityY, velocityZ);
         if (explosion != null) {
            explosion.setTrail(false);
            explosion.setFlicker(false);
            explosion.setColor(ColorsModule.getInstance().getRGB());
         }
      }
   }

   private boolean wasLastAttackedByPlayer(Entity entity) {
      Long lastAttackedTime = (Long)this.lastAttackedEntities.get(entity);
      return lastAttackedTime != null && System.currentTimeMillis() - lastAttackedTime < 5000L;
   }

   private static enum KillEffect {
      LIGHTNING,
      FIREWORK;

      // $FF: synthetic method
      private static KillEffect[] $values() {
         return new KillEffect[]{LIGHTNING, FIREWORK};
      }
   }
}
