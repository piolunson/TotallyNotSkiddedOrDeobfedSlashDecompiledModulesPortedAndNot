package net.hachimi.client.impl.module.misc;

import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.ItemListConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.config.setting.StringConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.component.DataComponentTypes;

public final class AutoAnvilRenameModule extends ToggleModule {
   Config<Selection> autoRenameSelectionConfig;
   Config<List<Item>> autoRenameWhitelistConfig;
   Config<List<Item>> autoRenameBlacklistConfig;
   Config<String> autoRenameTextConfig;
   Config<Integer> autoRenameDelayConfig;
   Config<Boolean> debugConfig;
   private final CacheTimer delayTimer;

   public AutoAnvilRenameModule() {
      super("AutoAnvilRename", "Automatically renames items in anvils.", ModuleCategory.MISCELLANEOUS);
      this.autoRenameSelectionConfig = this.register(new EnumConfig("Selection", "The selection of items to rename", AutoAnvilRenameModule.Selection.ALL, AutoAnvilRenameModule.Selection.values()));
      this.autoRenameWhitelistConfig = this.register(new ItemListConfig("Whitelist", "The items to rename.", new Item[]{Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.BLACK_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX, Items.BLUE_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.PINK_SHULKER_BOX}));
      this.autoRenameBlacklistConfig = this.register(new ItemListConfig("Blacklist", "The items to not rename.", new Item[]{Items.EXPERIENCE_BOTTLE}));
      this.autoRenameTextConfig = this.register(new StringConfig("Text", "The text to rename the items to.", "HachimiClient.net"));
      this.autoRenameDelayConfig = this.register(new NumberConfig("Delay", "The delay between renaming items.", 0, 10, 20));
      this.debugConfig = this.register(new BooleanConfig("Debug", "Prints debug information to chat.", false, () -> false));
      this.delayTimer = new CacheTimer();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         Screen var3 = mc.currentScreen;
         if (var3 instanceof AnvilScreen) {
            AnvilScreen anvilScreen = (AnvilScreen)var3;
            if (!this.delayTimer.passed((Number)this.autoRenameDelayConfig.getValue())) {
               return;
            }

            if (mc.player.experienceLevel <= 0 && !mc.player.isCreative()) {
               if ((Boolean)this.debugConfig.getValue()) {
                  this.sendModuleMessage("Not enough experience levels!");
               }

               return;
            }

            AnvilScreenHandler screenHandler = (AnvilScreenHandler)anvilScreen.getScreenHandler();
            if (!screenHandler.getSlot(1).getStack().isEmpty()) {
               this.moveToEmptySlot(screenHandler, 1);
               return;
            }

            if (!screenHandler.getSlot(0).getStack().isEmpty()) {
               this.moveToEmptySlot(screenHandler, 0);
               return;
            }

            if (!screenHandler.getSlot(2).getStack().isEmpty()) {
               this.moveToEmptySlot(screenHandler, 2);
               return;
            }

            for(int i = 3; i < 39; ++i) {
               ItemStack itemStack = screenHandler.getSlot(i).getStack();
               if (!itemStack.isEmpty() && !this.equalsName(itemStack, this.autoRenameTextConfig.getName()) && (this.autoRenameSelectionConfig.getValue() != AutoAnvilRenameModule.Selection.BLACKLIST || !((List)this.autoRenameBlacklistConfig.getValue()).contains(itemStack.getItem())) && (this.autoRenameSelectionConfig.getValue() != AutoAnvilRenameModule.Selection.WHITELIST || ((List)this.autoRenameWhitelistConfig.getValue()).contains(itemStack.getItem()))) {
                  String name = !((String)this.autoRenameTextConfig.getValue()).trim().isEmpty() ? (String)this.autoRenameTextConfig.getValue() : "";
                  mc.interactionManager.clickSlot(screenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(screenHandler.syncId, screenHandler.getSlot(0).id, 0, SlotActionType.PICKUP, mc.player);
                  ((AnvilScreen)mc.currentScreen).nameField.setText(name);
                  if ((Boolean)this.debugConfig.getValue()) {
                     this.sendModuleMessage("Successfully renamed item in slot: " + i + ".");
                  }

                  mc.interactionManager.clickSlot(screenHandler.syncId, screenHandler.getSlot(2).id, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(screenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                  break;
               }
            }

            this.delayTimer.reset();
            return;
         }
      }

   }

   private void moveToEmptySlot(AnvilScreenHandler screenHandler, int slot) {
      if (mc.interactionManager != null) {
         for(int i = 3; i < 39; ++i) {
            ItemStack itemStack = screenHandler.getSlot(i).getStack();
            if (itemStack.isEmpty()) {
               mc.interactionManager.clickSlot(screenHandler.syncId, screenHandler.getSlot(slot).id, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(screenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
               return;
            }
         }

         mc.interactionManager.clickSlot(screenHandler.syncId, screenHandler.getSlot(slot).id, 0, SlotActionType.THROW, mc.player);
      }
   }

   private boolean equalsName(ItemStack itemStack, String itemName) {
      if (itemName.trim().isEmpty()) {
         return itemStack.get(DataComponentTypes.CUSTOM_NAME) == null;
      } else {
         return itemStack.getName().getString().equals(itemName);
      }
   }

   private static enum Selection {
      ALL,
      WHITELIST,
      BLACKLIST;

      // $FF: synthetic method
      private static Selection[] $values() {
         return new Selection[]{ALL, WHITELIST, BLACKLIST};
      }
   }
}
