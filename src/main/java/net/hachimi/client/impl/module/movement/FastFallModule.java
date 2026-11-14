package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.player.PlayerMoveEvent;
import net.hachimi.client.impl.event.network.TickMovementEvent;
import net.hachimi.client.impl.module.exploit.PacketFlyModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.math.Box;

public class FastFallModule extends ToggleModule {
   Config<Float> heightConfig = this.register(new NumberConfig("Height", "The maximum fall height", 1.0F, 3.0F, 10.0F));
   Config<FallMode> fallModeConfig;
   Config<Boolean> accelerateConfig;
   Config<Integer> shiftTicksConfig;
   private boolean prevOnGround;
   private boolean cancelFallMovement;
   private int fallTicks;
   private final Timer fallTimer;

   public FastFallModule() {
      super("FastFall", "Falls down blocks faster", ModuleCategory.MOVEMENT);
      this.fallModeConfig = this.register(new EnumConfig("Mode", "The mode for falling down blocks", FastFallModule.FallMode.STEP, FastFallModule.FallMode.values()));
      this.accelerateConfig = this.register(new BooleanConfig("Accelerate", "Accels the fall speed", false, () -> this.fallModeConfig.getValue() == FastFallModule.FallMode.STEP));
      this.shiftTicksConfig = this.register(new NumberConfig("ShiftTicks", "Number of ticks to shift ahead", 1, 3, 5, () -> this.fallModeConfig.getValue() == FastFallModule.FallMode.SHIFT));
      this.fallTimer = new CacheTimer();
   }

   public void onDisable() {
      this.cancelFallMovement = false;
      this.fallTicks = 0;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         this.prevOnGround = mc.player.isOnGround();
         if (this.fallModeConfig.getValue() == FastFallModule.FallMode.STEP) {
            if (mc.player.isRiding() || mc.player.isFallFlying() || mc.player.isHoldingOntoLadder() || mc.player.isInLava() || mc.player.isTouchingWater() || mc.player.input.jumping || mc.player.input.sneaking || !Managers.ANTICHEAT.hasPassed(1000L)) {
               return;
            }

            if (SpeedModule.getInstance().isEnabled() || LongJumpModule.getInstance().isEnabled() || FlightModule.getInstance().isEnabled() || PacketFlyModule.getInstance().isEnabled()) {
               return;
            }

            double fallHeight = this.traceDown();
            if (fallHeight > 0.01 && fallHeight <= (double)(Float)this.heightConfig.getValue() && mc.player.isOnGround()) {
               Managers.MOVEMENT.setMotionXZ(mc.player.getVelocity().x * 0.05, mc.player.getVelocity().z * 0.05);
               Managers.MOVEMENT.setMotionY((Boolean)this.accelerateConfig.getValue() ? mc.player.getVelocity().y - (double)0.62F : (double)-3.0F);
            }
         }
      }

   }

   @EventListener
   public void onTickMovement(TickMovementEvent event) {
      if (this.fallModeConfig.getValue() == FastFallModule.FallMode.SHIFT) {
         if (mc.player.isRiding() || mc.player.isFallFlying() || mc.player.isHoldingOntoLadder() || mc.player.isInLava() || mc.player.isTouchingWater() || mc.player.input.jumping || mc.player.input.sneaking || !Managers.ANTICHEAT.hasPassed(1000L)) {
            return;
         }

         if (!Managers.ANTICHEAT.hasPassed(1000L) || !this.fallTimer.passed(1000) || SpeedModule.getInstance().isEnabled() || LongJumpModule.getInstance().isEnabled() || FlightModule.getInstance().isEnabled() || PacketFlyModule.getInstance().isEnabled()) {
            return;
         }

         double fallHeight = this.traceDown();
         if (fallHeight > 0.01 && fallHeight <= (double)(Float)this.heightConfig.getValue() + 0.01) {
            if (mc.player.isOnGround()) {
               Managers.MOVEMENT.setMotionXZ(mc.player.getVelocity().x * 0.05, mc.player.getVelocity().z * 0.05);
            }

            if (mc.player.getVelocity().y < (double)0.0F && this.prevOnGround && !mc.player.isOnGround()) {
               this.fallTimer.reset();
               event.cancel();
               event.setIterations((Integer)this.shiftTicksConfig.getValue());
               this.cancelFallMovement = true;
               this.fallTicks = 0;
            }
         }
      }

   }

   @EventListener
   public void onPlayerMove(PlayerMoveEvent event) {
      if (!FlightModule.getInstance().isEnabled() && !PacketFlyModule.getInstance().isEnabled()) {
         if (this.cancelFallMovement && this.fallModeConfig.getValue() == FastFallModule.FallMode.SHIFT) {
            event.setX((double)0.0F);
            event.setZ((double)0.0F);
            Managers.MOVEMENT.setMotionXZ((double)0.0F, (double)0.0F);
            ++this.fallTicks;
            if (this.fallTicks > (Integer)this.shiftTicksConfig.getValue()) {
               this.cancelFallMovement = false;
               this.fallTicks = 0;
            }
         }

      }
   }

   private double traceDown() {
      Box bb = mc.player.getBoundingBox();

      for(double i = (double)0.0F; i < (double)(Float)this.heightConfig.getValue() + (double)0.5F; i += 0.01) {
         if (!mc.world.isSpaceEmpty(mc.player, bb.offset((double)0.0F, -i, (double)0.0F))) {
            return i;
         }
      }

      return (double)-1.0F;
   }

   public static enum FallMode {
      STEP,
      SHIFT;

      // $FF: synthetic method
      private static FallMode[] $values() {
         return new FallMode[]{STEP, SHIFT};
      }
   }
}
