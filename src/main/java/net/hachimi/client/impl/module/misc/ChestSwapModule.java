package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.init.Managers;
import net.minecraft.util.Hand;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class ChestSwapModule extends ToggleModule {
   Config<Priority> priorityConfig;
   Config<Boolean> autoFireworkConfig;

   public ChestSwapModule() {
      super("ChestSwap", "Automatically swaps chestplate", ModuleCategory.MISCELLANEOUS);
      this.priorityConfig = this.register(new EnumConfig("Priority", "The chestplate material to prioritize", ChestSwapModule.Priority.NETHERITE, ChestSwapModule.Priority.values()));
      this.autoFireworkConfig = this.register(new BooleanConfig("AutoFirework", "Automatically fireworks when swapping to an elytra", false));
   }

   public void onEnable() {
      label61: {
         ItemStack armorStack = mc.player.getInventory().getArmorStack(2);
         Item var3 = armorStack.getItem();
         if (var3 instanceof ArmorItem armorItem) {
            if (armorItem.getSlotType() == EquipmentSlot.CHEST) {
               int elytraSlot = this.getElytraSlot();
               if (elytraSlot != -1) {
                  Managers.INVENTORY.pickupSlot(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot);
                  Managers.INVENTORY.pickupSlot(6);
                  Managers.INVENTORY.pickupSlot(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot);
                  if ((Boolean)this.autoFireworkConfig.getValue() && !mc.player.isOnGround()) {
                     Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                     mc.player.startFallFlying();
                     int slot = -1;

                     for(int i = 0; i < 45; ++i) {
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (stack.getItem() == Items.FIREWORK_ROCKET) {
                           slot = i;
                           break;
                        }
                     }

                     if (slot == -1) {
                        return;
                     }

                     if (slot < 9) {
                        Managers.INVENTORY.setSlot(slot);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        Managers.INVENTORY.syncToClient();
                     } else {
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                     }
                  }
               }
               break label61;
            }
         }

         int chestplateSlot = this.getChestplateSlot();
         if (chestplateSlot != -1) {
            Managers.INVENTORY.pickupSlot(chestplateSlot < 9 ? chestplateSlot + 36 : chestplateSlot);
            Managers.INVENTORY.pickupSlot(6);
            Managers.INVENTORY.pickupSlot(chestplateSlot < 9 ? chestplateSlot + 36 : chestplateSlot);
         }
      }

      this.disable();
   }

   private int getChestplateSlot() {
      int slot = -1;

      for(int i = 0; i < 36; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         Item var5 = stack.getItem();
         if (var5 instanceof ArmorItem armorItem) {
            if (armorItem.getSlotType() == EquipmentSlot.CHEST) {
               if (armorItem.getMaterial() == ArmorMaterials.NETHERITE && this.priorityConfig.getValue() == ChestSwapModule.Priority.NETHERITE) {
                  slot = i;
                  break;
               }

               if (armorItem.getMaterial() == ArmorMaterials.DIAMOND && this.priorityConfig.getValue() == ChestSwapModule.Priority.DIAMOND) {
                  slot = i;
                  break;
               }

               slot = i;
            }
         }
      }

      return slot;
   }

   private int getElytraSlot() {
      int slot = -1;

      for(int i = 0; i < 36; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof ElytraItem) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   private static enum Priority {
      NETHERITE,
      DIAMOND;

      // $FF: synthetic method
      private static Priority[] $values() {
         return new Priority[]{NETHERITE, DIAMOND};
      }
   }
}
