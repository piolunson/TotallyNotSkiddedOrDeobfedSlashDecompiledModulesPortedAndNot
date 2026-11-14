package net.hachimi.client.impl.module.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ItemListConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.file.ConfigFile;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.module.misc.KitModule;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class InventoryManagerModule extends ToggleModule {
   private static InventoryManagerModule INSTANCE;
   private static final int HOTBAR_START = 0;
   private static final int INVENTORY_START = 9;
   private static final int INVENTORY_END = 35;
   Config<Boolean> stackConfig = this.register(new BooleanConfig("Stack", "Combines partial stacks of the same item into full stacks", true));
   Config<Boolean> kitModeConfig = this.register(new BooleanConfig("KitMode", "Throws any item not part of the currently loaded Kit (see Kit module) out of the inventory", true));
   Config<Boolean> sortConfig = this.register(new BooleanConfig("Sort", "Groups items of the same type next to each other", true));
   Config<Integer> sortDelayConfig = this.register(new NumberConfig("SortDelay", "The delay in ticks between sort actions", 0, 0, 20));
   Config<Boolean> cleanerConfig = this.register(new BooleanConfig("Cleaner", "Automatically throws blacklisted items out of the inventory", true));
   Config<Boolean> renameConfig = this.register(new BooleanConfig("Rename", "Reserved for future use", false));
   Config<Boolean> hotbarConfig = this.register(new BooleanConfig("Hotbar", "Includes the hotbar slots when cleaning/sorting/stacking", true));
   Config<Integer> cleanerDelayConfig = this.register(new NumberConfig("CleanerDelay", "The delay in ticks between cleaner actions", 0, 0, 20));
   Config<List<Item>> blacklistConfig = this.register(new ItemListConfig("Blacklist", "The items to throw out of the inventory", new Item[0]));
   Config<List<Item>> whitelistConfig = this.register(new ItemListConfig("WhiteList", "Items the Cleaner will never throw, even if also blacklisted", new Item[0]));
   private final Timer cleanerTimer = new CacheTimer();
   private final Timer sortTimer = new CacheTimer();

   public InventoryManagerModule() {
      super("InventoryManager", "Cleans, sorts, and stacks the player inventory", ModuleCategory.PLAYER);
   }

   public static InventoryManagerModule getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new InventoryManagerModule();
      }

      return INSTANCE;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (mc.player != null && mc.player.getInventory() != null) {
            int lowerBound = (Boolean)this.hotbarConfig.getValue() ? 0 : 9;
            if (((Boolean)this.cleanerConfig.getValue() || (Boolean)this.kitModeConfig.getValue()) && this.cleanerTimer.passed((Integer)this.cleanerDelayConfig.getValue() * 50) && this.cleanInventory(lowerBound)) {
               this.cleanerTimer.reset();
            } else {
               if (((Boolean)this.stackConfig.getValue() || (Boolean)this.sortConfig.getValue()) && this.sortTimer.passed((Integer)this.sortDelayConfig.getValue() * 50)) {
                  if ((Boolean)this.stackConfig.getValue() && this.stackInventory(lowerBound)) {
                     this.sortTimer.reset();
                     return;
                  }

                  if ((Boolean)this.sortConfig.getValue() && this.sortInventory(lowerBound)) {
                     this.sortTimer.reset();
                  }
               }

            }
         }
      }
   }

   private boolean cleanInventory(int lowerBound) {
      for(int i = 35; i >= lowerBound; --i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty()) {
            boolean whitelisted = ((List)this.whitelistConfig.getValue()).contains(stack.getItem());
            boolean blacklisted = (Boolean)this.cleanerConfig.getValue() && !whitelisted && ((List)this.blacklistConfig.getValue()).contains(stack.getItem());
            boolean notInKit = (Boolean)this.kitModeConfig.getValue() && !whitelisted && KitModule.getInstance() != null && !KitModule.getInstance().isPartOfKit(stack.getItem());
            if (blacklisted || notInKit) {
               this.throwSlot(i);
               return true;
            }
         }
      }

      return false;
   }

   private boolean stackInventory(int lowerBound) {
      for(int i = lowerBound; i <= 35; ++i) {
         ItemStack from = mc.player.getInventory().getStack(i);
         if (!from.isEmpty() && from.getCount() < from.getMaxCount()) {
            for(int j = lowerBound; j <= 35; ++j) {
               if (i != j) {
                  ItemStack into = mc.player.getInventory().getStack(j);
                  if (!into.isEmpty() && into.getCount() < into.getMaxCount() && ItemStack.areEqual(from, into)) {
                     this.clickSlot(j);
                     this.clickSlot(i);
                     this.clickSlot(j);
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private boolean sortInventory(int lowerBound) {
      for(int i = lowerBound; i < 35; ++i) {
         ItemStack current = mc.player.getInventory().getStack(i);

         for(int j = i + 1; j <= 35; ++j) {
            ItemStack next = mc.player.getInventory().getStack(j);
            if (!next.isEmpty()) {
               boolean shouldSwap = current.isEmpty() || current.getItem() != next.getItem() && this.shouldComeBefore(next, current, i, j, lowerBound);
               if (shouldSwap) {
                  this.swapSlots(i, j);
                  return true;
               }
               break;
            }
         }
      }

      return false;
   }

   private boolean shouldComeBefore(ItemStack next, ItemStack current, int i, int j, int lowerBound) {
      if (current.isEmpty()) {
         return true;
      } else {
         for(int k = lowerBound; k < i; ++k) {
            ItemStack stack = mc.player.getInventory().getStack(k);
            if (!stack.isEmpty() && stack.getItem() == next.getItem()) {
               return false;
            }
         }

         Identifier currentId = Registries.ITEM.getId(current.getItem());
         Identifier nextId = Registries.ITEM.getId(next.getItem());
         return nextId.toString().compareTo(currentId.toString()) < 0;
      }
   }

   private void throwSlot(int slot) {
      this.clickSlot(slot);
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, -999, 0, SlotActionType.PICKUP, mc.player);
   }

   private void clickSlot(int slot) {
      mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
   }

   private void swapSlots(int slotA, int slotB) {
      this.clickSlot(slotA);
      this.clickSlot(slotB);
      this.clickSlot(slotA);
   }

   public ConfigFile getBlacklistFile(Path clientDir) {
      return new InventoryManagerFile(clientDir);
   }

   public class InventoryManagerFile extends ConfigFile {
      public InventoryManagerFile(Path clientDir) {
         super(clientDir, "inventory-manager");
      }

      public void save() {
         try {
            Path filepath = this.getFilepath();
            if (!Files.exists(filepath, new LinkOption[0])) {
               Files.createFile(filepath);
            }

            JsonObject json = new JsonObject();
            json.add("blacklist", this.itemListToJson(InventoryManagerModule.this.blacklistConfig.getValue()));
            json.add("whitelist", this.itemListToJson(InventoryManagerModule.this.whitelistConfig.getValue()));
            this.write(filepath, this.serialize(json));
         } catch (IOException e) {
            Hachimi.error("Could not save file for inventory manager!");
            e.printStackTrace();
         }

      }

      public void load() {
         try {
            Path filepath = this.getFilepath();
            if (Files.exists(filepath, new LinkOption[0])) {
               String content = this.read(filepath);
               JsonObject object = this.parseObject(content);
               if (object == null) {
                  return;
               }

               if (object.has("blacklist")) {
                  this.loadItemList(object.getAsJsonArray("blacklist"), InventoryManagerModule.this.blacklistConfig.getValue());
               }

               if (object.has("whitelist")) {
                  this.loadItemList(object.getAsJsonArray("whitelist"), InventoryManagerModule.this.whitelistConfig.getValue());
               }
            }
         } catch (IOException e) {
            Hachimi.error("Could not read file for inventory manager!");
            e.printStackTrace();
         }

      }

      private JsonArray itemListToJson(List<Item> items) {
         JsonArray array = new JsonArray();

         for(Item item : items) {
            Identifier id = Registries.ITEM.getId(item);
            array.add(id.toString());
         }

         return array;
      }

      private void loadItemList(JsonArray jsonArray, List<Item> target) {
         target.clear();

         for(JsonElement element : jsonArray) {
            Item item = (Item)Registries.ITEM.get(Identifier.of(element.getAsString()));
            if (item != null && item != Items.AIR) {
               target.add(item);
            }
         }

      }
   }
}
