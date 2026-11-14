package net.hachimi.client.impl.module.misc;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.network.GameJoinEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.world.AddEntityEvent;
import net.hachimi.client.impl.event.world.RemoveEntityEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;

public final class ChatNotifierModule extends ToggleModule {
   Config<Boolean> totemPopConfig = this.register(new BooleanConfig("TotemPop", "Notifies in chat when a player pops a totem", true));
   Config<Boolean> visualRangeConfig = this.register(new BooleanConfig("VisualRange", "Notifies in chat when player enters visual range", false));
   Config<Boolean> joinConfig = this.register(new BooleanConfig("Join", "Notifies in chat when a player joins", false));
   Config<Boolean> leaveConfig = this.register(new BooleanConfig("Leave", "Notifies in chat when a player leaves", false));
   Config<Boolean> friendsConfig = this.register(new BooleanConfig("Friends", "Notifies for friends", false));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Notifies you if the server you join is running Grim", false));

   public ChatNotifierModule() {
      super("ChatNotifier", "Notifies in chat", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         Packet rawPacket = event.getPacket();
         if (rawPacket instanceof EntityStatusS2CPacket) {
            EntityStatusS2CPacket packet = (EntityStatusS2CPacket)rawPacket;
            if (packet.getStatus() == 35 && (Boolean)this.totemPopConfig.getValue()) {
               Entity poppedEntity = packet.getEntity(mc.world);
               if (!(poppedEntity instanceof LivingEntity)) {
                  return;
               }

               int totems = Managers.TOTEM.getTotems(poppedEntity);
               String playerName = poppedEntity.getName().getString();
               boolean isFriend = Managers.SOCIAL.isFriend(playerName);
               if (isFriend && !(Boolean)this.friendsConfig.getValue() || poppedEntity == mc.player) {
                  return;
               }

               ChatUtil.clientSendMessage((isFriend ? "§g" : "§7") + playerName + "§f popped §s" + totems + "§f totems", poppedEntity.hashCode());
            }
         }

         Packet entityPacket = event.getPacket();
         if (entityPacket instanceof net.minecraft.network.packet.s2c.play.PlayerListS2CPacket) {
            net.minecraft.network.packet.s2c.play.PlayerListS2CPacket packet = (net.minecraft.network.packet.s2c.play.PlayerListS2CPacket)entityPacket;
            if ((Boolean)this.joinConfig.getValue()) {
               if (!packet.getActions().contains(net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action.ADD_PLAYER) || mc.player.age < 20) {
                  return;
               }

               packet.getEntries().stream().filter((data) -> data != null && data.profile() != null).filter((data) -> data.profile().getName() != null && !data.profile().getName().isEmpty() || data.profile().getId() != null).forEach((data) -> {
                  String name = data.profile().getName();
                  if (name != null) {
                     boolean isFriend = Managers.SOCIAL.isFriend(name);
                     if (!(Boolean)this.friendsConfig.getValue() && isFriend) {
                        return;
                     }

                     ChatUtil.clientSendMessage((isFriend ? "§g" : "§7") + name + "§f has joined the server", name.hashCode());
                  }

               });
            }
         }

         Packet leavePacket = event.getPacket();
         if (leavePacket instanceof PlayerRemoveS2CPacket) {
            PlayerRemoveS2CPacket packet = (PlayerRemoveS2CPacket)leavePacket;
            if ((Boolean)this.leaveConfig.getValue()) {
               if (mc.getNetworkHandler() == null) {
                  return;
               }

               for(UUID uuid : packet.profileIds()) {
                  String name = null;

                  for(PlayerListEntry info : new ArrayList<>(mc.getNetworkHandler().getPlayerList())) {
                     if (info != null) {
                        GameProfile gameProfile = info.getProfile();
                        if (gameProfile.getId().equals(uuid)) {
                           name = gameProfile.getName();
                        }
                     }
                  }

                  if (name != null) {
                     boolean isFriend = Managers.SOCIAL.isFriend(name);
                     if (!(Boolean)this.friendsConfig.getValue() && isFriend) {
                        return;
                     }

                     ChatUtil.clientSendMessage((isFriend ? "§g" : "§7") + name + "§f has left the server", name.hashCode());
                  }
               }
            }
         }

      }
   }

   @EventListener
   public void onAddEntity(AddEntityEvent event) {
      if ((Boolean)this.visualRangeConfig.getValue()) {
         Entity var3 = event.getEntity();
         if (var3 instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)var3;
            if (!(event.getEntity() instanceof FakePlayerEntity)) {
               String playerName = event.getEntity().getName().getString();
               boolean isFriend = Managers.SOCIAL.isFriend(playerName);
               if ((!isFriend || (Boolean)this.friendsConfig.getValue()) && event.getEntity() != mc.player && !playerName.equalsIgnoreCase("ShaderPlayer")) {
                  mc.executeSync(() -> ChatUtil.clientSendMessageRaw("§s[VisualRange] " + (isFriend ? "§g" : "§7") + playerName + "§f entered your visual range", playerEntity.hashCode()));
                  return;
               }

               return;
            }
         }
      }

   }

   @EventListener
   public void onRemoveEntity(RemoveEntityEvent event) {
      if ((Boolean)this.visualRangeConfig.getValue()) {
         Entity var3 = event.getEntity();
         if (var3 instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)var3;
            if (!(event.getEntity() instanceof FakePlayerEntity)) {
               String playerName = event.getEntity().getName().getString();
               boolean isFriend = Managers.SOCIAL.isFriend(playerName);
               if ((!isFriend || (Boolean)this.friendsConfig.getValue()) && event.getEntity() != mc.player && !playerName.equalsIgnoreCase("ShaderPlayer")) {
                  mc.executeSync(() -> ChatUtil.clientSendMessageRaw("§s[VisualRange] " + (isFriend ? "§g" : "§c") + playerName + "§f left your visual range", playerEntity.hashCode()));
                  return;
               }

               return;
            }
         }
      }

   }

   @EventListener
   public void onEntityDeath(EntityDeathEvent event) {
      if ((Boolean)this.totemPopConfig.getValue()) {
         int totems = Managers.TOTEM.getTotems(event.getEntity());
         if (totems != 0) {
            String playerName = event.getEntity().getName().getString();
            boolean isFriend = Managers.SOCIAL.isFriend(playerName);
            if ((!isFriend || (Boolean)this.friendsConfig.getValue()) && event.getEntity() != mc.player) {
               ChatUtil.clientSendMessage((isFriend ? "§g" : "§7") + playerName + "§f died after popping §s" + totems + "§f totems", event.getEntity().hashCode());
            }
         }
      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      if ((Boolean)this.grimConfig.getValue() && !mc.isInSingleplayer()) {
         if (Managers.ANTICHEAT.isGrim()) {
            ChatUtil.clientSendMessage("This server is running GrimAC", 102);
         } else {
            ChatUtil.clientSendMessage("This server is not running GrimAC", 102);
         }
      }

   }
}
