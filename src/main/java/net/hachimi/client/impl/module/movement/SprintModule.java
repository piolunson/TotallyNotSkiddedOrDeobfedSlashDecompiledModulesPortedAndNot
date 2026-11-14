package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.JumpRotationEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.network.SprintCancelEvent;
import net.hachimi.client.impl.module.RotationModule;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

public class SprintModule extends RotationModule {
   private static SprintModule INSTANCE;
   Config<SprintMode> modeConfig;
   Config<Boolean> jumpFixConfig;

   public SprintModule() {
      super("Sprint", "Automatically sprints", ModuleCategory.MOVEMENT, 110);
      this.modeConfig = this.register(new EnumConfig("Mode", "Sprinting mode. Rage allows for multi-directional sprinting.", SprintModule.SprintMode.LEGIT, SprintModule.SprintMode.values()));
      this.jumpFixConfig = this.register(new BooleanConfig("JumpFix", "Fixes jumping slowdown in Rage sprint", true, () -> this.modeConfig.getValue() == SprintModule.SprintMode.RAGE || this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT));
      INSTANCE = this;
   }

   public static SprintModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (this.canSprint()) {
            float sprintYaw = this.getSprintYaw(mc.player.getYaw());
            if (this.checkSprintAngle(sprintYaw)) {
               return;
            }

            switch (((SprintMode)this.modeConfig.getValue()).ordinal()) {
               case 0:
                  if (mc.player.input.hasForwardMovement() && (!mc.player.horizontalCollision || mc.player.collidedSoftly)) {
                     mc.player.setSprinting(true);
                  }
                  break;
               case 1:
               case 2:
               case 3:
                  mc.player.setSprinting(true);
            }
         }

      }
   }

   @EventListener
   public void onSprintCancel(SprintCancelEvent event) {
      if (this.canSprint() && (this.modeConfig.getValue() == SprintModule.SprintMode.RAGE || this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT)) {
         float sprintYaw = this.getSprintYaw(mc.player.getYaw());
         if (this.checkSprintAngle(sprintYaw)) {
            return;
         }

         event.cancel();
      }

   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (this.canSprint() && this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT) {
         this.setRotation(this.getSprintYaw(mc.player.getYaw()), mc.player.getPitch());
      }

   }

   @EventListener
   public void onJumpYaw(JumpRotationEvent event) {
      if ((Boolean)this.jumpFixConfig.getValue() && (this.modeConfig.getValue() == SprintModule.SprintMode.RAGE || this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT)) {
         float yaw = event.getYaw();
         float forward = Math.signum(mc.player.input.movementForward);
         float strafe = 90.0F * Math.signum(mc.player.input.movementSideways);
         if (forward != 0.0F) {
            strafe *= forward * 0.5F;
         }

         yaw -= strafe;
         if (forward < 0.0F) {
            yaw -= 180.0F;
         }

         event.cancel();
         event.setYaw(yaw);
      }

   }

   private boolean canSprint() {
      if (AnticheatModule.getInstance().getWebJumpFix() && PlayerUtil.inWeb((double)1.0F)) {
         return false;
      } else {
         return MovementUtil.isInputtingMovement() && !mc.player.isSneaking() && !mc.player.isRiding() && !mc.player.isFallFlying() && !mc.player.isTouchingWater() && !mc.player.isInLava() && !mc.player.isHoldingOntoLadder() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && (float)mc.player.getHungerManager().getFoodLevel() > 6.0F;
      }
   }

   private boolean checkSprintAngle(float sprintYaw) {
      if (this.modeConfig.getValue() == SprintModule.SprintMode.RAGE_STRICT) {
         return MathHelper.angleBetween(sprintYaw, Managers.ROTATION.getServerYaw()) > 0.0F;
      } else if (this.modeConfig.getValue() == SprintModule.SprintMode.GRIM) {
         return MathHelper.angleBetween(mc.player.getYaw(), Managers.ROTATION.getServerYaw()) > 0.0F;
      } else {
         return false;
      }
   }

   public float getSprintYaw(float yaw) {
      boolean forward = mc.options.forwardKey.isPressed();
      boolean backward = mc.options.backKey.isPressed();
      boolean left = mc.options.leftKey.isPressed();
      boolean right = mc.options.rightKey.isPressed();
      if (forward && !backward) {
         if (left && !right) {
            yaw -= 45.0F;
         } else if (right && !left) {
            yaw += 45.0F;
         }
      } else if (backward && !forward) {
         yaw += 180.0F;
         if (left && !right) {
            yaw += 45.0F;
         } else if (right && !left) {
            yaw -= 45.0F;
         }
      } else if (left && !right) {
         yaw -= 90.0F;
      } else if (right && !left) {
         yaw += 90.0F;
      }

      return MathHelper.wrapDegrees(yaw);
   }

   public static enum SprintMode {
      LEGIT,
      RAGE,
      RAGE_STRICT,
      GRIM;

      // $FF: synthetic method
      private static SprintMode[] $values() {
         return new SprintMode[]{LEGIT, RAGE, RAGE_STRICT, GRIM};
      }
   }
}
