package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.player.PlayerMoveEvent;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec2f;

public class NoAccelModule extends ToggleModule {
   Config<Boolean> airConfig = this.register(new BooleanConfig("Air", "Removes acceleration while in the air", true));
   Config<Boolean> downwardsConfig = this.register(new BooleanConfig("Downwards", "Removes acceleration while descending", true));

   public NoAccelModule() {
      super("NoAccel", "Removes sprint acceleration", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onPlayerMove(PlayerMoveEvent event) {
      if (!SpeedModule.getInstance().isEnabled() && !FlightModule.getInstance().isEnabled()) {
         if ((mc.player.isOnGround() || (Boolean)this.airConfig.getValue()) && (!(mc.player.getVelocity().y < (double)0.0F) || (Boolean)this.downwardsConfig.getValue()) && MovementUtil.isInputtingMovement()) {
            double speedEffect = (double)1.0F;
            double slowEffect = (double)1.0F;
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
               double amplifier = (double)mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
               speedEffect = (double)1.0F + 0.2 * (amplifier + (double)1.0F);
            }

            if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
               double amplifier = (double)mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
               slowEffect = (double)1.0F + 0.2 * (amplifier + (double)1.0F);
            }

            double base = (double)0.2873F * speedEffect / slowEffect;
            Vec2f motion = SpeedModule.getInstance().handleVanillaMotion((float)base);
            event.cancel();
            event.setX((double)motion.x);
            event.setZ((double)motion.y);
         }
      }
   }
}
