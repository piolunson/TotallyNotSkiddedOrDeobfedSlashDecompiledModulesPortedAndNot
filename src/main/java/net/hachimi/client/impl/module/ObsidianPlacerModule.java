package net.hachimi.client.impl.module;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.hachimi.client.api.module.ModuleCategory;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public class ObsidianPlacerModule extends BlockPlacerModule {
   protected static final BlockState DEFAULT_OBSIDIAN_STATE;
   private static final List<Block> RESISTANT_BLOCKS;

   public ObsidianPlacerModule(String name, String desc, ModuleCategory category) {
      super(name, desc, category);
   }

   public ObsidianPlacerModule(String name, String desc, ModuleCategory category, int rotationPriority) {
      super(name, desc, category, rotationPriority);
   }

   protected BlockSlot getResistantBlockItem() {
      Set<BlockSlot> blockSlots = new HashSet<>();

      for(Block type : RESISTANT_BLOCKS) {
         int slot = this.getBlockItemSlot(type);
         if (slot != -1) {
            blockSlots.add(new BlockSlot(type, slot));
         }
      }

      BlockSlot slot = (BlockSlot)blockSlots.stream().filter((b) -> b.block() == Blocks.OBSIDIAN).findFirst().orElse(null);
      if (slot != null) {
         return slot;
      } else {
         BlockSlot slot1 = (BlockSlot)blockSlots.stream().filter((b) -> b.block() == Blocks.CRYING_OBSIDIAN).findFirst().orElse(null);
         if (slot1 != null) {
            return slot1;
         } else {
            BlockSlot slot2 = (BlockSlot)blockSlots.stream().filter((b) -> b.block() == Blocks.ENDER_CHEST).findFirst().orElse(null);
            if (slot2 != null) {
               return slot2;
            } else {
               return null;
            }
         }
      }
   }

   static {
      DEFAULT_OBSIDIAN_STATE = Blocks.OBSIDIAN.getDefaultState();
      RESISTANT_BLOCKS = new LinkedList<Block>() {
         {
            this.add(Blocks.OBSIDIAN);
            this.add(Blocks.CRYING_OBSIDIAN);
            this.add(Blocks.ENDER_CHEST);
         }
      };
   }

   public static record BlockSlot(Block block, int slot) {
      public boolean equals(Object obj) {
         boolean var10000;
         if (obj instanceof BlockSlot b) {
            if (b.block() == this.block) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   }
}
