package net.hachimi.client.util.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.DataComponentTypes;

public class EnchantmentUtil {
   public static int getLevel(ItemStack stack, RegistryKey<Enchantment> enchantmentRegistryKey) {
      if (!stack.getComponents().contains(DataComponentTypes.ENCHANTMENTS)) {
         return 0;
      } else {
         for(Object2IntMap.Entry<RegistryEntry<Enchantment>> e : ((ItemEnchantmentsComponent)stack.getComponents().get(DataComponentTypes.ENCHANTMENTS)).getEnchantmentEntries()) {
            if (((RegistryEntry)e.getKey()).getKey().isPresent() && ((RegistryKey)((RegistryEntry)e.getKey()).getKey().get()).equals(enchantmentRegistryKey)) {
               return e.getIntValue();
            }
         }

         return 0;
      }
   }

   public static boolean isFakeEnchant2b2t(ItemStack itemStack) {
      Set<Object2IntMap.Entry<RegistryEntry<Enchantment>>> enchants = EnchantmentHelper.getEnchantments(itemStack).getEnchantmentEntries();
      if (enchants.size() > 1) {
         return false;
      } else {
         for(Object2IntMap.Entry<RegistryEntry<Enchantment>> e : enchants) {
            RegistryEntry<Enchantment> enchantment = (RegistryEntry)e.getKey();
            int lvl = e.getIntValue();
            if (lvl == 0 && enchantment.getKey().isPresent() && enchantment.getKey().get() == Enchantments.PROTECTION) {
               return true;
            }
         }

         return false;
      }
   }
}
