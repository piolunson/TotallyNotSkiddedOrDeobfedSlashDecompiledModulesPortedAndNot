package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public final class FireworkBoostModule extends ToggleModule {
   Config<Double> speedConfig = this.register(new NumberConfig("Speed", "The initial speed of the rocket.", (double)1.0F, (double)1.5F, (double)10.0F));
   Config<Boolean> accelerateConfig = this.register(new BooleanConfig("Accelerate", "Whether or not to accelerate the rocket.", false));
   Config<Double> accelSpeedConfig = this.register(new NumberConfig("AccelSpeed", "The speed to accelerate the rocket at.", 0.1, (double)0.5F, (double)5.0F, () -> this.accelerateConfig.getValue()));
   Config<Double> maxSpeedConfig = this.register(new NumberConfig("MaxSpeed", "The initial speed of the rocket.", (double)1.0F, (double)3.0F, (double)10.0F, () -> this.accelerateConfig.getValue()));
   private int tickCounter;
   private double speed;

   public FireworkBoostModule() {
      super("FireworkBoost", "Allows you to change the acceleration of your firework.", ModuleCategory.MOVEMENT);
   }

   public void onDisable() {
      this.speed = (double)0.0F;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && !mc.player.isInLava()) {
         if (this.isBoostedByRocket() && mc.player.isFallFlying()) {
            Vec3d vec3d = mc.player.getRotationVector();
            Vec3d vec3d2 = mc.player.getVelocity();
            if ((Boolean)this.accelerateConfig.getValue()) {
               this.speed += (Double)this.accelSpeedConfig.getValue() * (double)this.tickCounter;
               if (this.speed < (Double)this.maxSpeedConfig.getValue()) {
                  ++this.tickCounter;
               } else {
                  this.speed = MathHelper.clamp(this.speed, (double)0.0F, (Double)this.maxSpeedConfig.getValue());
               }
            } else {
               this.speed = (Double)this.speedConfig.getValue();
            }

            mc.player.setVelocity(vec3d2.add(vec3d.x * 0.1 + (vec3d.x * this.speed - vec3d2.x) * (double)0.5F, vec3d.y * 0.1 + (vec3d.y * this.speed - vec3d2.y) * (double)0.5F, vec3d.z * 0.1 + (vec3d.z * this.speed - vec3d2.z) * (double)0.5F));
         } else {
            this.speed = (double)0.0F;
         }

      }
   }

   private boolean isBoostedByRocket() {
      for(Entity entity : mc.world.getEntities()) {
         if (entity instanceof FireworkRocketEntity rocket) {
            if (((AccessorFireworkRocketEntity)(Object)rocket).hookWasShotByEntity() && ((AccessorFireworkRocketEntity)(Object)rocket).hookGetShooter() == mc.player) {
               return true;
            }
         }
      }

      return false;
   }
}
