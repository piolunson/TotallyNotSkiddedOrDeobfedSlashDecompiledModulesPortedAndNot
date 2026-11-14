package net.hachimi.client.impl.module.combat;

import com.google.common.collect.Lists;
import java.util.LinkedHashSet;
import java.util.Set;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.world.LoadWorldEvent;
import net.hachimi.client.impl.module.exploit.ChorusInvincibilityModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.client.util.world.SneakBlocks;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.hit.BlockHitResult;

public final class AutoTotemModule extends ToggleModule {
   private static AutoTotemModule INSTANCE;
   Config<OffhandItem> itemConfig;
   Config<Float> healthConfig;
   Config<Boolean> gappleConfig;
   Config<Boolean> crappleConfig;
   Config<Boolean> lethalConfig;
   Config<Boolean> fastConfig;
   Config<Boolean> mainhandTotemConfig;
   Config<Integer> totemSlotConfig;
   Config<Boolean> alternativeConfig;
   Config<Boolean> debugConfig;
   private int lastHotbarSlot;
   private int lastTotemCount;
   private Item lastHotbarItem;
   private Item offhandItem;
   private boolean replacing;
   private long replaceTime;
   private final Timer mainhandSwapTimer;
   private boolean totemInMainhand;

   public AutoTotemModule() {
      super("AutoTotem", "Automatically replenishes the totem in your offhand", ModuleCategory.COMBAT);
      this.itemConfig = this.register(new EnumConfig("Item", "The item to wield in your offhand", AutoTotemModule.OffhandItem.TOTEM, AutoTotemModule.OffhandItem.values()));
      this.healthConfig = this.register(new NumberConfig("Health", "The health required to fall below before swapping to a totem", 0.0F, 14.0F, 20.0F));
      this.gappleConfig = this.register(new BooleanConfig("OffhandGapple", "Equips a golden apple if holding down the item use button", true));
      this.crappleConfig = this.register(new BooleanConfig("Crapple", "Uses a normal golden apple if Absorption is present", true));
      this.lethalConfig = this.register(new BooleanConfig("Lethal", "Calculates lethal damage sources", false, () -> this.itemConfig.getValue() != AutoTotemModule.OffhandItem.TOTEM));
      this.fastConfig = this.register(new BooleanConfig("FastSwap", "Swaps items to offhand", true));
      this.mainhandTotemConfig = this.register(new BooleanConfig("MainhandTotem", "Swaps to a totem in your mainhand", false));
      this.totemSlotConfig = this.register(new NumberConfig("TotemSlot", "Slot to use for mainhand totem", 1, 1, 9, () -> this.mainhandTotemConfig.getValue()));
      this.alternativeConfig = this.register(new BooleanConfig("Alternative", "Replaces totem using the swap packet", false));
      this.debugConfig = this.register(new BooleanConfig("Debug", "Debug on death", false));
      this.mainhandSwapTimer = new CacheTimer();
      INSTANCE = this;
   }

