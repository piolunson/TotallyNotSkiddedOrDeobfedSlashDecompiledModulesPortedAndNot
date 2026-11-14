package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.RotationModule;
import net.hachimi.eventbus.annotation.EventListener;

public class AntiAimModule extends RotationModule {
   Config<YawMode> yawModeConfig;
   Config<PitchMode> pitchModeConfig;
   Config<Float> yawAddConfig;
   Config<Float> pitchAddConfig;
   Config<Float> spinSpeedConfig;
   Config<Integer> flipTicksConfig;
   private float yaw;
   private float pitch;
   private float prevYaw;
   private float prevPitch;

   public AntiAimModule() {
      super("AntiAim", "Makes it harder to accurately aim at the player", ModuleCategory.MISCELLANEOUS, 50);
      this.yawModeConfig = this.register(new EnumConfig("Yaw", "The mode for the rotation yaw spin ", AntiAimModule.YawMode.SPIN, AntiAimModule.YawMode.values()));
      this.pitchModeConfig = this.register(new EnumConfig("Pitch", "The mode for the rotation pitch spin", AntiAimModule.PitchMode.DOWN, AntiAimModule.PitchMode.values()));
      this.yawAddConfig = this.register(new NumberConfig("YawAdd", "The yaw to add during each rotation", -180.0F, 20.0F, 180.0F));
      this.pitchAddConfig = this.register(new NumberConfig("CustomPitch", "The pitch to add during each rotation", -90.0F, 20.0F, 90.0F));
      this.spinSpeedConfig = this.register(new NumberConfig("SpinSpeed", "The yaw speed to rotate", 1.0F, 16.0F, 40.0F));
      this.flipTicksConfig = this.register(new NumberConfig("FlipTicks", "The number of ticks to wait between jitter", 2, 2, 20));
   }

   public void onEnable() {
      if (mc.player != null) {
         this.prevYaw = mc.player.getYaw();
         this.prevPitch = mc.player.getPitch();
      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      if (!mc.options.attackKey.isPressed() && !mc.options.useKey.isPressed()) {
         float var10001;
         switch (((YawMode)this.yawModeConfig.getValue()).ordinal()) {
            case 0:
               var10001 = mc.player.getYaw();
               break;
            case 1:
               var10001 = mc.player.getYaw() + (Float)this.yawAddConfig.getValue();
               break;
            case 2:
               var10001 = this.prevYaw;
               break;
            case 3:
               float spin = this.yaw + (Float)this.spinSpeedConfig.getValue();
               var10001 = spin > 360.0F ? spin - 360.0F : spin;
               break;
            case 4:
               var10001 = mc.player.getYaw() + (mc.player.age % (Integer)this.flipTicksConfig.getValue() == 0 ? (Float)this.yawAddConfig.getValue() : -(Float)this.yawAddConfig.getValue());
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
         }

         this.yaw = var10001;
         switch (((PitchMode)this.pitchModeConfig.getValue()).ordinal()) {
            case 0:
               var10001 = mc.player.getPitch();
               break;
            case 1:
               var10001 = (Float)this.pitchAddConfig.getValue();
               break;
            case 2:
               var10001 = this.prevPitch;
               break;
            case 3:
               var10001 = -90.0F;
               break;
            case 4:
               var10001 = 90.0F;
               break;
            case 5:
               float jitter = this.pitch + 30.0F;
               var10001 = jitter > 90.0F ? -90.0F : (jitter < -90.0F ? 90.0F : jitter);
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
         }

         this.pitch = var10001;
         this.setRotation(this.yaw, this.pitch);
      }
   }

   public static enum YawMode {
      OFF,
      STATIC,
      ZERO,
      SPIN,
      JITTER;

      // $FF: synthetic method
      private static YawMode[] $values() {
         return new YawMode[]{OFF, STATIC, ZERO, SPIN, JITTER};
      }
   }

   public static enum PitchMode {
      OFF,
      STATIC,
      ZERO,
      UP,
      DOWN,
      JITTER;

      // $FF: synthetic method
      private static PitchMode[] $values() {
         return new PitchMode[]{OFF, STATIC, ZERO, UP, DOWN, JITTER};
      }
   }
}
