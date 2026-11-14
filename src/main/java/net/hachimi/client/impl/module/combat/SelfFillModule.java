package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.ObsidianPlacerModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class SelfFillModule extends ObsidianPlacerModule {
   Config<Mode> modeConfig;
   Config<Boolean> attackConfig;
   Config<Boolean> autoDisableConfig;
   private double prevY;

   public SelfFillModule() {
      super("SelfFill", "Fills in the block your standing on", ModuleCategory.COMBAT);
      this.modeConfig = this.register(new EnumConfig("Mode", "The mode for block fill", SelfFillModule.Mode.BLOCK_LAG, SelfFillModule.Mode.values()));
      this.attackConfig = this.register(new BooleanConfig("Attack", "Attacks crystals in the way of block", true, () -> this.modeConfig.getValue() == SelfFillModule.Mode.BLOCK_LAG));
      this.autoDisableConfig = this.register(new BooleanConfig("AutoDisable", "Automatically disables after placing block", false));
   }

   public void onEnable() {
      if (mc.player != null) {
         this.prevY = mc.player.getY();
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (Math.abs(mc.player.getY() - this.prevY) > (double)0.5F) {
         this.disable();
      } else if (!this.checkMultitask() || (Boolean)this.multitaskConfig.getValue()) {
         boolean inBlock = PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream().anyMatch((p) -> !mc.world.getBlockState(p).isReplaceable());
         BlockPos pos = EntityUtil.getRoundedBlockPos(mc.player);
         if (!inBlock && mc.player.isOnGround()) {
            if (this.modeConfig.getValue() == SelfFillModule.Mode.BLOCK_LAG) {
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), true));
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + (double)0.75F, mc.player.getZ(), true));
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), true));
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.16, mc.player.getZ(), true));
               if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset((double)0.0F, 2.34, (double)0.0F))) {
                  return;
               }

               double y = mc.player.getY();
               Managers.POSITION.setPositionClient(mc.player.getX(), y + 1.167, mc.player.getZ());
               this.attackPlace(pos);
               Managers.POSITION.setPositionClient(mc.player.getX(), y, mc.player.getZ());
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.34, mc.player.getZ(), false));
            } else if (this.modeConfig.getValue() == SelfFillModule.Mode.WEB) {
               int slot = this.getBlockItemSlot(Blocks.COBWEB);
               if (slot == -1) {
                  return;
               }

               Managers.INTERACT.placeBlock((BlockPos)pos, slot, (Boolean)this.strictDirectionConfig.getValue(), false, (state, angles) -> {
                  if ((Boolean)this.rotateConfig.getValue()) {
                     if (state) {
                        Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                     } else {
                        Managers.ROTATION.setRotationSilentSync();
                     }
                  }

               });
            }
         }

         if ((Boolean)this.autoDisableConfig.getValue()) {
            this.disable();
         }

      }
   }

   private void attackPlace(BlockPos targetPos) {
      ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
      if (blockSlot != null) {
         int slot = blockSlot.slot();
         if (slot != -1) {
            this.attackPlace(targetPos, slot);
         }
      }
   }

   private void attackPlace(BlockPos targetPos, int slot) {
      if ((Boolean)this.attackConfig.getValue()) {
         Entity entity = (Entity)mc.world.getOtherEntities((Entity)null, new Box(targetPos)).stream().filter((e) -> e instanceof EndCrystalEntity).findFirst().orElse(null);
         if (entity != null) {
            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }
      }

      Managers.INTERACT.placeBlock((BlockPos)targetPos, slot, false, (Boolean)this.strictDirectionConfig.getValue(), false, (state, angles) -> {
         if ((Boolean)this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }

      });
   }

   private static enum Mode {
      BLOCK_LAG,
      WEB;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{BLOCK_LAG, WEB};
      }
   }
}
