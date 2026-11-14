package net.hachimi.client.impl.module.movement;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.player.PushEntityEvent;
import net.hachimi.client.impl.event.entity.player.PushFluidsEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.network.PushOutOfBlocksEvent;
import net.hachimi.client.impl.module.combat.SurroundModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorBundlePacket;
import net.hachimi.client.mixin.accessor.AccessorClientWorld;
import net.hachimi.client.mixin.accessor.AccessorEntityVelocityUpdateS2CPacket;
import net.hachimi.client.mixin.accessor.AccessorExplosionS2CPacket;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class VelocityModule extends ToggleModule {
   private static VelocityModule INSTANCE;
   Config<Boolean> knockbackConfig = this.register(new BooleanConfig("Knockback", "Removes player knockback velocity", true));
   Config<Boolean> explosionConfig = this.register(new BooleanConfig("Explosion", "Removes player explosion velocity", true));
   Config<VelocityMode> modeConfig;
   Config<Float> horizontalConfig;
   Config<Float> verticalConfig;
   Config<Boolean> concealConfig;
   Config<Boolean> wallsAirConfig;
   Config<Boolean> wallsTrappedConfig;
   Config<Boolean> pushEntitiesConfig;
   Config<Boolean> pushBlocksConfig;
   Config<Boolean> pushLiquidsConfig;
   Config<Boolean> pushFishhookConfig;
   private boolean cancelVelocity;
   private boolean concealVelocity;

   public VelocityModule() {
      super("Velocity", "Reduces the amount of player knockback velocity", ModuleCategory.MOVEMENT);
      this.modeConfig = this.register(new EnumConfig("Mode", "The mode for velocity", VelocityModule.VelocityMode.NORMAL, VelocityModule.VelocityMode.values()));
      this.horizontalConfig = this.register(new NumberConfig("Horizontal", "How much horizontal knock-back to take", 0.0F, 0.0F, 100.0F, NumberDisplay.PERCENT, () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL || this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS));
      this.verticalConfig = this.register(new NumberConfig("Vertical", "How much vertical knock-back to take", 0.0F, 0.0F, 100.0F, NumberDisplay.PERCENT, () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL || this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS));
      this.concealConfig = this.register(new BooleanConfig("Conceal", "Fixes velocity on servers with excessive setbacks", false));
      this.wallsAirConfig = this.register(new BooleanConfig("GroundOnly", "Only applies velocity in walls while on ground", false, () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS));
      this.wallsTrappedConfig = this.register(new BooleanConfig("Trapped", "Applies velocity while player head is trapped", false, () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS));
      this.pushEntitiesConfig = this.register(new BooleanConfig("NoPush-Entities", "Prevents being pushed away from entities", true));
      this.pushBlocksConfig = this.register(new BooleanConfig("NoPush-Blocks", "Prevents being pushed out of blocks", true));
      this.pushLiquidsConfig = this.register(new BooleanConfig("NoPush-Liquids", "Prevents being pushed by flowing liquids", true));
      this.pushFishhookConfig = this.register(new BooleanConfig("NoPush-Fishhook", "Prevents being pulled by fishing rod hooks", true));
      INSTANCE = this;
   }

   public static VelocityModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      if (this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL) {
         DecimalFormat decimal = new DecimalFormat("0.0");
         return String.format("H:%s%%, V:%s%%", decimal.format(this.horizontalConfig.getValue()), decimal.format(this.verticalConfig.getValue()));
      } else {
         return EnumFormatter.formatEnum(this.modeConfig.getValue());
      }
   }

   public void onEnable() {
      this.cancelVelocity = false;
   }

   public void onDisable() {
      if (this.cancelVelocity) {
         if (this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM) {
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            if (Managers.ROTATION.isRotating()) {
               yaw = Managers.ROTATION.getRotationYaw();
               pitch = Managers.ROTATION.getRotationPitch();
            }

            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN));
         }

         this.cancelVelocity = false;
      }

      this.concealVelocity = false;
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof PlayerPositionLookS2CPacket && (Boolean)this.concealConfig.getValue()) {
            this.concealVelocity = true;
         }

         Packet allowedBundle = event.getPacket();
         if (allowedBundle instanceof EntityVelocityUpdateS2CPacket) {
            EntityVelocityUpdateS2CPacket packet = (EntityVelocityUpdateS2CPacket)allowedBundle;
            if ((Boolean)this.knockbackConfig.getValue()) {
               if (packet.getEntityId() != mc.player.getId()) {
                  return;
               }

               if (this.concealVelocity && packet.getVelocityX() == (double)0.0F && packet.getVelocityZ() == (double)0.0F && packet.getVelocityZ() == (double)0.0F) {
                  this.concealVelocity = false;
                  return;
               }

               if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS) {
                  if (!this.isPhased() && (!(Boolean)this.wallsTrappedConfig.getValue() || !this.isWallsTrapped())) {
                     return;
                  }

                  if ((Boolean)this.wallsAirConfig.getValue() && !Managers.POSITION.isOnGround()) {
                     return;
                  }
               }

               switch (((VelocityMode)this.modeConfig.getValue()).ordinal()) {
                  case 0:
                  case 1:
                     if ((Float)this.horizontalConfig.getValue() == 0.0F && (Float)this.verticalConfig.getValue() == 0.0F) {
                        event.cancel();
                        return;
                     }

                     ((AccessorEntityVelocityUpdateS2CPacket)(Object)packet).setVelocityX((int)(packet.getVelocityX() * (double)((Float)this.horizontalConfig.getValue() / 100.0F)));
                     ((AccessorEntityVelocityUpdateS2CPacket)(Object)packet).setVelocityY((int)(packet.getVelocityY() * (double)((Float)this.verticalConfig.getValue() / 100.0F)));
                     ((AccessorEntityVelocityUpdateS2CPacket)(Object)packet).setVelocityZ((int)(packet.getVelocityZ() * (double)((Float)this.horizontalConfig.getValue() / 100.0F)));
                     return;
                  case 2:
                     if (!Managers.ANTICHEAT.hasPassed(100L)) {
                        return;
                     }

                     event.cancel();
                     this.cancelVelocity = true;
                     return;
                  case 3:
                     event.setCanceled(this.isPhased());
                     return;
                  default:
                     return;
               }
            }
         }

         allowedBundle = event.getPacket();
         if (allowedBundle instanceof ExplosionS2CPacket) {
            ExplosionS2CPacket packet = (ExplosionS2CPacket)allowedBundle;
            if ((Boolean)this.explosionConfig.getValue()) {
               if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS && !this.isPhased()) {
                  return;
               }

               switch (((VelocityMode)this.modeConfig.getValue()).ordinal()) {
                  case 0:
                  case 1:
                     if ((Float)this.horizontalConfig.getValue() == 0.0F && (Float)this.verticalConfig.getValue() == 0.0F) {
                        event.cancel();
                        break;
                     }

                     ((AccessorExplosionS2CPacket)(Object)packet).setPlayerVelocityX(packet.getPlayerVelocityX() * ((Float)this.horizontalConfig.getValue() / 100.0F));
                     ((AccessorExplosionS2CPacket)(Object)packet).setPlayerVelocityY(packet.getPlayerVelocityY() * ((Float)this.verticalConfig.getValue() / 100.0F));
                     ((AccessorExplosionS2CPacket)(Object)packet).setPlayerVelocityZ(packet.getPlayerVelocityZ() * ((Float)this.horizontalConfig.getValue() / 100.0F));
                     break;
                  case 2:
                     if (!Managers.ANTICHEAT.hasPassed(100L)) {
                        return;
                     }

                     event.cancel();
                     this.cancelVelocity = true;
                     break;
                  case 3:
                     event.setCanceled(this.isPhased());
               }

               if (event.isCanceled()) {
                  mc.executeSync(() -> ((AccessorClientWorld)(Object)mc.world).hookPlaySound(packet.getX(), packet.getY(), packet.getZ(), (SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 4.0F, (1.0F + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2F) * 0.7F, false, RANDOM.nextLong()));
               }

               return;
            }
         }

         allowedBundle = event.getPacket();
         if (!(allowedBundle instanceof BundleS2CPacket)) {
            allowedBundle = event.getPacket();
            if (allowedBundle instanceof EntityDamageS2CPacket) {
               EntityDamageS2CPacket packet = (EntityDamageS2CPacket)allowedBundle;
               if (packet.entityId() == mc.player.getId() && this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM_V3 && this.isPhased()) {
                  Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly(false));
                  Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly(true));
                  return;
               }
            }

            allowedBundle = event.getPacket();
            if (allowedBundle instanceof EntityStatusS2CPacket) {
               EntityStatusS2CPacket packet = (EntityStatusS2CPacket)allowedBundle;
               if (packet.getStatus() == 31 && (Boolean)this.pushFishhookConfig.getValue()) {
                  Entity entity = packet.getEntity(mc.world);
                  if (entity instanceof FishingBobberEntity) {
                     FishingBobberEntity hook = (FishingBobberEntity)entity;
                     if (hook.getHookedEntity() == mc.player) {
                        event.cancel();
                     }
                  }
               }
            }
         } else {
            BundleS2CPacket packet = (BundleS2CPacket)allowedBundle;
            List<Packet<?>> filteredPackets = new ArrayList<>();

            for(Packet<?> packet1 : packet.getPackets()) {
               label209: {
                  if (packet1 instanceof ExplosionS2CPacket) {
                     ExplosionS2CPacket packet2 = (ExplosionS2CPacket)packet1;
                     if ((Boolean)this.explosionConfig.getValue()) {
                        mc.executeSync(() -> ((AccessorClientWorld)(Object)mc.world).hookPlaySound(packet2.getX(), packet2.getY(), packet2.getZ(), (SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 4.0F, (1.0F + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2F) * 0.7F, false, RANDOM.nextLong()));
                        if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS && !this.isPhased()) {
                           filteredPackets.add(packet1);
                           continue;
                        }

                        switch (((VelocityMode)this.modeConfig.getValue()).ordinal()) {
                           case 0:
                           case 1:
                              if ((Float)this.horizontalConfig.getValue() == 0.0F && (Float)this.verticalConfig.getValue() == 0.0F) {
                                 continue;
                              }

                              ((AccessorExplosionS2CPacket)(Object)packet2).setPlayerVelocityX(packet2.getPlayerVelocityX() * ((Float)this.horizontalConfig.getValue() / 100.0F));
                              ((AccessorExplosionS2CPacket)(Object)packet2).setPlayerVelocityY(packet2.getPlayerVelocityY() * ((Float)this.verticalConfig.getValue() / 100.0F));
                              ((AccessorExplosionS2CPacket)(Object)packet2).setPlayerVelocityZ(packet2.getPlayerVelocityZ() * ((Float)this.horizontalConfig.getValue() / 100.0F));
                              break label209;
                           case 2:
                              if (Managers.ANTICHEAT.hasPassed(100L)) {
                                 filteredPackets.add(packet1);
                              } else {
                                 this.cancelVelocity = true;
                              }
                              continue;
                           case 3:
                              if (this.isPhased()) {
                                 continue;
                              }
                           default:
                              break label209;
                        }
                     }
                  }

                  if (packet1 instanceof EntityVelocityUpdateS2CPacket) {
                     EntityVelocityUpdateS2CPacket packet2 = (EntityVelocityUpdateS2CPacket)packet1;
                     if ((Boolean)this.knockbackConfig.getValue()) {
                        if (packet2.getEntityId() != mc.player.getId()) {
                           filteredPackets.add(packet1);
                           continue;
                        }

                        if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS) {
                           if (!this.isPhased() && (!(Boolean)this.wallsTrappedConfig.getValue() || !this.isWallsTrapped())) {
                              filteredPackets.add(packet1);
                              return;
                           }

                           if ((Boolean)this.wallsAirConfig.getValue() && !Managers.POSITION.isOnGround()) {
                              filteredPackets.add(packet1);
                              continue;
                           }
                        }

                        switch (((VelocityMode)this.modeConfig.getValue()).ordinal()) {
                           case 0:
                           case 1:
                              if ((Float)this.horizontalConfig.getValue() == 0.0F && (Float)this.verticalConfig.getValue() == 0.0F) {
                                 continue;
                              }

                              ((AccessorEntityVelocityUpdateS2CPacket)(Object)packet2).setVelocityX((int)(packet2.getVelocityX() * (double)((Float)this.horizontalConfig.getValue() / 100.0F)));
                              ((AccessorEntityVelocityUpdateS2CPacket)(Object)packet2).setVelocityY((int)(packet2.getVelocityY() * (double)((Float)this.verticalConfig.getValue() / 100.0F)));
                              ((AccessorEntityVelocityUpdateS2CPacket)(Object)packet2).setVelocityZ((int)(packet2.getVelocityZ() * (double)((Float)this.horizontalConfig.getValue() / 100.0F)));
                              break;
                           case 2:
                              if (!Managers.ANTICHEAT.hasPassed(100L)) {
                                 filteredPackets.add(packet1);
                              } else {
                                 this.cancelVelocity = true;
                              }
                              continue;
                           case 3:
                              if (this.isPhased()) {
                                 continue;
                              }
                        }
                     }
                  }
               }

               filteredPackets.add(packet1);
            }

            ((AccessorBundlePacket)(Object)packet).setIterable(filteredPackets);
         }

      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      this.concealVelocity = false;
      if (this.cancelVelocity) {
         if (this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM) {
            float yaw = Managers.ROTATION.getServerYaw();
            float pitch = Managers.ROTATION.getServerPitch();
            if (Managers.ROTATION.isRotating()) {
               yaw = Managers.ROTATION.getRotationYaw();
               pitch = Managers.ROTATION.getRotationPitch();
            }

            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN));
         }

         this.cancelVelocity = false;
      }

   }

   @EventListener
   public void onPushEntity(PushEntityEvent event) {
      if ((Boolean)this.pushEntitiesConfig.getValue() && event.getPushed().equals(mc.player)) {
         event.cancel();
      }

   }

   @EventListener
   public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
      if ((Boolean)this.pushBlocksConfig.getValue()) {
         event.cancel();
      }

   }

   @EventListener
   public void onPushFluid(PushFluidsEvent event) {
      if ((Boolean)this.pushLiquidsConfig.getValue()) {
         event.cancel();
      }

   }

   private boolean isWallsTrapped() {
      BlockPos headPos = mc.player.getBlockPos().up(mc.player.isCrawling() ? 1 : 2);
      return mc.world.getBlockState(headPos).isReplaceable() ? false : SurroundModule.getInstance().getSurroundNoDown(mc.player).stream().noneMatch((blockPos) -> mc.world.getBlockState(mc.player.isCrawling() ? blockPos : blockPos.up()).isReplaceable());
   }

   private boolean isPhased() {
      return PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream().anyMatch((blockPos) -> !mc.world.getBlockState(blockPos).isReplaceable());
   }

   private static enum VelocityMode {
      NORMAL,
      WALLS,
      GRIM,
      GRIM_V3;

      // $FF: synthetic method
      private static VelocityMode[] $values() {
         return new VelocityMode[]{NORMAL, WALLS, GRIM, GRIM_V3};
      }
   }
}
