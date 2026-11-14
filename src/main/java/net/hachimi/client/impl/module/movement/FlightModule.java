package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.exploit.DisablerModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.block.AbstractBlock;

public class FlightModule extends ToggleModule {
   private static FlightModule INSTANCE;
   Config<FlightMode> modeConfig;
   Config<Float> speedConfig;
   Config<Float> vspeedConfig;
   Config<AntiKick> antiKickConfig;
   Config<Boolean> accelerateConfig;
   Config<Float> accelerateSpeedConfig;
   Config<Float> maxSpeedConfig;
   private double speed;
   private double lastY;
   private boolean floating;
   private int floatingTicks;
   private boolean modifyY;

   public FlightModule() {
      super("Flight", "Allows the player to fly in survival", ModuleCategory.MOVEMENT);
      this.modeConfig = this.register(new EnumConfig("Mode", "The mode for vanilla flight", FlightModule.FlightMode.NORMAL, FlightModule.FlightMode.values()));
      this.speedConfig = this.register(new NumberConfig("Speed", "The horizontal flight speed", 0.1F, 2.5F, 10.0F));
      this.vspeedConfig = this.register(new NumberConfig("VerticalSpeed", "The vertical flight speed", 0.1F, 1.0F, 5.0F));
      this.antiKickConfig = this.register(new EnumConfig("AntiKick", "Prevents vanilla flight detection", FlightModule.AntiKick.NORMAL, FlightModule.AntiKick.values()));
      this.accelerateConfig = this.register(new BooleanConfig("Accelerate", "Accelerate as you fly", false));
      this.accelerateSpeedConfig = this.register(new NumberConfig("AccelerateSpeed", "Speed to accelerate as", 0.01F, 0.2F, 1.0F, () -> this.accelerateConfig.getValue()));
      this.maxSpeedConfig = this.register(new NumberConfig("MaxSpeed", "Max speed to acceleratee to", 1.0F, 5.0F, 10.0F, () -> this.accelerateConfig.getValue()));
      INSTANCE = this;
   }

   public static FlightModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   public void onEnable() {
      if (this.modeConfig.getValue() == FlightModule.FlightMode.VANILLA) {
         this.enableVanillaFly();
      }

      this.speed = (double)0.0F;
   }

