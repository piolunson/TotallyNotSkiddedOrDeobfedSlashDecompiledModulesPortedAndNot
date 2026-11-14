package net.hachimi.client.impl.module.movement;

import java.util.Objects;
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
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.TickTimer;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class ElytraFlyModule extends RotationModule {
   private static ElytraFlyModule INSTANCE;
   Config<Mode> modeConfig;
   Config<Float> controlSpeedConfig;
   Config<Float> controlUpFactorConfig;
   Config<Float> controlDownFactorConfig;
   Config<Boolean> controlSpeedLimitConfig;
   Config<Float> controlMaxSpeedConfig;
   Config<Boolean> controlNoDragConfig;
   Config<Boolean> noMoveConfig;
   Config<Boolean> noAnimationConfig;
   Config<Boolean> sneakingFixConfig;
   Config<Boolean> swapAlternativeConfig;
   Config<Boolean> freezeConfig;
   Config<FreezeMode> freezeModeConfig;
   Config<Float> freezeDurationConfig;
   Config<Boolean> noRotationConfig;
   Config<Boolean> packetConfig;
   Config<Boolean> grimV3Config;
   Config<Float> grimDelayConfig;
   Config<Boolean> autoJumpConfig;
   Config<Boolean> autoFlyConfig;
   Config<Boolean> fireworksConfig;
   Config<Float> delayConfig;
   Config<Boolean> rotateConfig;
   Config<Boolean> rotateHoldConfig;
   Config<Boolean> motionStopConfig;
   private static final double FALL_SPEED = 0.02;
   private boolean isUsingFirework;
   private boolean isFallFlying;
   private int packetDelayInt;
   private final CacheTimer fireworkTimer;
   private final CacheTimer swapTimer;
   private final CacheTimer freezeTimer;
   private final TickTimer freezeTickTimer;
   private boolean wasMovingLastTick;

   public ElytraFlyModule() {
      super("ElytraFly", "Elytra fly using fireworks (ported from LeavesHack)", ModuleCategory.MOVEMENT);
      this.modeConfig = this.register(new EnumConfig("Mode", "Vanilla: LeavesHack-style rotate/travel logic. Control: directly set velocity like free-fly", ElytraFlyModule.Mode.Vanilla, ElytraFlyModule.Mode.values()));
      this.controlSpeedConfig = this.register(new NumberConfig("ControlSpeed", "Horizontal fly speed while in Control mode", 0.1F, 1.0F, 10.0F, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control));
      this.controlUpFactorConfig = this.register(new NumberConfig("ControlUpFactor", "Vertical speed while holding jump in Control mode", 0.0F, 1.0F, 10.0F, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control));
      this.controlDownFactorConfig = this.register(new NumberConfig("ControlDownFactor", "Vertical speed while holding sneak in Control mode", 0.0F, 1.0F, 10.0F, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control));
      this.controlSpeedLimitConfig = this.register(new BooleanConfig("ControlSpeedLimit", "Cap horizontal speed in Control mode", true, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control));
      this.controlMaxSpeedConfig = this.register(new NumberConfig("ControlMaxSpeed", "Max horizontal speed in Control mode", 0.1F, 2.5F, 10.0F, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control && (Boolean)this.controlSpeedLimitConfig.getValue()));
      this.controlNoDragConfig = this.register(new BooleanConfig("ControlNoDrag", "Ignore vanilla air drag while in Control mode", false, () -> this.modeConfig.getValue() == ElytraFlyModule.Mode.Control));
      this.noMoveConfig = this.register(new BooleanConfig("NoMove", "Only move while a movement key is held", true));
      this.noAnimationConfig = this.register(new BooleanConfig("NoAnimation", "Stand still pose while flying", true));
      this.sneakingFixConfig = this.register(new BooleanConfig("SneakingFix", "Automatically press/release shift when the module is disabled", true));
      this.swapAlternativeConfig = this.register(new BooleanConfig("SwapAlternative", "Also search the full inventory (not just the hotbar) for fireworks", true));
      this.freezeConfig = this.register(new BooleanConfig("Freeze", "Freeze position when not moving", false));
      FreezeMode var10006 = ElytraFlyModule.FreezeMode.Static;
      FreezeMode[] var10007 = ElytraFlyModule.FreezeMode.values();
      Config var10008 = this.freezeConfig;
      Objects.requireNonNull(var10008);
      this.freezeModeConfig = this.register(new EnumConfig("FreezeMode", "How Freeze releases itself: never (Static), after N ticks (Tick), or after N ms (Timer)", var10006, var10007, var10008::getValue));
      this.freezeDurationConfig = this.register(new NumberConfig("FreezeDuration", "Duration before Freeze releases itself (ticks for Tick mode, milliseconds for Timer mode)", 1.0F, 20.0F, 20000.0F, () -> (Boolean)this.freezeConfig.getValue() && this.freezeModeConfig.getValue() != ElytraFlyModule.FreezeMode.Static));
      this.noRotationConfig = this.register(new BooleanConfig("NoRotation", "Prevents sending rotation packets to server", false));
      this.packetConfig = this.register(new BooleanConfig("Packet", "Sends anti-kick movement packets", false));
      this.grimV3Config = this.register(new BooleanConfig("GrimV3", "Continuously swaps armor/elytra to avoid durability-based flags on GrimAC", true));
      this.grimDelayConfig = this.register(new NumberConfig("GrimDelay", "Tick delay between each GrimV3 swap cycle", 0.0F, 3.0F, 100.0F, () -> this.grimV3Config.getValue()));
      this.autoJumpConfig = this.register(new BooleanConfig("AutoJump", "Auto jump to start elytra", false));
      this.autoFlyConfig = this.register(new BooleanConfig("AutoFly", "Automatically starts fall-flying when wearing an elytra while falling", true));
      this.fireworksConfig = this.register(new BooleanConfig("Fireworks", "Automatically fires a firework when nothing is currently boosting", true));
      this.delayConfig = this.register(new NumberConfig("Delay", "Minimum delay (seconds) between each automatic firework shot", 0.0F, 0.8F, 5.0F, () -> this.fireworksConfig.getValue()));
      this.rotateConfig = this.register(new BooleanConfig("Rotate", "Sends a rotation packet every tick following movement direction (requires Rotations > MovementFix enabled to keep actual movement in sync)", true));
      this.rotateHoldConfig = this.register(new BooleanConfig("RotateHold", "Holds rotation even when not moving", false, () -> this.rotateConfig.getValue()));
      this.motionStopConfig = this.register(new BooleanConfig("MotionStop", "Stops all motion while rotating and not actively moving", false, () -> this.rotateConfig.getValue()));
      this.isUsingFirework = false;
      this.isFallFlying = false;
      this.packetDelayInt = 0;
      this.fireworkTimer = new CacheTimer();
      this.swapTimer = new CacheTimer();
      this.freezeTimer = new CacheTimer();
      this.freezeTickTimer = new TickTimer();
      this.wasMovingLastTick = true;
      INSTANCE = this;
   }

   public static ElytraFlyModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      this.fireworkTimer.setElapsedTime(-255L);
      this.swapTimer.setElapsedTime(-255L);
      this.packetDelayInt = 0;
      this.freezeTimer.reset();
      this.freezeTickTimer.reset();
      this.wasMovingLastTick = true;
   }

   public void onDisable() {
      if (mc.player != null) {
         if ((Boolean)this.sneakingFixConfig.getValue()) {
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
         }

      }
   }

   @EventListener
   public void onElytraUpdate(ElytraUpdateEvent event) {
      if ((Boolean)this.noAnimationConfig.getValue()) {
         event.cancel();
      }

   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen) || mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof CreativeInventoryScreen) {
            ++this.packetDelayInt;
            float yaw = this.getSprintYaw(mc.player.getYaw());
            float pitch = this.getPitch(mc.player.getPitch());
            boolean followerSteering = FollowerModule.getInstance() != null && FollowerModule.getInstance().canFollow();
            if ((Boolean)this.rotateConfig.getValue() && !(Boolean)this.noRotationConfig.getValue() && !followerSteering && (this.wantToMove() || (Boolean)this.rotateHoldConfig.getValue())) {
               mc.player.setYaw(yaw);
               mc.player.setPitch(pitch);
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
            }

            boolean hasFirework = false;

            for(Entity entity : mc.world.getEntities()) {
               if (entity instanceof FireworkRocketEntity) {
                  FireworkRocketEntity firework = (FireworkRocketEntity)entity;
                  if (firework.getOwner() == mc.player) {
                     hasFirework = true;
                     break;
                  }
               }
            }

            this.isUsingFirework = hasFirework;
            int elytraSlot = this.findItemInventorySlot(Items.ELYTRA);
            boolean wearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && ElytraItem.isUsable(mc.player.getEquippedStack(EquipmentSlot.CHEST));
            if ((Boolean)this.autoFlyConfig.getValue() && wearingElytra && !this.isFallFlying && !mc.player.isOnGround()) {
               this.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
            }

            if ((Boolean)this.autoJumpConfig.getValue() && mc.player.isOnGround() && !mc.player.isFallFlying() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem) {
               mc.player.jump();
               this.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
            }

            if ((Boolean)this.grimV3Config.getValue()) {
               if (elytraSlot != -1 && (float)this.packetDelayInt > (Float)this.grimDelayConfig.getValue()) {
                  int syncId = mc.player.currentScreenHandler.syncId;
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  if (!mc.player.isOnGround()) {
                     this.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                     mc.player.startFallFlying();
                  }

                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                  this.packetDelayInt = 0;
               }
            } else if (wearingElytra && this.isFallFlying && !mc.player.isOnGround() && this.swapTimer.passed(800)) {
               int syncId = mc.player.currentScreenHandler.syncId;
               mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
               this.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               mc.player.startFallFlying();
               this.swapTimer.reset();
            }

            boolean standingStillIntentionally = ((Boolean)this.noMoveConfig.getValue() || (Boolean)this.freezeConfig.getValue()) && !this.wantToMove();
            if ((Boolean)this.fireworksConfig.getValue() && wearingElytra && this.isFallFlying && !hasFirework && !standingStillIntentionally) {
               this.fireFirework();
            }

         }
      }
   }

   @EventListener
   public void onTravel(TravelEvent event) {
      if (this.isFallFlying) {
         if (event.getStage() == StageEvent.EventStage.PRE) {
            if (this.modeConfig.getValue() == ElytraFlyModule.Mode.Control) {
               this.applyControlVelocity();
            } else if (FollowerModule.getInstance() == null || !FollowerModule.getInstance().canFollow()) {
               if ((Boolean)this.noMoveConfig.getValue() || (Boolean)this.freezeConfig.getValue()) {
                  if (mc.currentScreen instanceof ChatScreen) {
                     this.setVelocityY(0.02);
                  } else {
                     if (!this.wantToMove()) {
                        this.setVelocityX((double)0.0F);
                        this.setVelocityZ((double)0.0F);
                        boolean freezeExpired = (Boolean)this.freezeConfig.getValue() && this.freezeModeConfig.getValue() != ElytraFlyModule.FreezeMode.Static && this.isFreezeDurationElapsed();
                        if ((Boolean)this.freezeConfig.getValue() && !freezeExpired) {
                           this.setVelocityY((double)0.0F);
                        } else {
                           this.setVelocityY(0.02);
                        }

                        this.wasMovingLastTick = false;
                     } else {
                        if (!this.wasMovingLastTick) {
                           this.freezeTimer.reset();
                           this.freezeTickTimer.reset();
                        }

                        this.wasMovingLastTick = true;
                     }

                  }
               }
            }
         }
      }
   }

   private void applyControlVelocity() {
      if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
         this.setVelocityY((double)0.0F);
      } else if (mc.options.sneakKey.isPressed()) {
         this.setVelocityY((double)(-(Float)this.controlDownFactorConfig.getValue()));
      } else if (mc.player.input.jumping) {
         this.setVelocityY((double)(-(Float)this.controlUpFactorConfig.getValue()));
      } else {
         this.setVelocityY(3.0E-11 * (double)(Float)this.controlDownFactorConfig.getValue());
      }

      double yaw = Math.toRadians((double)mc.player.getYaw());
      double dirX = -Math.sin(yaw);
      double dirZ = Math.cos(yaw);
      double speed = (double)(Float)this.controlSpeedConfig.getValue();
      double vx = (double)0.0F;
      double vz = (double)0.0F;
      if (mc.options.forwardKey.isPressed()) {
         vx += dirX * speed;
         vz += dirZ * speed;
      }

      if (mc.options.backKey.isPressed()) {
         vx -= dirX * speed;
         vz -= dirZ * speed;
      }

      if (mc.options.leftKey.isPressed()) {
         vx += dirZ * speed;
         vz -= dirX * speed;
      }

      if (mc.options.rightKey.isPressed()) {
         vx -= dirZ * speed;
         vz += dirX * speed;
      }

      if ((Boolean)this.controlSpeedLimitConfig.getValue()) {
         double max = (double)(Float)this.controlMaxSpeedConfig.getValue();
         double horiz = Math.sqrt(vx * vx + vz * vz);
         if (horiz > max && horiz > (double)0.0F) {
            vx = vx / horiz * max;
            vz = vz / horiz * max;
         }
      }

      if ((Boolean)this.controlNoDragConfig.getValue()) {
         this.setVelocityX(-vx);
         this.setVelocityZ(-vz);
      } else {
         Vec3d v = mc.player.getVelocity();
         mc.player.setVelocity(v.x * 0.6 + vx, v.y, v.z * 0.6 + vz);
      }

   }

   private boolean isFreezeDurationElapsed() {
      boolean var10000;
      switch (((FreezeMode)this.freezeModeConfig.getValue()).ordinal()) {
         case 0 -> var10000 = false;
         case 1 -> var10000 = this.freezeTickTimer.passed((Number)this.freezeDurationConfig.getValue());
         case 2 -> var10000 = this.freezeTimer.passed((Number)this.freezeDurationConfig.getValue());
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private void setVelocityX(double x) {
      Vec3d v = mc.player.getVelocity();
      mc.player.setVelocity(x, v.y, v.z);
   }

   private void setVelocityY(double y) {
      Vec3d v = mc.player.getVelocity();
      mc.player.setVelocity(v.x, -y, v.z);
   }

   private void setVelocityZ(double z) {
      Vec3d v = mc.player.getVelocity();
      mc.player.setVelocity(v.x, v.y, z);
   }

   public void setFallFlying(boolean value) {
      this.isFallFlying = value;
   }

   public boolean getFallFlying() {
      return this.isFallFlying;
   }

   public void fireFirework() {
      if (this.fireworkTimer.passed((long)((Float)this.delayConfig.getValue() * 1000.0F))) {
         if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            this.fireworkTimer.reset();
         } else if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            this.fireworkTimer.reset();
         } else {
            int firework;
            if ((Boolean)this.swapAlternativeConfig.getValue() && (firework = this.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
               this.inventorySwap(firework, mc.player.getInventory().selectedSlot);
               Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
               this.inventorySwap(firework, mc.player.getInventory().selectedSlot);
               this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
               this.fireworkTimer.reset();
            } else if ((firework = this.findItemHotbar(Items.FIREWORK_ROCKET)) != -1) {
               int old = mc.player.getInventory().selectedSlot;
               this.switchToSlot(firework);
               Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
               this.switchToSlot(old);
               this.fireworkTimer.reset();
            }
         }

      }
   }

   private int findItemInventorySlot(Item item) {
      for(int i = 0; i < 36; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   private int findItemHotbar(Item item) {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private void inventorySwap(int slotA, int hotbarSlot) {
      int syncId = mc.player.currentScreenHandler.syncId;
      mc.interactionManager.clickSlot(syncId, slotA, hotbarSlot, SlotActionType.SWAP, mc.player);
   }

   private void switchToSlot(int hotbarSlot) {
      Managers.INVENTORY.setSlot(hotbarSlot);
   }

   private void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(packet);
      }

   }

   private boolean wantToMove() {
      return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed();
   }

   public boolean isMoving() {
      if (mc.player != null && mc.player.input != null) {
         return (double)mc.player.input.movementForward != (double)0.0F || (double)mc.player.input.movementSideways != (double)0.0F;
      } else {
         return false;
      }
   }

   public float getSprintYaw(float yaw) {
      if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
         if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw -= 45.0F;
         } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw += 45.0F;
         }
      } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
         yaw += 180.0F;
         if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw += 45.0F;
         } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw -= 45.0F;
         }
      } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
         yaw -= 90.0F;
      } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
         yaw += 90.0F;
      }

      return yaw;
   }

   private float getPitch(float pitch) {
      if (!(mc.currentScreen instanceof ChatScreen)) {
         if (mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed()) {
            pitch = -3.0F;
         } else if (mc.options.jumpKey.isPressed()) {
            pitch = this.isMoving() ? -45.0F : -90.0F;
         } else if (mc.options.sneakKey.isPressed()) {
            pitch = this.isMoving() ? 45.0F : 90.0F;
         }

         if (this.isMoving() && !mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()) {
            pitch = -1.9F;
         }
      }

      return pitch;
   }

   public boolean isPhased() {
      return mc.world.canCollide(mc.player, mc.player.getBoundingBox());
   }

   public static enum Mode {
      Vanilla,
      Control;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{Vanilla, Control};
      }
   }

   public static enum FreezeMode {
      Static,
      Tick,
      Timer;

      // $FF: synthetic method
      private static FreezeMode[] $values() {
         return new FreezeMode[]{Static, Tick, Timer};
      }
   }
}