   public static AutoTotemModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      return String.valueOf(InventoryUtil.count(Items.TOTEM_OF_UNDYING));
   }

   public void onDisable() {
      super.onDisable();
      this.lastHotbarSlot = -1;
      this.lastHotbarItem = null;
      this.offhandItem = null;
      this.totemInMainhand = false;
   }

   @EventListener
   public void onLoadWorld(LoadWorldEvent event) {
      this.lastTotemCount = InventoryUtil.count(Items.TOTEM_OF_UNDYING);
   }

   @EventListener(
      priority = 2147483646
   )
   public void onTick(TickEvent event) {
      if (mc.player != null && event.getStage() == StageEvent.EventStage.PRE) {
         if ((Boolean)this.mainhandTotemConfig.getValue() && this.mainhandSwapTimer.passed(200)) {
            int totemSlot1 = (Integer)this.totemSlotConfig.getValue() - 1;
            ItemStack totemSlotStack = mc.player.getInventory().getStack(totemSlot1);
            totemSlot1 += 36;
            if (totemSlotStack.getItem() != Items.TOTEM_OF_UNDYING) {
               for(int n = 35; n >= 0; --n) {
                  if (mc.player.getInventory().getStack(n).getItem() == Items.TOTEM_OF_UNDYING) {
                     int slot = n < 9 ? n + 36 : n;
                     this.replacing = true;
                     if ((Boolean)this.alternativeConfig.getValue()) {
                        mc.interactionManager.clickSlot(0, slot, totemSlot1, SlotActionType.SWAP, mc.player);
                        this.replacing = false;
                     } else {
                        if (mc.player.currentScreenHandler.getCursorStack().getItem() != Items.TOTEM_OF_UNDYING) {
                           mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                        }

                        if (mc.player.currentScreenHandler.getCursorStack().getItem() == Items.TOTEM_OF_UNDYING) {
                           mc.interactionManager.clickSlot(0, totemSlot1, 0, SlotActionType.PICKUP, mc.player);
                           this.lastTotemCount = InventoryUtil.count(Items.TOTEM_OF_UNDYING) - 1;
                        }

                        this.replacing = false;
                        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty() && mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                           mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                           return;
                        }
                     }
                  }
               }
            }

            this.totemInMainhand = this.checkMainhandTotem();
            if (this.totemInMainhand) {
               int totemSlot = -1;

               for(int i = 0; i < 9; ++i) {
                  ItemStack stack = mc.player.getInventory().getStack(i);
                  if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                     totemSlot = i;
                     break;
                  }
               }

               if (totemSlot != -1) {
                  Managers.INVENTORY.setClientSlot(totemSlot);
               }
            }
         } else {
            this.totemInMainhand = false;
         }

         this.offhandItem = ((OffhandItem)this.itemConfig.getValue()).getItem();
         if (this.checkLethal()) {
            this.offhandItem = Items.TOTEM_OF_UNDYING;
         } else {
            Item mainHandItem = mc.player.getMainHandStack().getItem();
            if ((Boolean)this.gappleConfig.getValue() && mc.options.useKey.isPressed() && (mainHandItem instanceof SwordItem || mainHandItem instanceof TridentItem || mainHandItem instanceof AxeItem) && PlayerUtil.getLocalPlayerHealth() >= (Float)this.healthConfig.getValue()) {
               HitResult var14 = mc.crosshairTarget;
               if (var14 instanceof BlockHitResult) {
                  BlockHitResult result = (BlockHitResult)var14;
                  BlockState interactBlock = mc.world.getBlockState(result.getBlockPos());
                  if (!SneakBlocks.isSneakBlock(interactBlock)) {
                     this.offhandItem = this.getGoldenAppleType();
                  }
               } else {
                  this.offhandItem = this.getGoldenAppleType();
               }
            }
         }

         if (mc.player.getOffHandStack().getItem() != this.offhandItem) {
            int n = 35;
            if (this.lastHotbarSlot != -1 && this.lastHotbarItem != null) {
               ItemStack stack = mc.player.getInventory().getStack(this.lastHotbarSlot);
               if (stack.getItem().equals(this.offhandItem) && this.lastHotbarItem.equals(mc.player.getOffHandStack().getItem())) {
                  int tmp = this.lastHotbarSlot;
                  this.lastHotbarSlot = -1;
                  this.lastHotbarItem = null;
                  n = tmp;
               }
            }

            for(; n >= 0; --n) {
               if (mc.player.getInventory().getStack(n).getItem() == this.offhandItem) {
                  if (n < 9) {
                     this.lastHotbarItem = this.offhandItem;
                     this.lastHotbarSlot = n;
                  }

                  int slot = n < 9 ? n + 36 : n;
                  this.replacing = true;
                  if ((Boolean)this.alternativeConfig.getValue()) {
                     mc.interactionManager.clickSlot(0, slot, 40, SlotActionType.SWAP, mc.player);
                     this.replacing = false;
                  } else {
                     if (mc.player.currentScreenHandler.getCursorStack().getItem() != this.offhandItem) {
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                     }

                     if (mc.player.currentScreenHandler.getCursorStack().getItem() == this.offhandItem) {
                        mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.PICKUP, mc.player);
                        this.lastTotemCount = InventoryUtil.count(Items.TOTEM_OF_UNDYING) - 1;
                     }

                     this.replacing = false;
                     if (!mc.player.currentScreenHandler.getCursorStack().isEmpty() && mc.player.getOffHandStack().getItem() == this.offhandItem) {
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                        return;
                     }
                  }
               }
            }

         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         Packet failureReasonsSet = event.getPacket();
         if (failureReasonsSet instanceof HealthUpdateS2CPacket) {
            HealthUpdateS2CPacket packet = (HealthUpdateS2CPacket)failureReasonsSet;
            if (packet.getHealth() <= 0.0F && (Boolean)this.debugConfig.getValue()) {
               if (this.lastTotemCount <= 0) {
                  return;
               }

               Set<String> failureReasons = this.getFailureReasons();
               if (failureReasons.isEmpty()) {
                  long serverLatency = System.currentTimeMillis() - this.replaceTime;
                  this.sendModuleError("Failed to replace totem in %sms!", new Object[]{serverLatency});
               } else {
                  this.sendModuleError("Failed to replace totem! Possible reasons: %s", new Object[]{String.join(", ", failureReasons)});
               }
            }
         }

         failureReasonsSet = event.getPacket();
         if (failureReasonsSet instanceof ScreenHandlerSlotUpdateS2CPacket) {
            ScreenHandlerSlotUpdateS2CPacket packet = (ScreenHandlerSlotUpdateS2CPacket)failureReasonsSet;
            if (packet.getSlot() == 45 && this.offhandItem == Items.TOTEM_OF_UNDYING) {
               if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING || !packet.getStack().isEmpty()) {
                  return;
               }

               this.replaceTime = System.currentTimeMillis();
            }
         }

      }
   }

   private Set<String> getFailureReasons() {
      Set<String> failureReasonsSet = new LinkedHashSet<>();
      if (mc.player.currentScreenHandler.syncId != 0) {
         failureReasonsSet.add("Current screen handler is not the player inventory");
      }

      if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         failureReasonsSet.add("Totem was not placed in offhand on time");
      }

      return failureReasonsSet;
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getConfig() == this.totemSlotConfig) {
         this.mainhandSwapTimer.reset();
      }

   }

   private boolean checkLethal() {
      float health = PlayerUtil.getLocalPlayerHealth();
      return health <= (Float)this.healthConfig.getValue() || (Boolean)this.lethalConfig.getValue() && this.checkLethalCrystal(health) || (float)PlayerUtil.computeFallDamage(mc.player.fallDistance, 1.0F) + 0.5F > mc.player.getHealth();
   }

   private boolean checkLethalCrystal(float health) {
      for(Entity e : Lists.newArrayList(mc.world.getEntities())) {
         if (e != null && e.isAlive() && e instanceof EndCrystalEntity crystal) {
            if (!(mc.player.squaredDistanceTo(e) > (double)144.0F)) {
               double potential = ExplosionUtil.getDamageTo(mc.player, crystal.getPos(), false);
               if (!((double)health + (double)0.5F > potential)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private Item getGoldenAppleType() {
      return !(Boolean)this.crappleConfig.getValue() || !InventoryUtil.hasItemInInventory(Items.GOLDEN_APPLE, true) || !mc.player.hasStatusEffect(StatusEffects.ABSORPTION) && InventoryUtil.hasItemInInventory(Items.ENCHANTED_GOLDEN_APPLE, true) ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE;
   }

   private boolean checkMainhandTotem() {
      return mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING && !ChorusInvincibilityModule.getInstance().isUsingChorus() ? this.checkLethalCrystal(PlayerUtil.getLocalPlayerHealth()) : false;
   }

   public boolean isTotemInMainhand() {
      return this.totemInMainhand;
   }

   public boolean isReplacing() {
      return this.replacing;
   }

   private static enum OffhandItem {
      TOTEM(Items.TOTEM_OF_UNDYING),
      GAPPLE(Items.ENCHANTED_GOLDEN_APPLE),
      CRYSTAL(Items.END_CRYSTAL);

      private final Item item;

      private OffhandItem(Item item) {
         this.item = item;
      }

      public Item getItem() {
         return this.item;
      }

      // $FF: synthetic method
      private static OffhandItem[] $values() {
         return new OffhandItem[]{TOTEM, GAPPLE, CRYSTAL};
      }
   }
}
