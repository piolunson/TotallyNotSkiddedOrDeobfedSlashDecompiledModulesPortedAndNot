package net.hachimi.client.impl.module.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BlockListConfig;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.AttackBlockEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.player.SpeedmineModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.TickTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public final class AutoMineModule extends ToggleModule {
   public static AutoMineModule INSTANCE;
   private final Config<SelectionMode> selection;
   private final Config<List<Block>> whitelist;
   private final Config<List<Block>> blacklist;
   private final Config<Float> targetRange;
   private final Config<Float> mineRange;
   private final Config<SortMode> sort;
   private final Config<Boolean> wait;
   private final Config<Boolean> burrow;
   private final Config<Boolean> surround;
   private final Config<Boolean> under;
   private final Config<Boolean> face;
   private final Config<Integer> priority;
   private static final Set<Block> ALWAYS_SKIP;
   private final Timer tickTimer;
   private final Map<BlockPos, net.minecraft.util.math.Direction> miningMap;
   private PlayerEntity currentTarget;
   private BlockPos lastMinePos;
   private BlockPos manualMinePos;
   private boolean silentSwapping;

   public AutoMineModule() {
      super("AutoMine", "Automatically mine blocks around enemies", ModuleCategory.COMBAT);
      this.selection = this.register(new EnumConfig("Selection", "Block filter mode", AutoMineModule.SelectionMode.BLACKLIST, AutoMineModule.SelectionMode.values()));
      this.whitelist = this.register(new BlockListConfig("Whitelist", "Blocks to mine (whitelist mode)", new Block[]{Blocks.OBSIDIAN, Blocks.ENDER_CHEST}));
      this.blacklist = this.register(new BlockListConfig("Blacklist", "Blocks to skip (blacklist mode)", new Block[]{Blocks.BEDROCK, Blocks.LADDER, Blocks.SCAFFOLDING, Blocks.FIRE, Blocks.COBWEB, Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED, Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED, Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.BLACK_BED}));
      this.targetRange = this.register(new NumberConfig("TargetRange", "Range to find target", 1.0F, 6.0F, 12.0F));
      this.mineRange = this.register(new NumberConfig("MineRange", "Range to mine blocks", 1.0F, 6.0F, 6.0F));
      this.sort = this.register(new EnumConfig("Sort", "Target sorting", AutoMineModule.SortMode.LOWEST_DISTANCE, AutoMineModule.SortMode.values()));
      this.wait = this.register(new BooleanConfig("Wait", "Wait for block to break before mining next", true));
      this.burrow = this.register(new BooleanConfig("Burrow", "Mine block player is standing inside", true));
      this.surround = this.register(new BooleanConfig("Surround", "Mine surround blocks around enemy", true));
      this.under = this.register(new BooleanConfig("Under", "Mine block below target's feet", false));
      this.face = this.register(new BooleanConfig("Face", "Rotate to block before mining", false));
      this.priority = this.register(new NumberConfig("Priority", "Mining priority (higher = mine first)", 0, 8, 10));
      this.tickTimer = new TickTimer();
      this.miningMap = new LinkedHashMap<>();
      this.silentSwapping = false;
      INSTANCE = this;
   }

   public static AutoMineModule getInstance() {
      return INSTANCE;
   }

   public BlockPos getMiningBlock() {
      return this.lastMinePos;
   }

   public boolean isSilentSwapping() {
      return this.silentSwapping;
   }

   public void onEnable() {
      this.miningMap.clear();
      this.currentTarget = null;
      this.lastMinePos = null;
      this.manualMinePos = null;
      this.silentSwapping = false;
   }

   public void onDisable() {
      for(Map.Entry<BlockPos, net.minecraft.util.math.Direction> e : this.miningMap.entrySet()) {
         Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, (BlockPos)e.getKey(), (net.minecraft.util.math.Direction)e.getValue()));
      }

      this.miningMap.clear();
      this.currentTarget = null;
      this.lastMinePos = null;
      this.manualMinePos = null;
      this.silentSwapping = false;
   }

   @EventListener
   public void onAttackBlock(AttackBlockEvent event) {
      if (mc.player != null && mc.world != null) {
         BlockPos pos = event.getPos();
         if (this.lastMinePos == null || !pos.equals(this.lastMinePos)) {
            this.manualMinePos = pos;
            if (this.lastMinePos != null && this.miningMap.containsKey(this.lastMinePos) && !this.lastMinePos.equals(pos)) {
               net.minecraft.util.math.Direction dir = (net.minecraft.util.math.Direction)this.miningMap.remove(this.lastMinePos);
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, this.lastMinePos, dir));
               this.lastMinePos = null;
            }

         }
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         this.miningMap.entrySet().removeIf((e) -> mc.world.getBlockState((BlockPos)e.getKey()).isAir());
         if (this.manualMinePos != null) {
            BlockState manualState = mc.world.getBlockState(this.manualMinePos);
            boolean stillMining = !manualState.isAir() && mc.player.squaredDistanceTo(this.manualMinePos.toCenterPos()) <= (double)((Float)this.mineRange.getValue() * (Float)this.mineRange.getValue());
            if (stillMining) {
               return;
            }

            this.manualMinePos = null;
         }

         this.currentTarget = this.getBestTarget();
         if ((Boolean)this.wait.getValue() && this.lastMinePos != null && this.miningMap.containsKey(this.lastMinePos) && this.isStillMinable(this.lastMinePos)) {
            this.doMine(this.lastMinePos);
         } else if (this.currentTarget == null) {
            if (!this.miningMap.isEmpty()) {
               this.abortAll();
            }

            this.lastMinePos = null;
         } else {
            List<BlockPos> toMine = this.getBlocksToMine(this.currentTarget);
            if (toMine.isEmpty()) {
               if (!this.miningMap.isEmpty()) {
                  this.abortAll();
               }

               this.lastMinePos = null;
            } else {
               if ((Boolean)this.wait.getValue()) {
                  if (this.lastMinePos != null && !mc.world.getBlockState(this.lastMinePos).isAir() && this.miningMap.containsKey(this.lastMinePos)) {
                     this.doMine(this.lastMinePos);
                     return;
                  }

                  BlockPos next = (BlockPos)toMine.stream().filter((p) -> !this.miningMap.containsKey(p)).findFirst().orElse(null);
                  if (next == null) {
                     return;
                  }

                  this.doMine(next);
               } else {
                  for(BlockPos pos : toMine) {
                     this.doMine(pos);
                  }
               }

            }
         }
      }
   }

   private void doMine(BlockPos pos) {
      if (!mc.world.getBlockState(pos).isAir()) {
         net.minecraft.util.math.Direction bestFace = this.getBestFace(pos);
         if ((Boolean)this.face.getValue()) {
            float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), pos.toCenterPos().add((double)bestFace.getOffsetX() * (double)0.5F, (double)bestFace.getOffsetY() * (double)0.5F, (double)bestFace.getOffsetZ() * (double)0.5F));
            this.silentSwapping = true;
            Managers.ROTATION.setRotationSilent(rot[0], rot[1]);
            this.silentSwapping = false;
         }

         SpeedmineModule speedmine = SpeedmineModule.getInstance();
         if (speedmine != null && speedmine.isEnabled()) {
            speedmine.clickMine(new SpeedmineModule.MiningData(pos, bestFace));
            this.miningMap.putIfAbsent(pos, bestFace);
            this.lastMinePos = pos;
         } else {
            if (!this.miningMap.containsKey(pos)) {
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, bestFace));
               this.miningMap.put(pos, bestFace);
            }

            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, bestFace));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            this.lastMinePos = pos;
         }
      }
   }

   private void abortAll() {
      for(Map.Entry<BlockPos, net.minecraft.util.math.Direction> e : this.miningMap.entrySet()) {
         Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, (BlockPos)e.getKey(), (net.minecraft.util.math.Direction)e.getValue()));
      }

      this.miningMap.clear();
   }

   private List<BlockPos> getBlocksToMine(PlayerEntity target) {
      List<BlockPos> candidates = new ArrayList<>();
      BlockPos feet = target.getBlockPos();
      if ((Boolean)this.burrow.getValue() && this.isBurrowed(target, feet) && this.isValid(feet)) {
         candidates.add(feet);
      }

      if ((Boolean)this.surround.getValue()) {
         List<BlockPos> sides = new ArrayList<>();

         for(net.minecraft.util.math.Direction dir : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos side = feet.offset(dir);
            if (this.isValid(side)) {
               sides.add(side);
            }
         }

         sides.sort(Comparator.comparingDouble((p) -> mc.player.squaredDistanceTo(p.toCenterPos())));
         candidates.addAll(sides);
      }

      BlockPos under = feet.down();
      if ((Boolean)this.under.getValue() && this.isValid(under) && !candidates.contains(under)) {
         candidates.add(under);
      }

      double maxDistSq = (double)((Float)this.mineRange.getValue() * (Float)this.mineRange.getValue());
      return (List)candidates.stream().filter((p) -> mc.player.squaredDistanceTo(p.toCenterPos()) <= maxDistSq).collect(Collectors.toList());
   }

   private boolean isStillMinable(BlockPos pos) {
      if (mc.world.getBlockState(pos).isAir()) {
         return false;
      } else {
         return mc.player.squaredDistanceTo(pos.toCenterPos()) <= (double)((Float)this.mineRange.getValue() * (Float)this.mineRange.getValue());
      }
   }

   private boolean isBurrowed(PlayerEntity player, BlockPos feet) {
      return !mc.world.getBlockState(feet).isAir() && player.getBoundingBox().intersects(new Box(feet));
   }

   private boolean isValid(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      if (state.isAir()) {
         return false;
      } else {
         Block block = state.getBlock();
         if (ALWAYS_SKIP.contains(block)) {
            return false;
         } else if (state.getHardness(mc.world, pos) < 0.0F) {
            return false;
         } else {
            boolean var10000;
            switch (((SelectionMode)this.selection.getValue()).ordinal()) {
               case 0 -> var10000 = true;
               case 1 -> var10000 = ((List)this.whitelist.getValue()).contains(block);
               case 2 -> var10000 = !((List)this.blacklist.getValue()).contains(block);
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            return var10000;
         }
      }
   }

   private net.minecraft.util.math.Direction getBestFace(BlockPos pos) {
      net.minecraft.util.math.Direction best = net.minecraft.util.math.Direction.UP;
      double min = Double.MAX_VALUE;

      for(net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
         double dist = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add((double)d.getOffsetX() * (double)0.5F, (double)d.getOffsetY() * (double)0.5F, (double)d.getOffsetZ() * (double)0.5F));
         if (dist < min) {
            min = dist;
            best = d;
         }
      }

      return best;
   }

   private PlayerEntity getBestTarget() {
      double r = (double)(Float)this.targetRange.getValue();
      return (PlayerEntity)mc.world.getPlayers().stream().filter((p) -> p != mc.player).filter((p) -> !p.isSpectator() && !p.isDead()).filter((p) -> !Managers.SOCIAL.isFriend(p.getName().getString())).filter((p) -> mc.player.squaredDistanceTo(p) <= r * r).min(this.getComparator()).orElse(null);
   }

   private Comparator<PlayerEntity> getComparator() {
      Comparator<PlayerEntity> var10000;
      switch (((SortMode)this.sort.getValue()).ordinal()) {
         case 1 -> var10000 = Comparator.comparingDouble((PlayerEntity p) -> (double)(p.getHealth() + p.getAbsorptionAmount()));
         case 2 -> var10000 = Comparator.comparingDouble(this::getAngleTo);
         default -> var10000 = Comparator.comparingDouble((PlayerEntity p) -> mc.player.squaredDistanceTo(p));
      }

      return var10000;
   }

   private double getAngleTo(PlayerEntity target) {
      float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), target.getEyePos());
      float deltaYaw = Math.abs(rot[0] - mc.player.getYaw()) % 360.0F;
      if (deltaYaw > 180.0F) {
         deltaYaw = 360.0F - deltaYaw;
      }

      return (double)(deltaYaw + Math.abs(rot[1] - mc.player.getPitch()));
   }

   static {
      ALWAYS_SKIP = Set.of(Blocks.BEDROCK, Blocks.BARRIER, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW, Blocks.FIRE, Blocks.AIR, Blocks.VOID_AIR, Blocks.CAVE_AIR);
   }

   public static enum SelectionMode {
      ANY,
      WHITELIST,
      BLACKLIST;

      // $FF: synthetic method
      private static SelectionMode[] $values() {
         return new SelectionMode[]{ANY, WHITELIST, BLACKLIST};
      }
   }

   public static enum SortMode {
      LOWEST_DISTANCE,
      LOWEST_HEALTH,
      CLOSEST_ANGLE;

      // $FF: synthetic method
      private static SortMode[] $values() {
         return new SortMode[]{LOWEST_DISTANCE, LOWEST_HEALTH, CLOSEST_ANGLE};
      }
   }
}
