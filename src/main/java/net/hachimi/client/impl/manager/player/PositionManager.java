package net.hachimi.client.impl.manager.player;

import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;

public class PositionManager implements Globals {
   private double x;
   private double y;
   private double z;
   private BlockPos blockPos;
   private boolean sneaking;
   private boolean sprinting;
   private boolean onGround;

   public PositionManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   public void setPosition(Vec3d vec3d) {
      this.setPosition(vec3d.getX(), vec3d.getY(), vec3d.getZ());
   }

   public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
      this.setPositionClient(x, y, z);
      Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, this.isOnGround()));
   }

   public void setPosition(double x, double y, double z) {
      this.setPositionClient(x, y, z);
      Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, this.isOnGround()));
   }

   public void setPositionClient(double x, double y, double z) {
      if (mc.player.isRiding()) {
         mc.player.getVehicle().setPosition(x, y, z);
      } else {
         mc.player.setPosition(x, y, z);
      }
   }

   public void setPositionXZ(double x, double z) {
      this.setPosition(x, this.y, z);
   }

   public void setPositionY(double y) {
      this.setPosition(this.x, y, this.z);
   }

   public Vec3d getPos() {
      return new Vec3d(this.getX(), this.getY(), this.getZ());
   }

   public Vec3d getEyePos() {
      return this.getPos().add((double)0.0F, (double)mc.player.getStandingEyeHeight(), (double)0.0F);
   }

   public final Vec3d getCameraPosVec(float tickDelta) {
      double d = MathHelper.lerp((double)tickDelta, mc.player.prevX, this.getX());
      double e = MathHelper.lerp((double)tickDelta, mc.player.prevY, this.getY()) + (double)mc.player.getStandingEyeHeight();
      double f = MathHelper.lerp((double)tickDelta, mc.player.prevZ, this.getZ());
      return new Vec3d(d, e, f);
   }

   public double squaredDistanceTo(Entity entity) {
      float f = (float)(this.getX() - entity.getX());
      float g = (float)(this.getY() - entity.getY());
      float h = (float)(this.getZ() - entity.getZ());
      return MathHelper.squaredMagnitude((double)f, (double)g, (double)h);
   }

   public double squaredReachDistanceTo(Entity entity) {
      Vec3d cam = this.getCameraPosVec(1.0F);
      float f = (float)(cam.getX() - entity.getX());
      float g = (float)(cam.getY() - entity.getY());
      float h = (float)(cam.getZ() - entity.getZ());
      return MathHelper.squaredMagnitude((double)f, (double)g, (double)h);
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null) {
         Packet var4 = event.getPacket();
         if (var4 instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) {
            net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet = (net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)var4;
            this.onGround = packet.isOnGround();
            if (packet.changesPosition()) {
               this.x = packet.getX(this.x);
               this.y = packet.getY(this.y);
               this.z = packet.getZ(this.z);
               this.blockPos = BlockPos.ofFloored(this.x, this.y, this.z);
            }
         } else {
            var4 = event.getPacket();
            if (var4 instanceof ClientCommandC2SPacket) {
               ClientCommandC2SPacket packet = (ClientCommandC2SPacket)var4;
               switch (packet.getMode()) {
                  case START_SPRINTING -> this.sprinting = true;
                  case STOP_SPRINTING -> this.sprinting = false;
                  case PRESS_SHIFT_KEY -> this.sneaking = true;
                  case RELEASE_SHIFT_KEY -> this.sneaking = false;
               }
            }
         }
      }

   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public boolean isSneaking() {
      return this.sneaking;
   }

   public boolean isSprinting() {
      return this.sprinting;
   }

   public boolean isOnGround() {
      return this.onGround;
   }
}
