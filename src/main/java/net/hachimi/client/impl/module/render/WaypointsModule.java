package net.hachimi.client.impl.module.render;

import com.google.common.collect.Maps;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.api.waypoint.Waypoint;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.world.LoadWorldEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.world.DimensionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;

public class WaypointsModule extends ToggleModule {
   private static WaypointsModule INSTANCE;
   Config<Boolean> logoutsConfig = this.register(new BooleanConfig("LogoutPoints", "Marks the position of player logouts", false));
   Config<Boolean> deathsConfig = this.register(new BooleanConfig("DeathPoints", "Marks the position of player deaths", false));
   Config<Boolean> coordsConfig = this.register(new BooleanConfig("Coords", "Shows the coordinates of the waypoint", true));
   Config<Boolean> distanceConfig = this.register(new BooleanConfig("Distance", "Shows the distance to the waypoint", true));
   DecimalFormat format = new DecimalFormat("0.0");
   private final Map<UUID, PlayerEntity> loginPlayers = Maps.newConcurrentMap();
   private final Map<UUID, PlayerEntity> logoutPlayers = Maps.newConcurrentMap();

   public WaypointsModule() {
      super("Waypoints", "Renders a waypoint at marked locations", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static WaypointsModule getInstance() {
      return INSTANCE;
   }

   public void onDisable() {
      Managers.WAYPOINT.clear();
      this.loginPlayers.clear();
      this.logoutPlayers.clear();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.onDisable();
   }

   @EventListener
   public void onLoadWorld(LoadWorldEvent event) {
      this.onDisable();
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && (Boolean)this.logoutsConfig.getValue()) {
         Packet var3 = event.getPacket();
         if (var3 instanceof BundleS2CPacket) {
            BundleS2CPacket packet = (BundleS2CPacket)var3;

            for(Packet<?> packet1 : packet.getPackets()) {
               this.handlePlayerListPackets(packet1);
            }
         } else {
            this.handlePlayerListPackets(event.getPacket());
         }

      }
   }

   public void handlePlayerListPackets(Packet<?> packet1) {
      if (packet1 instanceof net.minecraft.network.packet.s2c.play.PlayerListS2CPacket packet) {
         if (packet.getActions().contains(net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action.ADD_PLAYER)) {
            for(net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry player : packet.getPlayerAdditionEntries()) {
               for(UUID uuid : this.logoutPlayers.keySet()) {
                  if (uuid.equals(player.profile().getId())) {
                     this.logoutPlayers.remove(uuid);
                  }
               }
            }

            this.loginPlayers.clear();
            return;
         }
      }

      if (packet1 instanceof PlayerRemoveS2CPacket packet) {
         for(UUID uuid2 : packet.profileIds()) {
            for(UUID uuid : this.loginPlayers.keySet()) {
               if (uuid.equals(uuid2)) {
                  PlayerEntity player = (PlayerEntity)this.loginPlayers.get(uuid);
                  if (!this.logoutPlayers.containsKey(uuid)) {
                     this.logoutPlayers.put(uuid, player);
                  }
               }
            }
         }

         this.loginPlayers.clear();
      }

   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST) {
         for(PlayerEntity player : mc.world.getPlayers()) {
            if (player != null && !player.equals(mc.player)) {
               this.loginPlayers.put(player.getGameProfile().getId(), player);
            }
         }

      }
   }

   @EventListener
   public void onRemoveEntity(EntityDeathEvent event) {
      if (event.getEntity() instanceof ClientPlayerEntity && (Boolean)this.deathsConfig.getValue()) {
         String serverIp = mc.isInSingleplayer() ? "Singleplayer" : Managers.NETWORK.getServerIp();
         Managers.WAYPOINT.removeContains("Last Death");
         Managers.WAYPOINT.register(new Waypoint("Last Death", serverIp, DimensionUtil.getDimension(), mc.player.getX(), mc.player.getY(), mc.player.getZ()));
      }

   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && mc.world != null) {
         RenderBuffers.preRender();

         for(Waypoint waypoint : Managers.WAYPOINT.getWaypoints()) {
            if (waypoint.getIp().equalsIgnoreCase(mc.isInSingleplayer() ? "Singleplayer" : Managers.NETWORK.getServerIp()) && DimensionUtil.getDimension() == waypoint.getDimension()) {
               this.renderWaypointBox(event.getMatrices(), waypoint.getPos(), waypoint.getName());
            }
         }

         if ((Boolean)this.logoutsConfig.getValue()) {
            for(UUID uuid : this.logoutPlayers.keySet()) {
               PlayerEntity data = (PlayerEntity)this.logoutPlayers.get(uuid);
               if (data != null) {
                  this.renderWaypointBox(event.getMatrices(), data.getPos(), data.getName().getString() + "'s Logout", data.isCrawling());
               }
            }
         }

         RenderBuffers.postRender();
      }
   }

   private void renderWaypointBox(MatrixStack matrixStack, Vec3d pos, String tag) {
      this.renderWaypointBox(matrixStack, pos, tag, false);
   }

   private void renderWaypointBox(MatrixStack matrixStack, Vec3d pos, String tag, boolean crawlHitbox) {
      Box waypointBox = crawlHitbox ? EntityDimensions.changing(0.6F, 0.6F).withEyeHeight(0.4F).getBoxAt(pos) : EntityDimensions.fixed(0.6F, 2.2F).getBoxAt(pos);
      double center = (waypointBox.maxX - waypointBox.minX) / (double)2.0F;
      RenderManager.renderBoundingBox(matrixStack, waypointBox, 1.5F, ColorsModule.getInstance().getRGB(255));
      int dist = (int)Math.sqrt(mc.player.squaredDistanceTo(pos));
      String waypointTag = "§7" + tag + ((Boolean)this.coordsConfig.getValue() ? String.format(" XYZ %s %s %s", this.format.format(pos.getX()), this.format.format(pos.getY()), this.format.format(pos.getZ())) : "") + ((Boolean)this.distanceConfig.getValue() ? String.format(" %sm", dist) : "");
      RenderManager.renderSign(waypointTag, waypointBox.minX + center, waypointBox.maxY + 0.4, waypointBox.minZ + center, -1);
   }
}
