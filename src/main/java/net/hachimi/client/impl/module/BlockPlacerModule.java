package net.hachimi.client.impl.module;

import java.util.function.Predicate;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;

public class BlockPlacerModule extends CombatModule {
   protected Config<Boolean> strictDirectionConfig = this.register(new BooleanConfig("StrictDirection", "Places on visible sides only", false));
   protected Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to block before placing", false));
   protected Config<Boolean> stopMotionConfig = this.register(new BooleanConfig("StopMotion", "Stops movement before placing", false));

   public BlockPlacerModule(String name, String desc, ModuleCategory category) {
      super(name, desc, category);
      this.register(new Config[]{this.strictDirectionConfig, this.rotateConfig, this.stopMotionConfig});
   }

   public BlockPlacerModule(String name, String desc, ModuleCategory category, int rotationPriority) {
      super(name, desc, category, rotationPriority);
      this.register(new Config[]{this.strictDirectionConfig, this.rotateConfig, this.stopMotionConfig});
   }

   protected int getSlot(Predicate<ItemStack> filter) {
      for(int i = 0; i < 9; ++i) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (!itemStack.isEmpty() && filter.test(itemStack)) {
            return i;
         }
      }

      return -1;
   }

   protected int getBlockItemSlot(Block block) {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         Item var5 = stack.getItem();
         if (var5 instanceof BlockItem blockItem) {
            if (blockItem.getBlock() == block) {
               return i;
            }
         }
      }

      return -1;
   }

   public static record BlockSlot(Block block, int slot) {
      public boolean equals(Object obj) {
         if (obj instanceof BlockSlot other) {
            return other.block() == this.block;
         } else {
            return false;
         }
      }
   }
}
