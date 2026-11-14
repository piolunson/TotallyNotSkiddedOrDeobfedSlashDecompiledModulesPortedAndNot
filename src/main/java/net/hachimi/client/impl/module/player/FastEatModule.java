package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.network.SetCurrentHandEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.component.DataComponentTypes;

public final class FastEatModule extends ToggleModule {
   Config<Mode> modeConfig;
   Config<Integer> ticksConfig;
   private int packets;

   public FastEatModule() {
      super("FastEat", "Allows you to consume items faster", ModuleCategory.PLAYER);
      this.modeConfig = this.register(new EnumConfig("Mode", "The bypass mode", FastEatModule.Mode.VANILLA, FastEatModule.Mode.values()));
      this.ticksConfig = this.register(new NumberConfig("Ticks", "The amount of ticks to have 'consumed' an item before fast eating", 0, 10, 30));
   }

   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!MovementUtil.isMoving() && mc.player.isOnGround()) {
         ++this.packets;
         if (this.packets > 100) {
            this.packets = 100;
         }
      } else {
         --this.packets;
         if (this.packets <= 0) {
            this.packets = 0;
         }
      }

      if (mc.player.isUsingItem()) {
         ItemStack stack = mc.player.getStackInHand(mc.player.getActiveHand());
         if (!stack.isEmpty() && stack.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            int timeUsed = mc.player.getItemUseTime();
            if (timeUsed >= (Integer)this.ticksConfig.getValue()) {
               int usePackets = 32 - timeUsed;

               for(int i = 0; i < usePackets; ++i) {
                  ((Mode)this.modeConfig.getValue()).ordinal();
               }
            }

         }
      }
   }

   @EventListener
   public void onSetCurrentHand(SetCurrentHandEvent event) {
      if (this.modeConfig.getValue() == FastEatModule.Mode.SHIFT) {
         ItemStack stack = event.getStackInHand();
         if (!stack.getItem().getComponents().contains(DataComponentTypes.FOOD) && !(stack.getItem() instanceof PotionItem)) {
            return;
         }

         int maxUseTime = stack.getMaxUseTime(mc.player);
         if (this.packets < maxUseTime) {
            return;
         }

         for(int i = 0; i < maxUseTime; ++i) {
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
            this.packets -= maxUseTime;
         }

         event.cancel();
         stack.getItem().finishUsing(stack, mc.world, mc.player);
      }

   }

   private static enum Mode {
      VANILLA,
      SHIFT,
      GRIM;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{VANILLA, SHIFT, GRIM};
      }
   }
}
