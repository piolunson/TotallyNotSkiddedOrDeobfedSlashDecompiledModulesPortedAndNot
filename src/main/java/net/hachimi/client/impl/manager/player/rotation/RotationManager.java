package net.hachimi.client.impl.manager.player.rotation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.impl.event.entity.UpdateVelocityEvent;
import net.hachimi.client.impl.event.entity.player.PlayerJumpEvent;
import net.hachimi.client.impl.event.keyboard.KeyboardTickEvent;
import net.hachimi.client.impl.event.network.MovementPacketsEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.network.PlayerUpdateEvent;
import net.hachimi.client.impl.event.render.entity.RenderPlayerEvent;
import net.hachimi.client.impl.imixin.IClientPlayerEntity;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.impl.module.client.RotationsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

public class RotationManager implements Globals {
   private final List<Rotation> requests = new CopyOnWriteArrayList();
   private float serverYaw;
   private float serverPitch;
   private float lastServerYaw;
   private float lastServerPitch;
   private float prevJumpYaw;
   private float prevYaw;
   private float prevPitch;
   boolean rotate;
   private Rotation rotation;
   private int rotateTicks;
   private boolean webJumpFix;
   private boolean preJumpFix;

   public RotationManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) {
            net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet = (net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)var3;
            if (packet.changesLook()) {
               float packetYaw = packet.getYaw(0.0F);
               float packetPitch = packet.getPitch(0.0F);
               this.serverYaw = packetYaw;
               this.serverPitch = packetPitch;
            }
         }

      }
   }

   @EventListener(
      priority = Integer.MIN_VALUE
   )
   public void onUpdate(PlayerTickEvent event) {
      this.webJumpFix = PlayerUtil.inWeb((double)1.0F);
      if (this.requests.isEmpty()) {
         this.rotation = null;
      } else {
         Rotation request = this.getRotationRequest();
         if (request == null) {
            if (this.isDoneRotating()) {
               this.rotation = null;
               return;
            }
         } else {
            this.rotation = request;
         }

         if (this.rotation != null) {
            this.rotateTicks = 0;
            this.rotate = true;
         }
      }
   }

   @EventListener
   public void onMovementPackets(MovementPacketsEvent event) {
      if (this.rotation != null) {
         if (this.rotate) {
            this.removeRotation(this.rotation);
            event.cancel();
            event.setYaw(this.rotation.getYaw());
            event.setPitch(this.rotation.getPitch());
            this.rotate = false;
         }

         if (this.rotation.isSnap()) {
            this.rotation = null;
         }
      }

   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST) {
         this.lastServerYaw = ((IClientPlayerEntity)mc.player).getLastSpoofedYaw();
         this.lastServerPitch = ((IClientPlayerEntity)mc.player).getLastSpoofedPitch();
      }

   }

   @EventListener
   public void onKeyboardTick(KeyboardTickEvent event) {
      if (this.rotation != null && mc.player != null && RotationsModule.getInstance().getMovementFix()) {
         float forward = mc.player.input.movementForward;
         float sideways = mc.player.input.movementSideways;
         float delta = (mc.player.getYaw() - this.rotation.getYaw()) * ((float)Math.PI / 180F);
         float cos = MathHelper.cos(delta);
         float sin = MathHelper.sin(delta);
         mc.player.input.movementSideways = (float)Math.round(sideways * cos - forward * sin);
         mc.player.input.movementForward = (float)Math.round(forward * cos + sideways * sin);
      }

   }

   @EventListener
   public void onUpdateVelocity(UpdateVelocityEvent event) {
      if (this.rotation != null && RotationsModule.getInstance().getMovementFix()) {
         event.cancel();
         event.setVelocity(this.movementInputToVelocity(this.rotation.getYaw(), event.getMovementInput(), event.getSpeed()));
      }

   }

   @EventListener
   public void onPlayerJump(PlayerJumpEvent event) {
      if (this.rotation != null && RotationsModule.getInstance().getMovementFix()) {
         if (event.getStage() == StageEvent.EventStage.PRE) {
            this.prevJumpYaw = mc.player.getYaw();
            mc.player.setYaw(this.rotation.getYaw());
            if (AnticheatModule.getInstance().getWebJumpFix() && this.webJumpFix) {
               this.preJumpFix = mc.player.isSprinting();
               mc.player.setSprinting(false);
            }
         } else {
            mc.player.setYaw(this.prevJumpYaw);
            if (this.webJumpFix) {
               mc.player.setSprinting(this.preJumpFix);
            }
         }
      }

   }

   @EventListener
   public void onRenderPlayer(RenderPlayerEvent event) {
      if (event.getEntity() == mc.player && this.rotation != null) {
         event.setYaw(Interpolation.interpolateFloat(this.prevYaw, this.getServerYaw(), mc.getRenderTickCounter().getTickDelta(true)));
         event.setPitch(Interpolation.interpolateFloat(this.prevPitch, this.getServerPitch(), mc.getRenderTickCounter().getTickDelta(true)));
         this.prevYaw = event.getYaw();
         this.prevPitch = event.getPitch();
         event.cancel();
      }

   }

   public void setRotation(Rotation rotation) {
      if (RotationsModule.getInstance().getMouseSensFix()) {
         double fix = Math.pow((Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, (double)3.0F) * 1.2;
         rotation.setYaw((float)((double)rotation.getYaw() - (double)(rotation.getYaw() - this.serverYaw) % fix));
         rotation.setPitch((float)((double)rotation.getPitch() - (double)(rotation.getPitch() - this.serverPitch) % fix));
      }

      if (rotation.getPriority() == Integer.MAX_VALUE) {
         this.rotation = rotation;
      }

      Rotation request = (Rotation)this.requests.stream().filter((r) -> rotation.getPriority() == r.getPriority()).findFirst().orElse(null);
      if (request == null) {
         this.requests.add(rotation);
      } else {
         request.setYaw(rotation.getYaw());
         request.setPitch(rotation.getPitch());
      }

   }

   public void setRotationClient(float yaw, float pitch) {
      if (mc.player != null) {
         mc.player.setYaw(yaw);
         mc.player.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F));
      }
   }

   public void setRotationSilent(float yaw, float pitch) {
      this.setRotation(new Rotation(Integer.MAX_VALUE, yaw, pitch, true));
      Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
   }

   public void setRotationSilentSync() {
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();
      this.setRotation(new Rotation(Integer.MAX_VALUE, yaw, pitch, true));
      Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
   }

   public boolean removeRotation(Rotation request) {
      return this.requests.remove(request);
   }

   public boolean isRotationBlocked(int priority) {
      return this.rotation != null && priority < this.rotation.getPriority();
   }

   public boolean isDoneRotating() {
      return (float)this.rotateTicks > RotationsModule.getInstance().getPreserveTicks();
   }

   public boolean isRotating() {
      return this.rotation != null;
   }

   public float getRotationYaw() {
      return this.rotation.getYaw();
   }

   public float getRotationPitch() {
      return this.rotation.getPitch();
   }

   public float getServerYaw() {
      return this.serverYaw;
   }

   public float getWrappedYaw() {
      return MathHelper.wrapDegrees(this.serverYaw);
   }

   public float getServerPitch() {
      return this.serverPitch;
   }

   private Vec3d movementInputToVelocity(float yaw, Vec3d movementInput, float speed) {
      double d = movementInput.lengthSquared();
      if (d < 1.0E-7) {
         return Vec3d.ZERO;
      } else {
         Vec3d vec3d = (d > (double)1.0F ? movementInput.normalize() : movementInput).multiply((double)speed);
         float f = MathHelper.sin(yaw * ((float)Math.PI / 180F));
         float g = MathHelper.cos(yaw * ((float)Math.PI / 180F));
         return new Vec3d(vec3d.x * (double)g - vec3d.z * (double)f, vec3d.y, vec3d.z * (double)g + vec3d.x * (double)f);
      }
   }

   private Rotation getRotationRequest() {
      Rotation rotationRequest = null;
      int priority = 0;

      for(Rotation request : this.requests) {
         if (request.getPriority() > priority) {
            rotationRequest = request;
            priority = request.getPriority();
         }
      }

      return rotationRequest;
   }
}
