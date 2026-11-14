package net.hachimi.client.impl.module.combat;

import java.util.PriorityQueue;
import java.util.Queue;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.DataComponentTypes;

public class AutoArmorModule extends ToggleModule {
   Config<Priority> priorityConfig;
   Config<Float> minDurabilityConfig;
   Config<Boolean> elytraPriorityConfig;
   Config<Boolean> blastLeggingsConfig;
   Config<Boolean> noBindingConfig;
   Config<Boolean> inventoryConfig;
   private final Queue<ArmorSlot> helmet;
   private final Queue<ArmorSlot> chestplate;
   private final Queue<ArmorSlot> leggings;
   private final Queue<ArmorSlot> boots;

   public AutoArmorModule() {
      super("AutoArmor", "Automatically replaces armor pieces", ModuleCategory.COMBAT);
      this.priorityConfig = this.register(new EnumConfig("Priority", "Armor enchantment priority", AutoArmorModule.Priority.BLAST_PROTECTION, AutoArmorModule.Priority.values()));
      this.minDurabilityConfig = this.register(new NumberConfig("MinDurability", "Durability percent to replace armor", 0.0F, 0.0F, 20.0F, NumberDisplay.PERCENT));
      this.elytraPriorityConfig = this.register(new BooleanConfig("ElytraPriority", "Prioritizes existing elytras in the chestplate armor slot", true));
      this.blastLeggingsConfig = this.register(new BooleanConfig("Leggings-BlastPriority", "Prioritizes Blast Protection leggings", true));
      this.noBindingConfig = this.register(new BooleanConfig("NoBinding", "Avoids armor with the Curse of Binding enchantment", true));
      this.inventoryConfig = this.register(new BooleanConfig("AllowInventory", "Allows armor to be swapped while in the inventory menu", false));
      this.helmet = new PriorityQueue<>();
      this.chestplate = new PriorityQueue<>();
      this.leggings = new PriorityQueue<>();
      this.boots = new PriorityQueue<>();
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (mc.currentScreen == null || mc.currentScreen instanceof InventoryScreen && (Boolean)this.inventoryConfig.getValue()) {
         this.helmet.clear();
         this.chestplate.clear();
         this.leggings.clear();
         this.boots.clear();

         for(int j = 0; j < 36; ++j) {
            ItemStack stack = mc.player.getInventory().getStack(j);
            if (!stack.isEmpty()) {
               Item var5 = stack.getItem();
               if (var5 instanceof ArmorItem) {
                  ArmorItem armor = (ArmorItem)var5;
                  if (!(Boolean)this.noBindingConfig.getValue() || !this.hasEnchantment(stack, Enchantments.BINDING_CURSE)) {
                     int index = armor.getSlotType().getEntitySlotId();
                     float dura = (float)(stack.getMaxDamage() - stack.getDamage()) / (float)stack.getMaxDamage();
                     if (!(dura < (Float)this.minDurabilityConfig.getValue())) {
                        ArmorSlot data = new ArmorSlot(index, j, stack);
                        switch (index) {
                           case 0:
                              this.helmet.add(data);
                              break;
                           case 1:
                              this.chestplate.add(data);
                              break;
                           case 2:
                              this.leggings.add(data);
                              break;
                           case 3:
                              this.boots.add(data);
                        }
                     }
                  }
               }
            }
         }

         for(int i = 0; i < 4; ++i) {
            ItemStack armorStack = mc.player.getInventory().getArmorStack(i);
            if (!(Boolean)this.elytraPriorityConfig.getValue() || armorStack.getItem() != Items.ELYTRA) {
               float armorDura = (float)(armorStack.getMaxDamage() - armorStack.getDamage()) / (float)armorStack.getMaxDamage();
               if (armorStack.isEmpty() && !(armorDura >= (Float)this.minDurabilityConfig.getValue())) {
                  switch (i) {
                     case 0:
                        if (!this.helmet.isEmpty()) {
                           ArmorSlot helmetSlot = (ArmorSlot)this.helmet.poll();
                           this.swapArmor(helmetSlot.getType(), helmetSlot.getSlot());
                        }
                        break;
                     case 1:
                        if (!this.chestplate.isEmpty()) {
                           ArmorSlot chestSlot = (ArmorSlot)this.chestplate.poll();
                           this.swapArmor(chestSlot.getType(), chestSlot.getSlot());
                        }
                        break;
                     case 2:
                        if (!this.leggings.isEmpty()) {
                           ArmorSlot leggingsSlot = (ArmorSlot)this.leggings.poll();
                           this.swapArmor(leggingsSlot.getType(), leggingsSlot.getSlot());
                        }
                        break;
                     case 3:
                        if (!this.boots.isEmpty()) {
                           ArmorSlot bootsSlot = (ArmorSlot)this.boots.poll();
                           this.swapArmor(bootsSlot.getType(), bootsSlot.getSlot());
                        }
                  }
               }
            }
         }

      }
   }

