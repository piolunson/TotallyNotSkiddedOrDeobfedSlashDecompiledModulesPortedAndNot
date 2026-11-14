package net.hachimi.client.impl.manager.combat;

import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.exploit.PhaseModule;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.player.RayCastUtil;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;

public class PearlManager implements Globals {
   private float[] lastThrownAngles;
   private Box pearlBB;

   public PearlManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && PhaseModule.getInstance().shouldRaytrace()) {
         Packet var3 = event.getPacket();
         if (var3 instanceof PlayerPositionLookS2CPacket) {
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket)var3;
            if (this.lastThrownAngles != null) {
               BlockHitResult hitResult = (BlockHitResult)RayCastUtil.rayCast((double)3.0F, this.lastThrownAngles);
               this.pearlBB = new Box(hitResult.getPos().subtract(0.4, 0.4, 0.4), hitResult.getPos().add(0.4, 0.4, 0.4));
               if (mc.world.getBlockState(hitResult.getBlockPos()).isAir()) {
                  return;
               }

               if (!this.pearlBB.contains(packet.getX(), packet.getY(), packet.getZ())) {
                  event.cancel();
                  mc.getNetworkHandler().getConnection().send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
                  mc.getNetworkHandler().getConnection().send(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false));
               }

               this.lastThrownAngles = null;
            }
         }

      }
   }

   public void setLastThrownAngles(float[] lastThrownAngles) {
      this.lastThrownAngles = lastThrownAngles;
   }
}
