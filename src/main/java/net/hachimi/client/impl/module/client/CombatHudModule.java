package net.hachimi.client.impl.module.client;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.gui.hud.RenderOverlayEvent;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class CombatHudModule extends ToggleModule {
   private static CombatHudModule INSTANCE;
   Config<Boolean> hideEmptyConfig = this.register(new BooleanConfig("HideEmpty", "Hides items that you don't have any of", true));
   Config<Boolean> crystalConfig = this.register(new BooleanConfig("Crystal", "Displays end crystal count", true));
   Config<Boolean> xpConfig = this.register(new BooleanConfig("XP", "Displays experience bottle count", true));
   Config<Boolean> pearlConfig = this.register(new BooleanConfig("Pearl", "Displays ender pearl count", true));
   Config<Boolean> obsidianConfig = this.register(new BooleanConfig("Obsidian", "Displays obsidian count", true));
   Config<Boolean> egappleConfig = this.register(new BooleanConfig("E-GApple", "Displays enchanted golden apple count", true));
   Config<Boolean> gappleConfig = this.register(new BooleanConfig("GApple", "Displays golden apple count", true));
   Config<Boolean> totemConfig = this.register(new BooleanConfig("Totem", "Displays totem of undying count", true));
   Config<Boolean> webConfig = this.register(new BooleanConfig("Web", "Displays cobweb count", true));
   Config<Boolean> anchorConfig = this.register(new BooleanConfig("Anchor", "Displays respawn anchor count", true));
   Config<Boolean> glowstoneConfig = this.register(new BooleanConfig("Glowstone", "Displays glowstone count", true));
   Config<Boolean> pistonConfig = this.register(new BooleanConfig("Piston", "Displays piston count", true));
   Config<Boolean> redstoneConfig = this.register(new BooleanConfig("RedStone", "Displays redstone block count", true));
   Config<Boolean> enderChestConfig = this.register(new BooleanConfig("EnderChest", "Displays ender chest count", true));
   Config<Boolean> fireworkConfig = this.register(new BooleanConfig("Firework", "Displays firework rocket count", true));
   Config<Integer> xConfig;
   Config<Integer> yConfig;
   Config<Integer> offsetConfig;
   private final ItemStack crystalStack;
   private final ItemStack xpStack;
   private final ItemStack pearlStack;
   private final ItemStack obsidianStack;
   private final ItemStack egappleStack;
   private final ItemStack gappleStack;
   private final ItemStack totemStack;
   private final ItemStack webStack;
   private final ItemStack anchorStack;
   private final ItemStack glowstoneStack;
   private final ItemStack pistonStack;
   private final ItemStack redstoneStack;
   private final ItemStack enderChestStack;
   private final ItemStack fireworkStack;
   private int x;
   private int y;

   public CombatHudModule() {
      super("CombatHud", "Displays a row of combat item counts on the HUD.", ModuleCategory.CLIENT);
      this.xConfig = this.register(new NumberConfig("X", "The x position of the item HUD", 0, 100, 1500, NumberDisplay.DEFAULT));
      this.yConfig = this.register(new NumberConfig("Y", "The y position of the item HUD", 0, 100, 1000, NumberDisplay.DEFAULT));
      this.offsetConfig = this.register(new NumberConfig("Offset", "The spacing between each item", 0, 18, 30, NumberDisplay.DEFAULT));
      this.crystalStack = new ItemStack(Items.END_CRYSTAL);
      this.xpStack = new ItemStack(Items.EXPERIENCE_BOTTLE);
      this.pearlStack = new ItemStack(Items.ENDER_PEARL);
      this.obsidianStack = new ItemStack(Items.OBSIDIAN);
      this.egappleStack = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
      this.gappleStack = new ItemStack(Items.GOLDEN_APPLE);
      this.totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
      this.webStack = new ItemStack(Items.COBWEB);
      this.anchorStack = new ItemStack(Items.RESPAWN_ANCHOR);
      this.glowstoneStack = new ItemStack(Items.GLOWSTONE);
      this.pistonStack = new ItemStack(Items.PISTON);
      this.redstoneStack = new ItemStack(Items.REDSTONE_BLOCK);
      this.enderChestStack = new ItemStack(Items.ENDER_CHEST);
      this.fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
      INSTANCE = this;
   }

   public static CombatHudModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onRenderOverlayPost(RenderOverlayEvent.Post event) {
      if (mc.player != null) {
         this.x = (Integer)this.xConfig.getValue() - (Integer)this.offsetConfig.getValue();
         this.y = (Integer)this.yConfig.getValue();
         if ((Boolean)this.crystalConfig.getValue()) {
            this.drawItem(event, this.crystalStack, Items.END_CRYSTAL);
         }

         if ((Boolean)this.xpConfig.getValue()) {
            this.drawItem(event, this.xpStack, Items.EXPERIENCE_BOTTLE);
         }

         if ((Boolean)this.pearlConfig.getValue()) {
            this.drawItem(event, this.pearlStack, Items.ENDER_PEARL);
         }

         if ((Boolean)this.obsidianConfig.getValue()) {
            this.drawItem(event, this.obsidianStack, Items.OBSIDIAN);
         }

         if ((Boolean)this.egappleConfig.getValue()) {
            this.drawItem(event, this.egappleStack, Items.ENCHANTED_GOLDEN_APPLE);
         }

         if ((Boolean)this.gappleConfig.getValue()) {
            this.drawItem(event, this.gappleStack, Items.GOLDEN_APPLE);
         }

         if ((Boolean)this.totemConfig.getValue()) {
            this.drawItem(event, this.totemStack, Items.TOTEM_OF_UNDYING);
         }

         if ((Boolean)this.webConfig.getValue()) {
            this.drawItem(event, this.webStack, Items.COBWEB);
         }

         if ((Boolean)this.anchorConfig.getValue()) {
            this.drawItem(event, this.anchorStack, Items.RESPAWN_ANCHOR);
         }

         if ((Boolean)this.glowstoneConfig.getValue()) {
            this.drawItem(event, this.glowstoneStack, Items.GLOWSTONE);
         }

         if ((Boolean)this.pistonConfig.getValue()) {
            this.drawItem(event, this.pistonStack, Items.PISTON);
         }

         if ((Boolean)this.redstoneConfig.getValue()) {
            this.drawItem(event, this.redstoneStack, Items.REDSTONE_BLOCK);
         }

         if ((Boolean)this.enderChestConfig.getValue()) {
            this.drawItem(event, this.enderChestStack, Items.ENDER_CHEST);
         }

         if ((Boolean)this.fireworkConfig.getValue()) {
            this.drawItem(event, this.fireworkStack, Items.FIREWORK_ROCKET);
         }

      }
   }

   private void drawItem(RenderOverlayEvent.Post event, ItemStack stack, Item item) {
      int count = InventoryUtil.count(item);
      if (!(Boolean)this.hideEmptyConfig.getValue() || count != 0) {
         this.x += (Integer)this.offsetConfig.getValue();
         stack.setCount(Math.max(count, 1));
         event.getContext().drawItem(stack, this.x, this.y);
         event.getContext().drawItemInSlot(mc.textRenderer, stack, this.x, this.y);
      }
   }
}
