package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.ObsidianPlacerModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.world.BlastResistantBlocks;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;

public class AutoCrawlTrapModule extends ObsidianPlacerModule {
   private static AutoCrawlTrapModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig("PlaceRange", "The range to trap enemies", 0.1F, 4.0F, 6.0F));
   Config<Float> enemyRangeConfig = this.register(new NumberConfig("EnemyRange", "The maximum range of targets", 0.1F, 10.0F, 15.0F));
   Config<Boolean> downConfig = this.register(new BooleanConfig("PreventDownwards", "Prevents digging downwards", true));
   Config<Boolean> serverHitboxConfig = this.register(new BooleanConfig("HitboxSync", "Places on serverside crawling hitboxes", false));
   Config<Boolean> mineIgnoreConfig = this.register(new BooleanConfig("PreventMine", "Prevents enemies from mining the trap", false));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig("ShiftTicks", "The number of blocks to place per tick", 1, 2, 10));
   Config<Float> shiftDelayConfig = this.register(new NumberConfig("ShiftDelay", "The delay between each block placement interval", 0.0F, 1.0F, 5.0F));
   Config<Integer> extrapolateTicksConfig = this.register(new NumberConfig("ExtrapolationTicks", "Accounts for motion when calculating enemy positions, not fully accurate.", 0, 0, 10));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders trap placements", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private List<BlockPos> surround = new ArrayList<>();
   private List<BlockPos> placements = new ArrayList<>();
   private final Map<BlockPos, Long> packets = new HashMap<>();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private PlayerEntity target;
   private int blocksPlaced;

   public AutoCrawlTrapModule() {
      super("AutoCrawlTrap", "Automatically places blocks to keep enemies in crawl", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AutoCrawlTrapModule getInstance() {
      return INSTANCE;
   }

   public void onDisable() {
      this.surround.clear();
      this.placements.clear();
      this.fadeList.clear();
      this.target = null;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      this.blocksPlaced = 0;
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
               this.target = this.getClosestPlayer((double)(Float)this.enemyRangeConfig.getValue());
               if (this.target == null) {
                  this.surround.clear();
                  this.placements.clear();
               } else {
                  BlockPos targetPos = EntityUtil.getRoundedBlockPos(this.target);
                  this.surround = this.getCrawlTrap(this.target, targetPos);
                  if (this.canCrawlTrap(this.target, targetPos) && !this.surround.isEmpty()) {
                     this.placements = this.getPlacementsFromTrap(this.surround);
                     if (!this.placements.isEmpty()) {
                        this.placements.sort(Comparator.comparingInt(Vec3i::getY));

                        while(this.blocksPlaced < (Integer)this.shiftTicksConfig.getValue() && this.blocksPlaced < this.placements.size()) {
                           BlockPos targetPlacePos = (BlockPos)this.placements.get(this.blocksPlaced);
                           this.placeBlock(targetPlacePos, slot);
                        }

                        if ((Boolean)this.rotateConfig.getValue()) {
                           Managers.ROTATION.setRotationSilentSync();
                        }

                     }
                  }
               }
            }
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
      if (serverPacket instanceof BlockUpdateS2CPacket packet) {
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

   public List<BlockPos> getPlacementsFromTrap(List<BlockPos> surround) {
      List<BlockPos> placements = new ArrayList<>();

      for(BlockPos surroundPos : surround) {
         Long placed = (Long)this.packets.get(surroundPos);
         if (!((Float)this.shiftDelayConfig.getValue() > 0.0F) || placed == null || !((float)(System.currentTimeMillis() - placed) < (Float)this.shiftDelayConfig.getValue() * 50.0F)) {
            Box surroundBox = new Box(surroundPos);
            List<Entity> invalid = mc.world.getOtherEntities((Entity)null, surroundBox).stream().filter((e) -> this.invalidEntity(e)).toList();
            boolean serverCrawling = invalid.stream().allMatch((e) -> Managers.HITBOX.isServerCrawling(e) && Managers.HITBOX.getCrawlingBoundingBox(e).intersects(surroundBox));
            if (mc.world.getBlockState(surroundPos).isReplaceable() || serverCrawling && (Boolean)this.serverHitboxConfig.getValue() || Managers.BLOCK.isPassed(surroundPos, 0.7F) && (Boolean)this.mineIgnoreConfig.getValue()) {
               double dist = mc.player.squaredDistanceTo(surroundPos.toCenterPos());
               if (!(dist > ((NumberConfig)this.rangeConfig).getValueSq()) && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, surroundPos, ShapeContext.absent())) {
                  placements.add(surroundPos);
               }
            }
         }
      }

      return placements;
   }

   public List<BlockPos> getCrawlTrap(PlayerEntity entity, BlockPos playerPos) {
      List<BlockPos> crawlTrap = new ArrayList<>();
      crawlTrap.add(playerPos.up());
      if ((Boolean)this.downConfig.getValue()) {
         crawlTrap.add(playerPos.down());
      }

      double x = entity.getX();
      double y = entity.getY();
      double z = entity.getZ();

      for(int ticks = 0; ticks <= (Integer)this.extrapolateTicksConfig.getValue(); ++ticks) {
         double ox = (x - entity.prevX) * (double)ticks;
         double oz = (z - entity.prevZ) * (double)ticks;
         BlockPos blockPos = BlockPos.ofFloored(x + ox, y, z + oz);
         if (!crawlTrap.contains(blockPos.up())) {
            crawlTrap.add(blockPos.up());
         }

         if ((Boolean)this.downConfig.getValue() && !crawlTrap.contains(blockPos.down())) {
            crawlTrap.add(blockPos.down());
         }
      }

      return crawlTrap;
   }

   public boolean invalidEntity(Entity entity) {
      return !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ArrowEntity);
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
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

   private boolean canCrawlTrap(PlayerEntity player, BlockPos playerPos) {
      return player.isOnGround() || !mc.world.getBlockState(playerPos.up()).isReplaceable() || !mc.world.getBlockState(playerPos.up(2)).isReplaceable();
   }

   public boolean isPlacing() {
      return !this.placements.isEmpty();
   }
}
