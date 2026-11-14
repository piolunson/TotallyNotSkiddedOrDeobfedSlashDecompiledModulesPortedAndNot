package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.imixin.IPlayerInteractEntityC2SPacket;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.network.InteractType;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public final class AntiWeaknessModule extends ToggleModule {
   private static AntiWeaknessModule INSTANCE;
   Config<SwapMode> swapModeConfig;
   Config<Integer> delayConfig;
   Config<Boolean> onlyCrystalConfig;
   private final Timer delayTimer;
   private boolean ignore;

   public AntiWeaknessModule() {
      super("AntiWeakness", "Swaps to a sword to attack while affected by Weakness", ModuleCategory.COMBAT);
      this.swapModeConfig = this.register(new EnumConfig("SwapMode", "How to swap to a sword before attacking", AntiWeaknessModule.SwapMode.INVENTORY, AntiWeaknessModule.SwapMode.values()));
      this.delayConfig = this.register(new NumberConfig("Delay", "Delay in milliseconds between swaps", 0, 100, 500));
      this.onlyCrystalConfig = this.register(new BooleanConfig("OnlyCrystal", "Only swap when attacking an end crystal", true));
      this.delayTimer = new CacheTimer();
      INSTANCE = this;
   }

   public static AntiWeaknessModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      return ((SwapMode)this.swapModeConfig.getValue()).name();
   }

   @EventListener(
      priority = Integer.MAX_VALUE
   )
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null && !event.isCanceled() && !this.ignore) {
         if (mc.player.getStatusEffect(StatusEffects.WEAKNESS) != null) {
            if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
               if (this.delayTimer.passed((Number)this.delayConfig.getValue())) {
                  Packet var3 = event.getPacket();
                  if (var3 instanceof IPlayerInteractEntityC2SPacket) {
                     IPlayerInteractEntityC2SPacket packet = (IPlayerInteractEntityC2SPacket)var3;
                     if (packet.getType() == InteractType.ATTACK) {
                        if ((Boolean)this.onlyCrystalConfig.getValue() && !(packet.getEntity() instanceof EndCrystalEntity)) {
                           return;
                        }

                        this.delayTimer.reset();
                        this.ignore = true;
                        this.doAnti((PlayerInteractEntityC2SPacket)event.getPacket());
                        this.ignore = false;
                        event.cancel();
                     }
                  }

               }
            }
         }
      }
   }

   private void doAnti(PlayerInteractEntityC2SPacket attackPacket) {
      int swordSlot;
      if (this.swapModeConfig.getValue() != AntiWeaknessModule.SwapMode.INVENTORY) {
         swordSlot = this.findSwordSlot(0, 45);
      } else {
         swordSlot = this.findSwordSlot(9, 36);
      }

      if (swordSlot != -1) {
         int oldSlot = mc.player.getInventory().selectedSlot;
         switch (((SwapMode)this.swapModeConfig.getValue()).ordinal()) {
            case 0:
               Managers.INVENTORY.setClientSlot(swordSlot);
               Managers.NETWORK.sendPacket(attackPacket);
               break;
            case 1:
               Managers.INVENTORY.setSlot(swordSlot);
               Managers.NETWORK.sendPacket(attackPacket);
               Managers.INVENTORY.setSlot(oldSlot);
               break;
            case 2:
               int sourceSlot = swordSlot < 9 ? swordSlot + 36 : swordSlot;
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, sourceSlot, oldSlot, SlotActionType.SWAP, mc.player);
               Managers.NETWORK.sendPacket(attackPacket);
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, sourceSlot, oldSlot, SlotActionType.SWAP, mc.player);
               Managers.INVENTORY.syncToClient();
         }

      }
   }

   private int findSwordSlot(int start, int endExclusive) {
      for(int i = start; i < Math.min(endExclusive, 9); ++i) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof SwordItem) {
            return i;
         }
      }

      for(int i = Math.max(start, 9); i < Math.min(endExclusive, 36); ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof SwordItem) {
            return i;
         }
      }

      return -1;
   }

   public static enum SwapMode {
      NORMAL,
      SILENT,
      INVENTORY;

      // $FF: synthetic method
      private static SwapMode[] $values() {
         return new SwapMode[]{NORMAL, SILENT, INVENTORY};
      }
   }
}
