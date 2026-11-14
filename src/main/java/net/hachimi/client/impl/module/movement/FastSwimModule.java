package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.player.PlayerMoveEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.EnchantmentUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.Vec2f;
import net.minecraft.registry.tag.FluidTags;

public class FastSwimModule extends ToggleModule {
   Config<SwimMode> modeConfig;
   Config<Float> waterSpeedConfig;
   Config<Float> lavaSpeedConfig;
   Config<Boolean> verticalConfig;
   Config<Boolean> elytraConfig;
   Config<Float> elytraSpeedConfig;
   Config<Boolean> depthStriderConfig;

   public FastSwimModule() {
      super("FastSwim", "Move faster in liquids", ModuleCategory.MOVEMENT);
      this.modeConfig = this.register(new EnumConfig("Mode", "The mode for swimming", FastSwimModule.SwimMode.VANILLA, FastSwimModule.SwimMode.values()));
      this.waterSpeedConfig = this.register(new NumberConfig("WaterSpeed", "Base speed for moving through water", 0.1F, 1.0F, 10.0F));
      this.lavaSpeedConfig = this.register(new NumberConfig("LavaSpeed", "Base speed for moving through lava", 0.1F, 1.0F, 10.0F));
      this.verticalConfig = this.register(new BooleanConfig("Vertical", "Allows moving vertically", true));
      this.elytraConfig = this.register(new BooleanConfig("Elytra", "Applies elytra speed when moving through liquids", false));
      this.elytraSpeedConfig = this.register(new NumberConfig("ElytraSpeed", "Base speed for moving through lava", 0.1F, 1.0F, 10.0F, () -> this.elytraConfig.getValue()));
      this.depthStriderConfig = this.register(new BooleanConfig("DepthStrider", "Prefers depth strider", false));
   }

   @EventListener
   public void onPlayerMove(PlayerMoveEvent event) {
      if ((Boolean)this.depthStriderConfig.getValue() || EnchantmentUtil.getLevel(mc.player.getEquippedStack(EquipmentSlot.FEET), Enchantments.DEPTH_STRIDER) <= 0) {
         if (mc.player.isSubmergedIn(FluidTags.WATER) || mc.player.isSubmergedIn(FluidTags.LAVA)) {
            event.cancel();
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem && (Boolean)this.elytraConfig.getValue()) {
               event.setX(event.getX() * (double)(Float)this.elytraSpeedConfig.getValue());
               event.setZ(event.getZ() * (double)(Float)this.elytraSpeedConfig.getValue());
               if ((Boolean)this.verticalConfig.getValue()) {
                  if (mc.options.jumpKey.isPressed()) {
                     event.setY(event.getY() + 0.16);
                     Managers.MOVEMENT.setMotionY(event.getY() + 0.16);
                  } else if (mc.options.sneakKey.isPressed()) {
                     event.setY(event.getY() - 0.12);
                     Managers.MOVEMENT.setMotionY(event.getY() - 0.12);
                  }
               }

            } else {
               float speed = mc.player.isSubmergedIn(FluidTags.WATER) ? (Float)this.waterSpeedConfig.getValue() : (Float)this.lavaSpeedConfig.getValue();
               switch (((SwimMode)this.modeConfig.getValue()).ordinal()) {
                  case 0:
                     event.setX(event.getX() * (double)speed);
                     event.setZ(event.getZ() * (double)speed);
                     break;
                  case 1:
                     Vec2f strafe = SpeedModule.getInstance().handleStrafeMotion(speed / 10.0F);
                     event.setX((double)strafe.x);
                     event.setZ((double)strafe.y);
               }

               if ((Boolean)this.verticalConfig.getValue()) {
                  if (mc.options.jumpKey.isPressed()) {
                     event.setY(event.getY() + 0.16);
                     Managers.MOVEMENT.setMotionY(event.getY() + 0.16);
                  } else if (mc.options.sneakKey.isPressed()) {
                     event.setY(event.getY() - 0.12);
                     Managers.MOVEMENT.setMotionY(event.getY() - 0.12);
                  }
               }

            }
         }
      }
   }

   private static enum SwimMode {
      VANILLA,
      NORMAL;

      // $FF: synthetic method
      private static SwimMode[] $values() {
         return new SwimMode[]{VANILLA, NORMAL};
      }
   }
}
