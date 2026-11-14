package net.hachimi.client.impl.module.movement;

import java.util.TimerTask;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.entity.ElytraUpdateEvent;
import net.hachimi.client.impl.event.entity.player.TravelEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.RotationModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.chat.ChatUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.player.SnapRotation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class FakeFlyModule extends RotationModule {
   private static FakeFlyModule INSTANCE;
   private final Config<Mode> mode;
   private final Config<FireWorkMode> fireworkMode;
   private final Config<Integer> packetDelay;
   private final Config<Double> fireworkDelay;
   private final Config<Boolean> unbreaking;
   private final Config<Double> fakeDelay;
   private final Config<Boolean> stand;
   private final Config<Boolean> pressSneak;
   private final Config<Boolean> releaseSneak;
   private final Config<Integer> releaseDelay;
   private final Config<Boolean> control;
   private final Config<Double> fallSpeed;
   private final Config<Boolean> inventorySwap;
   private final Config<Boolean> checkFirework;
   private final Config<Boolean> rotate;
   private final Config<Boolean> deBug;
   private final Timer fireworkTimer;
   private final Timer fireworkCooldown;
   private final Timer swapTimer;
   private int packetDelayCounter;
   private boolean fallFlying;
   private float moduleYaw;
   private float modulePitch;

   public FakeFlyModule() {
      super("FakeFly", "Elytra fake fly using fireworks — ported from LeavesHack", ModuleCategory.MOVEMENT);
      this.mode = this.register(new EnumConfig("Mode", "Flight mode (Legit / GrimDurability)", FakeFlyModule.Mode.GrimDurability, FakeFlyModule.Mode.values()));
      this.fireworkMode = this.register(new EnumConfig("FireWorkMode", "Firework boosting mode", FakeFlyModule.FireWorkMode.Auto, FakeFlyModule.FireWorkMode.values()));
      this.packetDelay = this.register(new NumberConfig("PacketDelay", "Tick delay between each elytra swap (GrimDurability)", 0, 3, 20, () -> this.mode.getValue() == FakeFlyModule.Mode.GrimDurability));
      this.fireworkDelay = this.register(new NumberConfig("FireworkDelay", "Delay (ms) between each firework shot (Delay mode)", (double)100.0F, (double)1000.0F, (double)3000.0F, () -> this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Delay));
      this.unbreaking = this.register(new BooleanConfig("Unbreaking", "Fake swaps elytra to avoid durability loss (Legit only)", true, () -> this.mode.getValue() == FakeFlyModule.Mode.Legit));
      this.fakeDelay = this.register(new NumberConfig("FakeDelay", "Interval (ms) between each unbreaking swap", (double)100.0F, (double)800.0F, (double)3000.0F, () -> this.mode.getValue() == FakeFlyModule.Mode.Legit && (Boolean)this.unbreaking.getValue()));
      this.stand = this.register(new BooleanConfig("Stand", "Cancels the flying animation (stand still pose)", true));
      this.pressSneak = this.register(new BooleanConfig("PressSneak", "Sends PRESS shift when the module is disabled", true));
      this.releaseSneak = this.register(new BooleanConfig("ReleaseSneak", "Sends RELEASE shift after ReleaseDelay when the module is disabled", true));
      this.releaseDelay = this.register(new NumberConfig("ReleaseDelay", "Delay (ms) before sending RELEASE shift", 0, 100, 1000, () -> this.releaseSneak.getValue()));
      this.control = this.register(new BooleanConfig("Control", "Controls velocity while flying (cancels drift when standing still)", true));
      this.fallSpeed = this.register(new NumberConfig("FallSpeed", "Minimum fall speed while standing still", (double)0.0F, 0.02, (double)3.0F, () -> this.control.getValue()));
      this.inventorySwap = this.register(new BooleanConfig("InventorySwap", "Also search the full inventory (not just the hotbar) for fireworks", true));
      this.checkFirework = this.register(new BooleanConfig("CheckFirework", "Don't fire a new firework while one is already boosting", true));
      this.rotate = this.register(new BooleanConfig("Rotate", "Sends a yaw/pitch packet following movement direction", true));
      this.deBug = this.register(new BooleanConfig("DeBug", "Prints debug info to chat (for development use)", false));
      this.fireworkTimer = new CacheTimer();
      this.fireworkCooldown = new CacheTimer();
      this.swapTimer = new CacheTimer();
      this.packetDelayCounter = 0;
      this.fallFlying = false;
      this.moduleYaw = 0.0F;
      this.modulePitch = 0.0F;
      INSTANCE = this;
   }

   public static FakeFlyModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      if (mc.player == null) {
         return null;
      } else {
         int count = this.countFireworks();
         return "F:" + count;
      }
   }

   public void onEnable() {
      this.fireworkTimer.setElapsedTime(-255L);
      this.swapTimer.setElapsedTime(-255L);
      this.packetDelayCounter = 0;
   }

   public void onDisable() {
      if (mc.player != null) {
         if ((Boolean)this.pressSneak.getValue()) {
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
         }

         if ((Boolean)this.releaseSneak.getValue()) {
            long delayMs = (long)(Integer)this.releaseDelay.getValue();
            (new java.util.Timer()).schedule(new TimerTask() {
               public void run() {
                  Globals.mc.execute(() -> {
                     if (Globals.mc.player != null) {
                        Globals.mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(Globals.mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                     }

                  });
               }
            }, delayMs);
         }

      }
   }

   @EventListener
   public void onElytraUpdate(ElytraUpdateEvent event) {
      if ((Boolean)this.stand.getValue()) {
         event.cancel();
      }

   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen != null && (Boolean)this.deBug.getValue()) {
            String var10000 = String.valueOf(mc.currentScreen.getTitle());
            ChatUtil.clientSendMessage("screen " + var10000 + " " + mc.currentScreen.getClass().getSimpleName() + " " + mc.currentScreen.getClass().getSuperclass().getSimpleName() + " " + String.valueOf(mc.currentScreen.getTitle()));
         }

         if (!(mc.currentScreen instanceof HandledScreen) || mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof CreativeInventoryScreen) {
            ++this.packetDelayCounter;
            this.moduleYaw = this.getSprintYaw(mc.player.getYaw());
            this.modulePitch = this.getControlPitch();
            if ((Boolean)this.deBug.getValue()) {
               ChatUtil.clientSendMessage("Yaw: " + this.moduleYaw + " Pitch: " + this.modulePitch);
            }

            if ((Boolean)this.rotate.getValue() && this.mode.getValue() == FakeFlyModule.Mode.GrimDurability) {
               float yaw = this.getSprintYaw(mc.player.getYaw());
               float pitch = this.getControlPitch();
               if (SnapRotation.moveFix) {
                  SnapRotation.snapAt(yaw, pitch);
               } else {
                  Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
               }
            }

            boolean hasActiveFirework = (Boolean)this.checkFirework.getValue() && this.isBoostedByRocket();
            boolean wearingElytra = this.isWearingUsableElytra();
            if (wearingElytra && !mc.player.isFallFlying() && !mc.player.isOnGround()) {
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
            }

            if (this.mode.getValue() == FakeFlyModule.Mode.Legit && wearingElytra && mc.player.isFallFlying() && !mc.player.isOnGround() && (Boolean)this.unbreaking.getValue() && this.swapTimer.passed(((Double)this.fakeDelay.getValue()).longValue())) {
               int syncId = mc.player.playerScreenHandler.syncId;
               mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
               this.swapTimer.reset();
            }

            if (this.mode.getValue() == FakeFlyModule.Mode.GrimDurability) {
               int elytraSlot = this.findElytraInInventory();
               if (elytraSlot != -1 && this.packetDelayCounter > (Integer)this.packetDelay.getValue()) {
                  int syncId = mc.player.playerScreenHandler.syncId;
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  if (!mc.player.isOnGround()) {
                     Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                     mc.player.startFallFlying();
                     this.fallFlying = true;
                  }

                  if (this.fireworkMode.getValue() != FakeFlyModule.FireWorkMode.None) {
                     boolean shouldShoot = false;
                     if (this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Auto) {
                        shouldShoot = !hasActiveFirework;
                     } else if (this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Delay && this.wantToMove()) {
                        shouldShoot = !(Boolean)this.checkFirework.getValue() || !hasActiveFirework;
                     }

                     if (shouldShoot) {
                        this.useFirework();
                     }
                  }

                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  this.packetDelayCounter = 0;
               }
            } else if (wearingElytra && mc.player.isFallFlying() && this.fireworkMode.getValue() != FakeFlyModule.FireWorkMode.None) {
               boolean shouldShoot = false;
               if (this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Auto) {
                  shouldShoot = !hasActiveFirework;
               } else if (this.fireworkMode.getValue() == FakeFlyModule.FireWorkMode.Delay && this.wantToMove()) {
                  shouldShoot = !(Boolean)this.checkFirework.getValue() || !hasActiveFirework;
               }

               if (shouldShoot) {
                  this.useFirework();
               }
            }

         }
      }
   }

   @EventListener
   public void onTravel(TravelEvent event) {
      if (this.fallFlying) {
         if (this.mode.getValue() != FakeFlyModule.Mode.Legit) {
            if ((Boolean)this.control.getValue()) {
               if (mc.currentScreen instanceof ChatScreen) {
                  mc.player.setVelocity(mc.player.getVelocity().x, -(Double)this.fallSpeed.getValue(), mc.player.getVelocity().z);
               } else {
                  if (!this.wantToMove()) {
                     mc.player.setVelocity((double)0.0F, -(Double)this.fallSpeed.getValue(), (double)0.0F);
                  }

               }
            }
         }
      }
   }

   private void useFirework() {
      if (this.fireworkMode.getValue() != FakeFlyModule.FireWorkMode.Delay || this.fireworkTimer.passed(((Double)this.fireworkDelay.getValue()).longValue())) {
         if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            this.fireworkTimer.reset();
         } else if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            this.fireworkTimer.reset();
         } else {
            int slot = this.findFireworkSlot();
            if (slot != -1) {
               int oldSlot = mc.player.getInventory().selectedSlot;
               if (slot >= 9 && (Boolean)this.inventorySwap.getValue()) {
                  int syncId = mc.player.playerScreenHandler.syncId;
                  mc.interactionManager.clickSlot(syncId, slot, oldSlot, SlotActionType.SWAP, mc.player);
                  Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                  mc.interactionManager.clickSlot(syncId, slot, oldSlot, SlotActionType.SWAP, mc.player);
                  Managers.NETWORK.sendPacket(new CloseHandledScreenC2SPacket(syncId));
               } else {
                  Managers.INVENTORY.setSlot(slot);
                  Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                  Managers.INVENTORY.setSlot(oldSlot);
               }

               this.fireworkTimer.reset();
               this.fireworkCooldown.reset();
            }
         }
      }
   }

   private boolean isWearingUsableElytra() {
      ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      return chest.getItem() == Items.ELYTRA && ElytraItem.isUsable(chest);
   }

   private boolean isBoostedByRocket() {
      for(Entity e : mc.world.getEntities()) {
         if (e instanceof FireworkRocketEntity rocket) {
            if (((AccessorFireworkRocketEntity)(Object)rocket).hookWasShotByEntity() && ((AccessorFireworkRocketEntity)(Object)rocket).hookGetShooter() == mc.player) {
               return true;
            }
         }
      }

      return false;
   }

   private int findElytraInInventory() {
      for(int i = 9; i < 36; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == Items.ELYTRA && ElytraItem.isUsable(stack)) {
            return i;
         }
      }

      return -1;
   }

   private int findFireworkSlot() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET) {
            return i;
         }
      }

      if ((Boolean)this.inventorySwap.getValue()) {
         for(int i = 9; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET) {
               return i;
            }
         }
      }

      return -1;
   }

   private int countFireworks() {
      int count = 0;
      int end = (Boolean)this.inventorySwap.getValue() ? 36 : 9;

      for(int i = 0; i < end; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET) {
            count += stack.getCount();
         }
      }

      if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
         count += mc.player.getOffHandStack().getCount();
      }

      return count;
   }

   private boolean wantToMove() {
      return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed();
   }

   private float getSprintYaw(float yaw) {
      boolean fwd = mc.options.forwardKey.isPressed();
      boolean back = mc.options.backKey.isPressed();
      boolean left = mc.options.leftKey.isPressed();
      boolean rght = mc.options.rightKey.isPressed();
      if (fwd && !back) {
         if (left && !rght) {
            yaw -= 45.0F;
         } else if (rght && !left) {
            yaw += 45.0F;
         }
      } else if (back && !fwd) {
         yaw += 180.0F;
         if (left && !rght) {
            yaw += 45.0F;
         } else if (rght && !left) {
            yaw -= 45.0F;
         }
      } else if (left && !rght) {
         yaw -= 90.0F;
      } else if (rght && !left) {
         yaw += 90.0F;
      }

      return yaw;
   }

   private float getControlPitch() {
      if (mc.currentScreen instanceof ChatScreen) {
         return mc.player.getPitch();
      } else {
         boolean jump = mc.options.jumpKey.isPressed();
         boolean sneak = mc.options.sneakKey.isPressed();
         if (jump && sneak) {
            return -3.0F;
         } else if (jump) {
            return MovementUtil.isMoving() ? -45.0F : -90.0F;
         } else if (sneak) {
            return MovementUtil.isMoving() ? 45.0F : 90.0F;
         } else {
            return MovementUtil.isMoving() ? -1.9F : mc.player.getPitch();
         }
      }
   }

   public void setFallFlying(boolean v) {
      this.fallFlying = v;
   }

   public boolean getFallFlying() {
      return this.fallFlying;
   }

   public float getModuleYaw() {
      return this.moduleYaw;
   }

   public float getModulePitch() {
      return this.modulePitch;
   }

   public boolean isGrimDurabilityMode() {
      return this.mode.getValue() == FakeFlyModule.Mode.GrimDurability;
   }

   public boolean isControlEnabled() {
      return (Boolean)this.control.getValue();
   }

   public static enum Mode {
      Legit,
      GrimDurability;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Legit, GrimDurability};
      }
   }

   public static enum FireWorkMode {
      Auto,
      Delay,
      None;

      // $FF: synthetic method
      private static FireWorkMode[] $values() {
         return new FireWorkMode[]{Auto, Delay, None};
      }
   }
}
