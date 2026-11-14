package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class ParkourModule extends ToggleModule {
   private boolean override;

   public ParkourModule() {
      super("Parkour", "Automatically jumps at the edge of blocks", ModuleCategory.MOVEMENT);
   }

   protected void onDisable() {
      super.onDisable();
      if (this.override) {
         this.override = false;
         mc.options.jumpKey.setPressed(false);
      }

   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player.isOnGround() && !mc.player.isSneaking() && mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset((double)0.0F, (double)-0.5F, (double)0.0F).expand(-0.001, (double)0.0F, -0.001))) {
         mc.options.jumpKey.setPressed(true);
         this.override = true;
      } else if (this.override) {
         this.override = false;
         mc.options.jumpKey.setPressed(false);
      }

   }
}
