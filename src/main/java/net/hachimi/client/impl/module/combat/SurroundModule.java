package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.ObsidianPlacerModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.player.AirPlaceModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.world.BlastResistantBlocks;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.block.ShapeContext;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;

public class SurroundModule extends ObsidianPlacerModule {
   private static SurroundModule INSTANCE;
   Config<Timing> timingConfig;
   Config<Boolean> prePlaceExplosionConfig;
   Config<Boolean> prePlaceTickConfig;
   Config<Float> placeRangeConfig;
   Config<Boolean> attackConfig;
   Config<Boolean> extendConfig;
   Config<Boolean> headConfig;
   Config<Boolean> mineExtendConfig;
   Config<Boolean> supportConfig;
   Config<Integer> shiftTicksConfig;
   Config<Float> shiftDelayConfig;
   Config<Boolean> jumpDisableConfig;
   Config<Boolean> renderConfig;
   Config<Integer> fadeTimeConfig;
   private int blocksPlaced;
   private List<BlockPos> surround;
   private List<BlockPos> placements;
   private final Map<BlockPos, Long> packets;
   private final Map<BlockPos, Animation> fadeList;
   private double prevY;

   public SurroundModule() {
      super("Surround", "Surrounds feet with obsidian", ModuleCategory.COMBAT, 950);
      this.timingConfig = this.register(new EnumConfig("Timing", "Timing for replacing blocks", SurroundModule.Timing.VANILLA, SurroundModule.Timing.values()));
      this.prePlaceExplosionConfig = this.register(new BooleanConfig("PrePlace-Explosions", "Pre places before explosions", false, () -> this.timingConfig.getValue() == SurroundModule.Timing.SEQUENTIAL));
      this.prePlaceTickConfig = this.register(new BooleanConfig("PrePlace-Tick", "Pre places before ticks", false, () -> this.timingConfig.getValue() == SurroundModule.Timing.SEQUENTIAL));
      this.placeRangeConfig = this.register(new NumberConfig("PlaceRange", "The placement range for surround", 0.0F, 4.0F, 6.0F));
      this.attackConfig = this.register(new BooleanConfig("Attack", "Attacks crystals in the way of surround", true));
      this.extendConfig = this.register(new BooleanConfig("Extend", "Extends surround if the player is not in the center of a block", true));
      this.headConfig = this.register(new BooleanConfig("CoverHead", "Place a block at your head", false));
      this.mineExtendConfig = this.register(new BooleanConfig("MineExtend", "Extends surround if the block is being mined", false));
      this.supportConfig = this.register(new BooleanConfig("Support", "Creates a floor for the surround if there is none", false));
      this.shiftTicksConfig = this.register(new NumberConfig("ShiftTicks", "The number of blocks to place per tick", 1, 2, 10));
      this.shiftDelayConfig = this.register(new NumberConfig("ShiftDelay", "The delay between each block placement interval", 0.0F, 1.0F, 5.0F));
      this.jumpDisableConfig = this.register(new BooleanConfig("AutoDisable", "Disables after moving out of the hole", true));
      this.renderConfig = this.register(new BooleanConfig("Render", "Renders where scaffold is placing blocks", false));
      this.fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
      this.surround = new ArrayList<>();
      this.placements = new ArrayList<>();
      this.packets = new HashMap<>();
      this.fadeList = new HashMap<>();
      INSTANCE = this;
   }

