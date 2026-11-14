package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.EntityTravelEvent;
import net.hachimi.client.impl.event.entity.player.TravelEvent;
import net.hachimi.client.impl.event.network.DismountVehicleEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.exploit.DisablerModule;
import net.hachimi.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class BoatFlyModule extends ToggleModule {
   Config<Float> speedConfig = this.register(new NumberConfig("Speed", "The horizontal flight speed", 0.1F, 2.5F, 10.0F));
   Config<Float> vspeedConfig = this.register(new NumberConfig("VerticalSpeed", "The vertical flight speed", 0.1F, 1.0F, 5.0F));
   Config<Boolean> remountConfig = this.register(new BooleanConfig("Remount", "Automatically remounts", false));
   Config<AntiKick> antiKickConfig;
   Config<Boolean> accelerateConfig;
   Config<Float> accelerateSpeedConfig;
   Config<Float> maxSpeedConfig;
   private double speed;
   private double lastY;
   private boolean floating;
   private int floatingTicks;
   private boolean modifyY;

   public BoatFlyModule() {
      super("BoatFly", "Allows you fly with entities", ModuleCategory.MOVEMENT);
      this.antiKickConfig = this.register(new EnumConfig("AntiKick", "Prevents vanilla flight detection", BoatFlyModule.AntiKick.NORMAL, BoatFlyModule.AntiKick.values()));
      this.accelerateConfig = this.register(new BooleanConfig("Accelerate", "Accelerate as you fly", false));
      this.accelerateSpeedConfig = this.register(new NumberConfig("AccelerateSpeed", "Speed to accelerate as", 0.01F, 0.2F, 1.0F, () -> this.accelerateConfig.getValue()));
      this.maxSpeedConfig = this.register(new NumberConfig("MaxSpeed", "Max speed to accelerate to", 1.0F, 5.0F, 10.0F, () -> this.accelerateConfig.getValue()));
   }

   public void onEnable() {
      this.speed = (double)0.0F;
   }

   public void onDisable() {
      this.modifyY = false;
   }

   @EventListener
   public void onTravel(EntityTravelEvent event) {
      if (mc.player.getVehicle() != null && event.getEntity().getId() == mc.player.getVehicle().getId()) {
         event.cancel();
      }

   }

   @EventListener
   public void onDismountVehicle(DismountVehicleEvent event) {
      if (mc.options.sneakKey.isPressed()) {
         event.cancel();
      }

   }

   @EventListener
   public void onPlayerTick(TravelEvent event) {
      if (mc.player.getVehicle() != null && !DisablerModule.getInstance().grimFireworkCheck()) {
         Entity ridingEntity = mc.player.getVehicle();
         if ((Boolean)this.accelerateConfig.getValue()) {
            if (!MovementUtil.isInputtingMovement() || ridingEntity.horizontalCollision) {
               this.speed = (double)0.0F;
            }

            this.speed += (double)(Float)this.accelerateSpeedConfig.getValue();
            if (this.speed > (double)(Float)this.maxSpeedConfig.getValue()) {
               this.speed = (double)(Float)this.maxSpeedConfig.getValue();
            }
         } else {
            this.speed = (double)(Float)this.speedConfig.getValue();
         }

         ridingEntity.setYaw(mc.player.getYaw());
         if (ridingEntity instanceof LlamaEntity) {
            LlamaEntity llama = (LlamaEntity)ridingEntity;
            llama.headYaw = mc.player.getYaw();
         }

         boolean stopVerticalMovement = false;
         if (this.floating) {
            ++this.floatingTicks;
            if (this.floatingTicks >= 20) {
               if (this.antiKickConfig.getValue() == BoatFlyModule.AntiKick.PACKET) {
                  this.modifyY = true;
               } else if (this.antiKickConfig.getValue() == BoatFlyModule.AntiKick.NORMAL) {
                  ridingEntity.setPosition(ridingEntity.getX(), ridingEntity.getY() - 0.0313, ridingEntity.getZ());
               }

               this.floatingTicks = 0;
               this.floating = false;
               stopVerticalMovement = true;
            }
         }

         ridingEntity.setVelocity(ridingEntity.getVelocity().x, (double)0.0F, ridingEntity.getVelocity().z);
         if (mc.options.jumpKey.isPressed() && !stopVerticalMovement) {
            ridingEntity.setVelocity(ridingEntity.getVelocity().x, (double)(Float)this.vspeedConfig.getValue(), ridingEntity.getVelocity().z);
         } else if (mc.options.sneakKey.isPressed()) {
            ridingEntity.setVelocity(ridingEntity.getVelocity().x, (double)(-(Float)this.vspeedConfig.getValue()), ridingEntity.getVelocity().z);
         }

         this.speed = Math.max(this.speed, (double)0.2873F);
         float forward = mc.player.input.movementForward;
         float strafe = mc.player.input.movementSideways;
         float yaw = mc.player.getYaw();
         if (forward != 0.0F || strafe != 0.0F) {
            double rx = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
            double rz = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
            double var10001 = (double)forward * this.speed * rx + (double)strafe * this.speed * rz;
            double var10003 = (double)forward * this.speed * rz - (double)strafe * this.speed * rx;
            ridingEntity.setVelocity(var10001, ridingEntity.getVelocity().y, var10003);
            event.cancel();
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.player.getVehicle() != null) {
         Entity ridingEntity = mc.player.getVehicle();
         Packet packetY = event.getPacket();
         if (packetY instanceof VehicleMoveC2SPacket) {
            VehicleMoveC2SPacket packet = (VehicleMoveC2SPacket)packetY;
            if (this.antiKickConfig.getValue() != BoatFlyModule.AntiKick.OFF) {
               double packetYOffset = packet.getY();
               if (this.modifyY) {
                  ((AccessorPlayerMoveC2SPacket)(Object)packet).hookSetY(this.lastY - 0.04);
                  this.modifyY = false;
               } else {
                  this.floating = this.floatingCheck(packet, ridingEntity);
                  this.lastY = packetYOffset;
               }
            }
         }

         packetY = event.getPacket();
         if (packetY instanceof net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket) {
            net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket packet = (net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket)packetY;
            if (packet.getMode() == net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY && mc.player.getVehicle() != null && !mc.player.getVehicle().isOnGround()) {
               event.cancel();
            }
         }

      }
   }

   private boolean floatingCheck(VehicleMoveC2SPacket packet, Entity ridingEntity) {
      double e = MathHelper.clamp(packet.getY(), (double)-2.0E7F, (double)2.0E7F);
      double s = e - this.lastY;
      return s >= (double)-0.03125F && !ridingEntity.groundCollision && this.isEntityOnAir(ridingEntity);
   }

   private boolean isEntityOnAir(Entity entity) {
      return entity.getWorld().getStatesInBox(entity.getBoundingBox().expand((double)0.0625F).stretch((double)0.0F, -0.55, (double)0.0F)).allMatch(net.minecraft.block.AbstractBlock.AbstractBlockState::isAir);
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
