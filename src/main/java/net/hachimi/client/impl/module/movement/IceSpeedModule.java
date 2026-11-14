package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.mixin.accessor.AccessorAbstractBlock;
import net.minecraft.block.Blocks;

public class IceSpeedModule extends ToggleModule {
   public IceSpeedModule() {
      super("IceSpeed", "Modifies the walking speed on ice", ModuleCategory.MOVEMENT);
   }

   public void onEnable() {
      if (mc.world != null) {
         ((AccessorAbstractBlock)(Object)Blocks.ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)(Object)Blocks.PACKED_ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)(Object)Blocks.BLUE_ICE).setSlipperiness(0.4F);
         ((AccessorAbstractBlock)(Object)Blocks.FROSTED_ICE).setSlipperiness(0.4F);
      }
   }

   public void onDisable() {
      if (mc.world != null) {
         ((AccessorAbstractBlock)(Object)Blocks.ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)(Object)Blocks.PACKED_ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)(Object)Blocks.BLUE_ICE).setSlipperiness(0.98F);
         ((AccessorAbstractBlock)(Object)Blocks.FROSTED_ICE).setSlipperiness(0.98F);
      }
   }
}