   public static SurroundModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      if (mc.player != null) {
         this.prevY = mc.player.getY();
      }
   }

   public void onDisable() {
      this.surround.clear();
      this.placements.clear();
      this.packets.clear();
      this.fadeList.clear();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      this.blocksPlaced = 0;
      if (!SelfTrapModule.getInstance().isEnabled()) {
         if (!(Boolean)this.jumpDisableConfig.getValue() || !(mc.player.getY() - this.prevY > (double)0.5F) && !(mc.player.fallDistance > 1.5F)) {
            if (!(Boolean)this.multitaskConfig.getValue() && this.checkMultitask()) {
               this.surround.clear();
               this.placements.clear();
            } else {
               ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
               if (blockSlot != null) {
                  int slot = blockSlot.slot();
                  if (slot == -1) {
                     this.surround.clear();
                     this.placements.clear();
                  } else {
                     this.surround = this.getSurround(mc.player);
                     if (!this.surround.isEmpty()) {
                        if ((Boolean)this.attackConfig.getValue()) {
                           this.attackBlockingCrystals(this.surround);
                        }

                        this.placements = this.getPlacementsFromSurround(this.surround);
                        if (!this.placements.isEmpty()) {
                           if ((Boolean)this.supportConfig.getValue()) {
                              for(BlockPos block : new ArrayList<>(this.placements)) {
                                 if (!((double)block.getY() > (double)mc.player.getBlockY() + (double)1.0F)) {
                                    Direction direction = Managers.INTERACT.getInteractDirectionInternal(block, (Boolean)this.strictDirectionConfig.getValue());
                                    if (direction == null) {
                                       this.placements.add(block.down());
                                    }
                                 }
                              }
                           }

                           this.placements.sort(Comparator.comparingInt(Vec3i::getY));

                           while(this.blocksPlaced < (Integer)this.shiftTicksConfig.getValue() && this.blocksPlaced < this.placements.size()) {
                              BlockPos targetPos = (BlockPos)this.placements.get(this.blocksPlaced);
                              this.placeBlock(targetPos, slot);
                           }

                           if ((Boolean)this.rotateConfig.getValue()) {
                              Managers.ROTATION.setRotationSilentSync();
                           }

                        }
                     }
                  }
               }
            }
         } else {
            this.disable();
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof BundleS2CPacket) {
            BundleS2CPacket packet = (BundleS2CPacket)var3;

            for(Packet<?> packet1 : packet.getPackets()) {
               this.handlePackets(packet1);
            }
         } else {
            this.handlePackets(event.getPacket());
         }

      }
   }

   private void handlePackets(Packet<?> serverPacket) {
      if (this.timingConfig.getValue() == SurroundModule.Timing.SEQUENTIAL) {
         if (serverPacket instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket)serverPacket;
            BlockState blockState = packet.getState();
            BlockPos targetPos = packet.getPos();
            if (this.surround.contains(targetPos)) {
               if (blockState.isReplaceable() && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, targetPos, ShapeContext.absent())) {
                  ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
                  if (blockSlot == null) {
                     return;
                  }

                  int slot = blockSlot.slot();
                  if (slot == -1) {
                     return;
                  }

                  this.placeBlock(targetPos, slot);
               } else if (BlastResistantBlocks.isBlastResistant(blockState)) {
                  this.packets.remove(targetPos);
               }
            }
         }

         if (this.blocksPlaced <= (Integer)this.shiftTicksConfig.getValue() * 2) {
            if (serverPacket instanceof ExplosionS2CPacket) {
               ExplosionS2CPacket packet = (ExplosionS2CPacket)serverPacket;
               if ((Boolean)this.prePlaceExplosionConfig.getValue()) {
                  BlockPos pos = BlockPos.ofFloored(packet.getX(), packet.getY(), packet.getZ());
                  if (this.surround.contains(pos)) {
                     ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
                     if (blockSlot == null) {
                        return;
                     }

                     int slot = blockSlot.slot();
                     if (slot == -1) {
                        return;
                     }

                     this.placeBlock(pos, slot);
                  }
               }
            }

            if (serverPacket instanceof EntitySpawnS2CPacket) {
               EntitySpawnS2CPacket packet = (EntitySpawnS2CPacket)serverPacket;
               if (packet.getEntityType().equals(EntityType.END_CRYSTAL) && (Boolean)this.prePlaceTickConfig.getValue()) {
                  for(BlockPos pos : this.surround) {
                     if (pos.equals(BlockPos.ofFloored(packet.getX(), packet.getY(), packet.getZ()))) {
                        ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
                        if (blockSlot == null) {
                           return;
                        }

                        int slot = blockSlot.slot();
                        if (slot == -1) {
                           return;
                        }

                        this.placeBlock(pos, slot);
                        break;
                     }
                  }
               }
            }

         }
      }
   }

   private void placeBlock(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock((BlockPos)pos, slot, (Boolean)this.strictDirectionConfig.getValue(), false, true, (state, angles) -> {
         if ((Boolean)this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }

      });
      this.packets.put(pos, System.currentTimeMillis());
      ++this.blocksPlaced;
   }

   public void attackBlockingCrystals(List<BlockPos> posList) {
      for(BlockPos pos : posList) {
         Entity crystalEntity = (Entity)mc.world.getOtherEntities((Entity)null, new Box(pos)).stream().filter((e) -> e instanceof EndCrystalEntity).findFirst().orElse(null);
         if (crystalEntity != null) {
            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return;
         }
      }

   }

   public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround) {
      List<BlockPos> placements = new ArrayList<>();

      for(BlockPos surroundPos : surround) {
         Long placed = (Long)this.packets.get(surroundPos);
         if ((!((Float)this.shiftDelayConfig.getValue() > 0.0F) || placed == null || !((float)(System.currentTimeMillis() - placed) < (Float)this.shiftDelayConfig.getValue() * 50.0F)) && mc.world.getBlockState(surroundPos).isReplaceable()) {
            double dist = mc.player.squaredDistanceTo(surroundPos.toCenterPos());
            if (!(dist > ((NumberConfig)this.placeRangeConfig).getValueSq()) && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, surroundPos, ShapeContext.absent())) {
               placements.add(surroundPos);
            }
         }
      }

      return placements;
   }

   public List<BlockPos> getSurround(PlayerEntity player) {
      List<BlockPos> surroundBlocks = this.getSurroundNoDown(player);
      List<BlockPos> playerBlocks = this.getPlayerBlocks(player);

      for(BlockPos playerPos : playerBlocks) {
         if (!playerPos.equals(player.getBlockPos())) {
            surroundBlocks.add(playerPos.down());
         }
      }

      if ((Boolean)this.mineExtendConfig.getValue()) {
         for(BlockPos surroundPos : new ArrayList<>(surroundBlocks)) {
            if (Managers.BLOCK.isPassed(surroundPos, 0.7F)) {
               for(Direction direction : Direction.values()) {
                  if (direction != Direction.DOWN) {
                     BlockPos blockerPos = surroundPos.offset(direction);
                     if (!playerBlocks.contains(blockerPos) && AutoMineModule.getInstance().getMiningBlock() != blockerPos) {
                        surroundBlocks.add(blockerPos);
                     }
                  }
               }
            }
         }
      }

      if (AirPlaceModule.getInstance().isEnabled() && (Boolean)this.headConfig.getValue()) {
         surroundBlocks.add(mc.player.getBlockPos().up(2));
      }

      return surroundBlocks;
   }

   public List<BlockPos> getSurroundNoDown(PlayerEntity player) {
      return this.getSurroundNoDown(player, 0.0F);
   }

   public List<BlockPos> getSurroundNoDown(PlayerEntity player, float range) {
      List<BlockPos> surroundBlocks = new ArrayList<>();
      List<BlockPos> playerBlocks = this.getPlayerBlocks(player);

      for(BlockPos pos : playerBlocks) {
         if (!(range > 0.0F) || !(mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > (double)(range * range))) {
            for(Direction dir : Direction.values()) {
               if (dir.getAxis().isHorizontal()) {
                  BlockPos pos1 = pos.offset(dir);
                  if (!surroundBlocks.contains(pos1) && !playerBlocks.contains(pos1)) {
                     surroundBlocks.add(pos1);
                  }
               }
            }
         }
      }

      return surroundBlocks;
   }

   public List<BlockPos> getPlayerBlocks(PlayerEntity entity) {
      BlockPos playerPos = PositionUtil.getRoundedBlockPos(entity.getX(), entity.getY(), entity.getZ());
      List<BlockPos> playerBlocks = new ArrayList<>();
      if ((Boolean)this.extendConfig.getValue()) {
         playerBlocks.addAll(PositionUtil.getAllInBox(entity.getBoundingBox(), playerPos));
      } else {
         playerBlocks.add(playerPos);
      }

      return playerBlocks;
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (!SelfTrapModule.getInstance().isEnabled()) {
         if ((Boolean)this.renderConfig.getValue()) {
            RenderBuffers.preRender();

            for(Map.Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
               ((Animation)set.getValue()).setState(false);
               int boxAlpha = (int)((double)40.0F * ((Animation)set.getValue()).getFactor());
               int lineAlpha = (int)((double)100.0F * ((Animation)set.getValue()).getFactor());
               Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
               Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
               RenderManager.renderBox(event.getMatrices(), (BlockPos)set.getKey(), boxColor.getRGB());
               RenderManager.renderBoundingBox(event.getMatrices(), (BlockPos)set.getKey(), 1.5F, lineColor.getRGB());
            }

            RenderBuffers.postRender();
            if (this.placements.isEmpty()) {
               return;
            }

            for(BlockPos pos : this.placements) {
               Animation animation = new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue());
               this.fadeList.put(pos, animation);
            }
         }

         this.fadeList.entrySet().removeIf((e) -> ((Animation)e.getValue()).getFactor() == (double)0.0F);
      }
   }

   public boolean isPlacing() {
      return !this.placements.isEmpty();
   }

   public static enum Timing {
      VANILLA,
      SEQUENTIAL;

      // $FF: synthetic method
      private static Timing[] $values() {
         return new Timing[]{VANILLA, SEQUENTIAL};
      }
   }
}