   public void swapArmor(int armorSlot, int slot) {
      ItemStack stack = mc.player.getInventory().getArmorStack(armorSlot);
      armorSlot = 8 - armorSlot;
      Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
      boolean rt = !stack.isEmpty();
      Managers.INVENTORY.pickupSlot(armorSlot);
      if (rt) {
         Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
      }

   }

   public boolean hasEnchantment(ItemStack armorStack, RegistryKey<Enchantment> enchantment) {
      if (armorStack.getComponents().contains(DataComponentTypes.ENCHANTMENTS)) {
         for(RegistryEntry<Enchantment> entry : ((ItemEnchantmentsComponent)armorStack.getComponents().get(DataComponentTypes.ENCHANTMENTS)).getEnchantments()) {
            if (entry.getKey().isPresent() && ((RegistryKey)entry.getKey().get()).equals(enchantment)) {
               return true;
            }
         }
      }

      return false;
   }

   public static enum Priority {
      BLAST_PROTECTION(Enchantments.BLAST_PROTECTION),
      PROTECTION(Enchantments.PROTECTION),
      PROJECTILE_PROTECTION(Enchantments.PROJECTILE_PROTECTION);

      private final RegistryKey<Enchantment> enchant;

      private Priority(RegistryKey<Enchantment> enchant) {
         this.enchant = enchant;
      }

      public RegistryKey<Enchantment> getEnchantment() {
         return this.enchant;
      }

      // $FF: synthetic method
      private static Priority[] $values() {
         return new Priority[]{BLAST_PROTECTION, PROTECTION, PROJECTILE_PROTECTION};
      }
   }

   public class ArmorSlot implements Comparable<ArmorSlot> {
      private final int armorType;
      private final int slot;
      private final ItemStack armorStack;

      public ArmorSlot(int armorType, int slot, ItemStack armorStack) {
         this.armorType = armorType;
         this.slot = slot;
         this.armorStack = armorStack;
      }

      public int compareTo(ArmorSlot other) {
         if (this.armorType != other.armorType) {
            return 0;
         } else {
            ItemStack otherStack = other.getArmorStack();
            ArmorItem armorItem = (ArmorItem)this.armorStack.getItem();
            ArmorItem otherItem = (ArmorItem)otherStack.getItem();
            int durabilityDiff = ((ArmorMaterial)armorItem.getMaterial().value()).getProtection(armorItem.getType()) - ((ArmorMaterial)otherItem.getMaterial().value()).getProtection(otherItem.getType());
            if (durabilityDiff != 0) {
               return durabilityDiff;
            } else {
               RegistryKey<Enchantment> enchantment = ((Priority)AutoArmorModule.this.priorityConfig.getValue()).getEnchantment();
               if ((Boolean)AutoArmorModule.this.blastLeggingsConfig.getValue() && this.armorType == 2 && AutoArmorModule.this.hasEnchantment(this.armorStack, Enchantments.BLAST_PROTECTION)) {
                  return -1;
               } else if (AutoArmorModule.this.hasEnchantment(this.armorStack, enchantment)) {
                  return AutoArmorModule.this.hasEnchantment(otherStack, enchantment) ? 0 : -1;
               } else {
                  return AutoArmorModule.this.hasEnchantment(otherStack, enchantment) ? 1 : 0;
               }
            }
         }
      }

      public ItemStack getArmorStack() {
         return this.armorStack;
      }

      public int getType() {
         return this.armorType;
      }

      public int getSlot() {
         return this.slot;
      }
   }
}
