package net.hachimi.client.impl.module.client;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ConcurrentModule;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.hachimi.client.util.math.position.DirectionUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.world.BlastResistantBlocks;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AnticheatModule extends ConcurrentModule {
   private static AnticheatModule INSTANCE;
   Config<Anticheats> modeConfig;
   Config<PlacementMode> placementConfig;
   Config<SwingMode> entitySwingConfig;
   Config<SwingMode> placeSwingConfig;
   Config<Float> breakDelayConfig;
   Config<AttackMode> attackConfig;
   Config<PlacePacketMode> placeConfig;
   Config<Boolean> multiPlaceConfig;
   Config<Boolean> multiPointConfig;
   Config<Boolean> closestDirectionConfig;
   Config<Boolean> grimAirplaceConfig;
   Config<Boolean> sneakingSpoofConfig;
   Config<Boolean> pickConfig;
   Config<Float> damageConfig;
   Config<Float> entityPlaceConfig;
   Config<Boolean> noScreenCloseConfig;
   Config<Boolean> raytraceSpoofConfig;
   public Config<Boolean> grimRotationConfig;
   public Config<Boolean> snapBackConfig;
   private final Timer raytraceTimer;
   private float pitch;

   public AnticheatModule() {
      super("Anticheat", "Settings for anticheat configs", ModuleCategory.CLIENT);
      this.modeConfig = this.register(new EnumConfig("Mode", "Applies anticheat optimizations", AnticheatModule.Anticheats.GRIM, AnticheatModule.Anticheats.values()));
      this.placementConfig = this.register(new EnumConfig("Placement", "Placement packet mode sent to server", AnticheatModule.PlacementMode.Grim, AnticheatModule.PlacementMode.values()));
      this.entitySwingConfig = this.register(new EnumConfig("EntitySwing", "Swing mode when attacking entities", AnticheatModule.SwingMode.Normal, AnticheatModule.SwingMode.values()));
      this.placeSwingConfig = this.register(new EnumConfig("PlaceSwing", "Swing mode when placing blocks", AnticheatModule.SwingMode.None, AnticheatModule.SwingMode.values()));
      this.breakDelayConfig = this.register(new NumberConfig("BreakDelay", "Delay in ticks between block breaks", 0.0F, 1.0F, 10.0F));
      this.attackConfig = this.register(new EnumConfig("Attack", "How to send attack packets to the server", AnticheatModule.AttackMode.Normal, AnticheatModule.AttackMode.values()));
      this.placeConfig = this.register(new EnumConfig("Place", "How to send place packets to the server", AnticheatModule.PlacePacketMode.Packet, AnticheatModule.PlacePacketMode.values()));
      this.multiPlaceConfig = this.register(new BooleanConfig("MultiPlace", "Allows placing multiple blocks per tick", false));
      this.multiPointConfig = this.register(new BooleanConfig("MultiPoint", "Sends multiple hit vectors when placing to bypass raytrace", false));
      this.closestDirectionConfig = this.register(new BooleanConfig("ClosestDirection", "Uses closest valid direction when placing blocks", false));
      this.grimAirplaceConfig = this.register(new BooleanConfig("GrimAirplace", "Bypasses Grim airplace check using offhand swap trick", false));
      this.sneakingSpoofConfig = this.register(new BooleanConfig("SneakingSpoof", "Spoofs sneaking state when placing to bypass block interaction checks", false));
      this.pickConfig = this.register(new BooleanConfig("Pick", "Cancels PickFromInventory packets to prevent Grim flags", false));
      this.damageConfig = this.register(new NumberConfig("Damage", "Damage multiplier sent to server for Grim consistency", 0.0F, 0.7F, 1.0F));
      this.entityPlaceConfig = this.register(new NumberConfig("PlaceThreshold", "Max ticks to place on entities", 0.0F, 2.0F, 25.0F, () -> this.modeConfig.getValue() != AnticheatModule.Anticheats.VANILLA));
      this.noScreenCloseConfig = this.register(new BooleanConfig("NoScreenClose", "Prevents the server from closing your inventory screen", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM));
      this.raytraceSpoofConfig = this.register(new BooleanConfig("RaytraceFix", "Allows you to spoof your raytrace", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.N_C_P));
      this.grimRotationConfig = this.register(new BooleanConfig("GrimRotation", "Sends Full movement packet when rotating to bypass GrimAC rotation check", true, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM));
      this.snapBackConfig = this.register(new BooleanConfig("SnapBack", "Sends a snap-back rotation packet after placing/attacking to restore real look direction", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM));
      this.raytraceTimer = new CacheTimer();
      this.pitch = Float.NaN;
      INSTANCE = this;
   }

   public static AnticheatModule getInstance() {
      return INSTANCE;
   }

   @EventListener(
      priority = 10000
   )
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null) {
         if (this.isNCP() && (Boolean)this.raytraceSpoofConfig.getValue()) {
            Packet var3 = event.getPacket();
            if (var3 instanceof PlayerInteractBlockC2SPacket) {
               PlayerInteractBlockC2SPacket packet = (PlayerInteractBlockC2SPacket)var3;
               if (this.raytraceTimer.passed(250)) {
                  BlockHitResult packetResult = packet.getBlockHitResult();
                  BlockPos pos = packetResult.getBlockPos();
                  BlockHitResult result = mc.world.raycast(new net.minecraft.world.RaycastContext(mc.player.getEyePos(), DirectionUtil.getDirectionOffsetPos(pos, packetResult.getSide()), net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
                  if (mc.world.isSpaceEmpty(mc.player.getBoundingBox().stretch((double)0.0F, 0.15, (double)0.0F)) && result != null && result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK && !result.getBlockPos().equals(pos)) {
                     this.pitch = -75.0F;
                     this.raytraceTimer.reset();
                  }
               }
            }
         }

         Packet var9 = event.getPacket();
         if (var9 instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket)var9;
            if (packet.changesLook() && !Float.isNaN(this.pitch)) {
               ((AccessorPlayerMoveC2SPacket)(Object)packet).hookSetPitch(this.pitch);
               this.pitch = Float.NaN;
            }
         }

         if (this.isGrim()) {
            var9 = event.getPacket();
            if (var9 instanceof net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket) {
               net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket packet = (net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket)var9;
               if (packet.getAction() == net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK || packet.getAction() == net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK || packet.getAction() == net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK) {
                  if (BlastResistantBlocks.isUnbreakable(packet.getPos())) {
                     event.cancel();
                     return;
                  }

                  if (packet.getAction() == net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                     Managers.NETWORK.sendQuietPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                     Managers.NETWORK.sendQuietPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                     Managers.NETWORK.sendQuietPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                     event.cancel();
                  }
               }
            }
         }

      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (this.isGrim() && (Boolean)this.noScreenCloseConfig.getValue()) {
            Packet var3 = event.getPacket();
            if (var3 instanceof CloseScreenS2CPacket) {
               CloseScreenS2CPacket packet = (CloseScreenS2CPacket)var3;
               if (packet.getSyncId() == 0) {
                  event.cancel();
               }
            }
         }

      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && event.getConfig() == this.raytraceSpoofConfig && !(Boolean)this.raytraceSpoofConfig.getValue()) {
         this.pitch = Float.NaN;
      }

   }

   public boolean isGrim() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM;
   }

   public boolean isNCP() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.N_C_P;
   }

   public boolean getMiningFix() {
      return this.isGrim();
   }

   public boolean getWebJumpFix() {
      return false;
   }

   public int getEntityPlaceThreshold() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.VANILLA ? 0 : Math.round((Float)this.entityPlaceConfig.getValue() * 10.0F);
   }

   public PlacementMode getPlacementMode() {
      return this.placementConfig.getValue();
   }

   public SwingMode getEntitySwing() {
      return this.entitySwingConfig.getValue();
   }

   public SwingMode getPlaceSwing() {
      return this.placeSwingConfig.getValue();
   }

   public float getBreakDelayMs() {
      return (Float)this.breakDelayConfig.getValue() * 50.0F;
   }

   public AttackMode getAttackMode() {
      return this.attackConfig.getValue();
   }

   public PlacePacketMode getPlacePacketMode() {
      return this.placeConfig.getValue();
   }

   public boolean isMultiPlace() {
      return (Boolean)this.multiPlaceConfig.getValue();
   }

   public boolean isMultiPoint() {
      return (Boolean)this.multiPointConfig.getValue();
   }

   public boolean isClosestDirection() {
      return (Boolean)this.closestDirectionConfig.getValue();
   }

   public boolean isGrimAirplace() {
      return (Boolean)this.grimAirplaceConfig.getValue();
   }

   public boolean isSneakingSpoof() {
      return (Boolean)this.sneakingSpoofConfig.getValue();
   }

   public boolean isPick() {
      return (Boolean)this.pickConfig.getValue();
   }

   public float getDamageMultiplier() {
      return (Float)this.damageConfig.getValue();
   }

   public boolean isGrimRotation() {
      return (Boolean)this.grimRotationConfig.getValue();
   }

   public boolean isSnapBack() {
      return (Boolean)this.snapBackConfig.getValue();
   }

   private static enum Anticheats {
      GRIM,
      N_C_P,
      VANILLA;

      // $FF: synthetic method
      private static Anticheats[] $values() {
         return new Anticheats[]{GRIM, N_C_P, VANILLA};
      }
   }

   public static enum PlacementMode {
      Grim,
      NCP,
      Normal;

      // $FF: synthetic method
      private static PlacementMode[] $values() {
         return new PlacementMode[]{Grim, NCP, Normal};
      }
   }

   public static enum SwingMode {
      Normal,
      None,
      Packet;

      // $FF: synthetic method
      private static SwingMode[] $values() {
         return new SwingMode[]{Normal, None, Packet};
      }
   }

   public static enum AttackMode {
      Normal,
      Packet;

      // $FF: synthetic method
      private static AttackMode[] $values() {
         return new AttackMode[]{Normal, Packet};
      }
   }

   public static enum PlacePacketMode {
      Normal,
      Packet;

      // $FF: synthetic method
      private static PlacePacketMode[] $values() {
         return new PlacePacketMode[]{Normal, Packet};
      }
   }
}