   public void onDisable() {
      if (this.modeConfig.getValue() == FlightModule.FlightMode.VANILLA) {
         this.disableVanillaFly();
      }

      this.modifyY = false;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if ((Boolean)this.accelerateConfig.getValue()) {
         if (!MovementUtil.isInputtingMovement() || mc.player.horizontalCollision) {
            this.speed = (double)0.0F;
         }

         this.speed += (double)(Float)this.accelerateSpeedConfig.getValue();
         if (this.speed > (double)(Float)this.maxSpeedConfig.getValue()) {
            this.speed = (double)(Float)this.maxSpeedConfig.getValue();
         }
      } else {
         this.speed = (double)(Float)this.speedConfig.getValue();
      }

      if (DisablerModule.getInstance().grimFireworkCheck()) {
         if (this.modeConfig.getValue() == FlightModule.FlightMode.VANILLA) {
            this.disableVanillaFly();
         }

      } else {
         if (((FlightMode)this.modeConfig.getValue()).equals(FlightModule.FlightMode.VANILLA)) {
            this.enableVanillaFly();
            mc.player.getAbilities().setFlySpeed((float)(this.speed * (double)0.05F));
         } else {
            this.disableVanillaFly();
         }

         boolean stopVerticalMovement = false;
         if (this.floating) {
            ++this.floatingTicks;
            if (this.floatingTicks >= 20) {
               if (this.antiKickConfig.getValue() == FlightModule.AntiKick.PACKET) {
                  this.modifyY = true;
               } else if (this.antiKickConfig.getValue() == FlightModule.AntiKick.NORMAL) {
                  mc.player.setPosition(mc.player.getX(), mc.player.getY() - 0.0313, mc.player.getZ());
                  if (this.modeConfig.getValue() == FlightModule.FlightMode.VANILLA) {
                     this.disableVanillaFly();
                     Managers.MOVEMENT.setMotionY((double)0.0F);
                     stopVerticalMovement = true;
                  }
               }

               this.floatingTicks = 0;
               this.floating = false;
            }
         }

         if (this.modeConfig.getValue() == FlightModule.FlightMode.NORMAL) {
            Managers.MOVEMENT.setMotionY((double)0.0F);
            if (mc.options.jumpKey.isPressed() && !stopVerticalMovement) {
               Managers.MOVEMENT.setMotionY((double)(Float)this.vspeedConfig.getValue());
            } else if (mc.options.sneakKey.isPressed()) {
               Managers.MOVEMENT.setMotionY((double)(-(Float)this.vspeedConfig.getValue()));
            }

            this.speed = Math.max(this.speed, (double)0.2873F);
            float forward = mc.player.input.movementForward;
            float strafe = mc.player.input.movementSideways;
            float yaw = mc.player.getYaw();
            if (forward == 0.0F && strafe == 0.0F) {
               Managers.MOVEMENT.setMotionXZ((double)0.0F, (double)0.0F);
               return;
            }

            double rx = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
            double rz = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
            Managers.MOVEMENT.setMotionXZ((double)forward * this.speed * rx + (double)strafe * this.speed * rz, (double)forward * this.speed * rz - (double)strafe * this.speed * rx);
         }

      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) {
            net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet = (net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)var3;
            if (this.antiKickConfig.getValue() != FlightModule.AntiKick.OFF) {
               double packetY = packet.getY(Double.NaN);
               if (!Double.isNaN(packetY)) {
                  if (this.modifyY) {
                     ((AccessorPlayerMoveC2SPacket)(Object)packet).hookSetY(this.lastY - 0.04);
                     this.modifyY = false;
                     return;
                  }

                  this.floating = this.floatingCheck(packet);
                  this.lastY = packetY;
                  return;
               }

               if (this.modifyY) {
                  net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet1 = (net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)(packet.changesLook() ? new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() - 0.04, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), packet.isOnGround()) : new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.04, mc.player.getZ(), packet.isOnGround()));
                  event.cancel();
                  Managers.NETWORK.sendQuietPacket(packet1);
                  this.modifyY = false;
               }
            }
         }

      }
   }

   private boolean floatingCheck(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
      double e = MathHelper.clamp(packet.getY(mc.player.getY()), (double)-2.0E7F, (double)2.0E7F);
      double s = e - this.lastY;
      return s >= (double)-0.03125F && !mc.player.groundCollision && this.isEntityOnAir(mc.player);
   }

   private boolean isEntityOnAir(Entity entity) {
      return entity.getWorld().getStatesInBox(entity.getBoundingBox().expand((double)0.0625F).stretch((double)0.0F, -0.55, (double)0.0F)).allMatch(net.minecraft.block.AbstractBlock.AbstractBlockState::isAir);
   }

   private void enableVanillaFly() {
      mc.player.getAbilities().allowFlying = true;
      mc.player.getAbilities().flying = true;
   }

   private void disableVanillaFly() {
      if (!mc.player.isCreative()) {
         mc.player.getAbilities().allowFlying = false;
      }

      mc.player.getAbilities().flying = false;
      mc.player.getAbilities().setFlySpeed(0.05F);
   }

   public static enum FlightMode {
      NORMAL,
      VANILLA;

      // $FF: synthetic method
      private static FlightMode[] $values() {
         return new FlightMode[]{NORMAL, VANILLA};
      }
   }

   public static enum AntiKick {
      NORMAL,
      PACKET,
      OFF;

      // $FF: synthetic method
      private static AntiKick[] $values() {
         return new AntiKick[]{NORMAL, PACKET, OFF};
      }
   }
}
