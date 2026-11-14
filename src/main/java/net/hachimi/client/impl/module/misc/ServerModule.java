package net.hachimi.client.impl.module.misc;

import java.time.Instant;
import java.util.BitSet;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ConcurrentModule;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.gui.screen.MenuDisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.encryption.NetworkEncryptionUtils.SecureRandomUtil;

public final class ServerModule extends ConcurrentModule {
   Config<Boolean> demoConfig = this.register(new BooleanConfig("NoDemo", "Prevents servers from forcing you to a demo screen", true));
   Config<Boolean> resourcePackConfig = this.register(new BooleanConfig("NoResourcePack", "Prevents server from forcing resource pack", false));
   Config<Boolean> antiCrashConfig = this.register(new BooleanConfig("NoServerCrash", "Prevents server packets from crashing the client", false));
   Config<Boolean> illegalDisconnectConfig = this.register(new BooleanConfig("IllegalDisconnect", "Disconnects by getting kicked from server", false));

   public ServerModule() {
      super("Server", "Prevents servers actions on player", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof GameStateChangeS2CPacket packet) {
         if (packet.getReason() == GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN && !mc.isDemo() && (Boolean)this.demoConfig.getValue()) {
            Hachimi.info("Server attempted to use Demo mode features on you!");
            event.cancel();
         }
      }

      if (event.getPacket() instanceof ResourcePackSendS2CPacket && (Boolean)this.resourcePackConfig.getValue()) {
         event.cancel();
         Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket(mc.player.getUuid(), net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status.DECLINED));
      }

      if (mc.world != null) {
         if ((Boolean)this.antiCrashConfig.getValue()) {
            Packet var6 = event.getPacket();
            if (var6 instanceof PlayerPositionLookS2CPacket) {
               PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket)var6;
               if (packet.getX() > (double)3.0E7F || packet.getY() > (double)mc.world.getTopY() || packet.getZ() > (double)3.0E7F || packet.getX() < (double)-3.0E7F || packet.getY() < (double)mc.world.getBottomY() || packet.getZ() < (double)-3.0E7F) {
                  event.cancel();
                  return;
               }
            }

            var6 = event.getPacket();
            if (var6 instanceof ExplosionS2CPacket) {
               ExplosionS2CPacket packet = (ExplosionS2CPacket)var6;
               if (packet.getX() > (double)3.0E7F || packet.getY() > (double)mc.world.getTopY() || packet.getZ() > (double)3.0E7F || packet.getX() < (double)-3.0E7F || packet.getY() < (double)mc.world.getBottomY() || packet.getZ() < (double)-3.0E7F || packet.getRadius() > 1000.0F || packet.getAffectedBlocks().size() > 1000 || packet.getPlayerVelocityX() > 1000.0F || packet.getPlayerVelocityY() > 1000.0F || packet.getPlayerVelocityZ() > 1000.0F || packet.getPlayerVelocityX() < -1000.0F || packet.getPlayerVelocityY() < -1000.0F || packet.getPlayerVelocityZ() < -1000.0F) {
                  event.cancel();
                  return;
               }
            }

            var6 = event.getPacket();
            if (var6 instanceof EntityVelocityUpdateS2CPacket) {
               EntityVelocityUpdateS2CPacket packet = (EntityVelocityUpdateS2CPacket)var6;
               if (packet.getVelocityX() > (double)1000.0F || packet.getVelocityY() > (double)1000.0F || packet.getVelocityZ() > (double)1000.0F || packet.getVelocityX() < (double)-1000.0F || packet.getVelocityY() < (double)-1000.0F || packet.getVelocityZ() < (double)-1000.0F) {
                  event.cancel();
                  return;
               }
            }

            var6 = event.getPacket();
            if (var6 instanceof ParticleS2CPacket) {
               ParticleS2CPacket packet = (ParticleS2CPacket)var6;
               if (packet.getCount() > 500) {
                  event.cancel();
               }
            }
         }

      }
   }

   @EventListener
   public void onMenuDisconnect(MenuDisconnectEvent event) {
      if ((Boolean)this.illegalDisconnectConfig.getValue()) {
         Managers.NETWORK.sendPacket(new ChatMessageC2SPacket("§", Instant.now(), net.minecraft.network.encryption.NetworkEncryptionUtils.SecureRandomUtil.nextLong(), (MessageSignatureData)null, new net.minecraft.network.message.LastSeenMessageList.Acknowledgment(1, new BitSet(2))));
      }

   }
}
