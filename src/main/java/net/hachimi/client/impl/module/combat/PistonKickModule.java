package net.hachimi.client.impl.module.combat;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.SnapRotation;
import net.hachimi.client.util.world.BlockUtilPlus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.PistonBlock;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction.Type;

public final class PistonKickModule extends ToggleModule {
   public static PistonKickModule INSTANCE;
   private final Config<Integer> delay = this.register(new NumberConfig("Delay", "Delay between pushes (ticks)", 0, 1, 10));
   private final Config<Boolean> oneTwelve = this.register(new BooleanConfig("1.12", "Place piston and power in the same tick", true));
   private final Config<Float> targetRange = this.register(new NumberConfig("TargetRange", "Target range", 1.0F, 5.0F, 6.0F));
   private final Config<Boolean> inAirTarget = this.register(new BooleanConfig("InAirTarget", "Target players in the air", true));
   private final Config<Boolean> rotate = this.register(new BooleanConfig("Rotate", "Silently rotate when placing", true));
   private final Config<Boolean> outOfBlock = this.register(new BooleanConfig("OutOfBlock", "Target players partially outside of blocks", true));
   private final Config<Boolean> breakCrystal = this.register(new BooleanConfig("BreakCrystal", "Break end crystals blocking piston path", false));
   private final Config<Float> placeRange = this.register(new NumberConfig("PlaceRange", "Block place range", 1.0F, 5.0F, 6.0F));
   private final Config<Float> breakRange = this.register(new NumberConfig("BreakRange", "Crystal break range", 1.0F, 4.5F, 6.0F, () -> this.breakCrystal.getValue()));
   private final Config<SwapAction> swapAction;
   private final Config<Boolean> airPlace;
   private final Config<Boolean> consecutive;
   private final Config<Boolean> merge;
   private int tickCount;
   private PlayerEntity lastTarget;

   public PistonKickModule() {
      super("PistonKick", "Pushes players out of bedrock holes using pistons", ModuleCategory.COMBAT);
      this.swapAction = this.register(new EnumConfig("SwapAction", "How to swap to required items", PistonKickModule.SwapAction.SCREEN, PistonKickModule.SwapAction.values()));
      this.airPlace = this.register(new BooleanConfig("AirPlace", "Allow placing blocks in the air", false));
      this.consecutive = this.register(new BooleanConfig("Consecutive", "Keep targeting the same player consecutively", true));
      this.merge = this.register(new BooleanConfig("Merge", "Burst piston and power packets together", false));
      this.tickCount = 0;
      this.lastTarget = null;
      INSTANCE = this;
   }

   public static PistonKickModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      this.tickCount = 0;
      this.lastTarget = null;
      SnapRotation.INSTANCE.subscribe();
   }

   public void onDisable() {
      this.lastTarget = null;
      SnapRotation.INSTANCE.unsubscribe();
      SnapRotation.reset();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         ++this.tickCount;
         if (this.tickCount >= (Integer)this.delay.getValue()) {
            this.tickCount = 0;
            int pistonSlot = this.findClass(PistonBlock.class);
            int powerSlot = this.findPower();
            if (pistonSlot != -1 && powerSlot != -1) {
               List<PlayerEntity> enemies = this.getEnemies();
               if (!enemies.isEmpty()) {
                  if ((Boolean)this.consecutive.getValue() && this.lastTarget != null && enemies.contains(this.lastTarget)) {
                     enemies.remove(this.lastTarget);
                     enemies.add(0, this.lastTarget);
                  }

                  for(PlayerEntity target : enemies) {
                     if ((Boolean)this.inAirTarget.getValue() || target.isOnGround()) {
                        if ((Boolean)this.breakCrystal.getValue()) {
                           this.breakNearbyCrystals(target);
                        }

                        if (this.tryKick(target, pistonSlot, powerSlot)) {
                           this.lastTarget = target;
                           return;
                        }
                     }
                  }

               }
            }
         }
      }
   }

   private boolean tryKick(PlayerEntity target, int pistonSlot, int powerSlot) {
      float[] offsets = new float[]{-0.25F, 0.0F, 0.25F};

      for(float ox : offsets) {
         for(float oz : offsets) {
            BlockPos playerPos = BlockPos.ofFloored(target.getX() + (double)ox, target.getY() + (double)0.5F, target.getZ() + (double)oz);

            for(net.minecraft.util.math.Direction dir : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
               BlockPos wall = playerPos.offset(dir);
               if (this.isTargetHere(wall, target) && mc.world.canCollide(target, new Box(wall))) {
                  if (this.tryPush(playerPos.offset(dir.getOpposite()), dir, pistonSlot, powerSlot)) {
                     return true;
                  }

                  if (this.tryPush(playerPos.offset(dir.getOpposite()).up(), dir, pistonSlot, powerSlot)) {
                     return true;
                  }
               }
            }
         }
      }

      if (!mc.world.canCollide(target, new Box(BlockPos.ofFloored(target.getX(), target.getY() + (double)2.5F, target.getZ())))) {
         BlockPos entityPos = target.getBlockPos();

         for(net.minecraft.util.math.Direction dir : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos wall = entityPos.offset(dir);
            Box shifted = target.getBoundingBox().offset((double)dir.getOffsetX(), (double)dir.getOffsetY(), (double)dir.getOffsetZ());
            if (mc.world.getBlockState(wall.up()).getBlock() != Blocks.PISTON_HEAD && !mc.world.canCollide(target, shifted.offset((double)0.0F, (double)1.0F, (double)0.0F)) && !this.isTargetHere(wall, target)) {
               if (this.tryPush(entityPos.offset(dir.getOpposite()).up(), dir, pistonSlot, powerSlot)) {
                  return true;
               }

               if (this.tryPush(entityPos.offset(dir.getOpposite()), dir, pistonSlot, powerSlot)) {
                  return true;
               }
            }
         }
      }

      for(float ox : offsets) {
         for(float oz : offsets) {
            BlockPos playerPos = BlockPos.ofFloored(target.getX() + (double)ox, target.getY() + (double)0.5F, target.getZ() + (double)oz);

            for(net.minecraft.util.math.Direction dir : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
               BlockPos wall = playerPos.offset(dir);
               if (this.isTargetHere(wall, target)) {
                  if (this.tryPush(playerPos.offset(dir.getOpposite()).up(), dir, pistonSlot, powerSlot)) {
                     return true;
                  }

                  if (this.tryPush(playerPos.offset(dir.getOpposite()), dir, pistonSlot, powerSlot)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private boolean tryPush(BlockPos pistonPos, net.minecraft.util.math.Direction pushDir, int pistonSlot, int powerSlot) {
      if (!mc.world.isAir(pistonPos.offset(pushDir))) {
         return false;
      } else if (!this.canPlaceAt(pistonPos)) {
         return false;
      } else {
         net.minecraft.util.math.Direction placeSide = this.getPlaceSide(pistonPos);
         if (placeSide != null) {
            boolean canPower = false;

            for(net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
               if (this.isRedstone(pistonPos.offset(d)) || this.canPlaceAt(pistonPos.offset(d))) {
                  canPower = true;
                  break;
               }
            }

            if (!canPower) {
               return false;
            } else {
               this.doSwap(pistonSlot);
               if ((Boolean)this.rotate.getValue()) {
                  this.pistonFacingRotation(pushDir, placeSide);
               }

               this.placeBlockDirect(pistonPos, placeSide);
               this.restoreSlot(pistonSlot);
               if ((Boolean)this.oneTwelve.getValue() || (Boolean)this.merge.getValue() || mc.world.getBlockState(pistonPos).getBlock() instanceof PistonBlock) {
                  for(net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
                     if (this.isRedstone(pistonPos.offset(d))) {
                        return true;
                     }
                  }

                  for(net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
                     if (d != net.minecraft.util.math.Direction.UP) {
                        BlockPos powerPos = pistonPos.offset(d);
                        if (this.canPlaceAt(powerPos) && this.inPlaceRange(powerPos)) {
                           net.minecraft.util.math.Direction pSide = this.getPlaceSide(powerPos);
                           if (pSide != null) {
                              this.doSwap(powerSlot);
                              if ((Boolean)this.rotate.getValue()) {
                                 this.doRotate(powerPos, pSide);
                              }

                              this.placeBlockDirect(powerPos, pSide);
                              this.restoreSlot(powerSlot);
                              return true;
                           }
                        }
                     }
                  }
               }

               return true;
            }
         } else {
            net.minecraft.util.math.Direction powerDir = null;

            for(net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
               if (d != net.minecraft.util.math.Direction.UP) {
                  BlockPos powerPos = pistonPos.offset(d);
                  if (this.canPlaceAt(powerPos) && this.inPlaceRange(powerPos) && BlockUtilPlus.getPlaceSide(pistonPos, (Predicate)null) != null) {
                     powerDir = d;
                     break;
                  }
               }
            }

            if (powerDir == null) {
               return false;
            } else {
               BlockPos powerPos = pistonPos.offset(powerDir);
               net.minecraft.util.math.Direction pSide = this.getPlaceSide(powerPos);
               if (pSide == null) {
                  return false;
               } else {
                  this.doSwap(powerSlot);
                  if ((Boolean)this.rotate.getValue()) {
                     this.doRotate(powerPos, pSide);
                  }

                  this.placeBlockDirect(powerPos, pSide);
                  this.restoreSlot(powerSlot);
                  net.minecraft.util.math.Direction newSide = this.getPlaceSide(pistonPos);
                  if (newSide != null) {
                     this.doSwap(pistonSlot);
                     if ((Boolean)this.rotate.getValue()) {
                        this.pistonFacingRotation(pushDir, newSide);
                     }

                     this.placeBlockDirect(pistonPos, newSide);
                     this.restoreSlot(pistonSlot);
                  }

                  return true;
               }
            }
         }
      }
   }

   private boolean isTargetHere(BlockPos pos, PlayerEntity target) {
      return (new Box(pos)).intersects(target.getBoundingBox());
   }

   private boolean isRedstone(BlockPos pos) {
      Block b = mc.world.getBlockState(pos).getBlock();
      return b == Blocks.REDSTONE_BLOCK || b == Blocks.REDSTONE_TORCH;
   }

   private boolean canPlaceAt(BlockPos pos) {
      return mc.world.getBlockState(pos).isReplaceable() || mc.world.isAir(pos);
   }

   private boolean inPlaceRange(BlockPos pos) {
      double r = (double)(Float)this.placeRange.getValue();
      return mc.player.squaredDistanceTo((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) <= r * r;
   }

   private net.minecraft.util.math.Direction getPlaceSide(BlockPos pos) {
      return (Boolean)this.airPlace.getValue() ? BlockUtilPlus.getClickSideStrict(pos) : BlockUtilPlus.getPlaceSide(pos, (Predicate)null);
   }

   private void breakNearbyCrystals(PlayerEntity target) {
      double r = (double)(Float)this.breakRange.getValue();
      mc.world.getEntitiesByClass(EndCrystalEntity.class, target.getBoundingBox().expand(r), (e) -> e.isAlive() && mc.player.squaredDistanceTo(e) <= r * r).stream().min(Comparator.comparingDouble((e) -> mc.player.squaredDistanceTo(e))).ifPresent((crystal) -> {
         Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
         Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      });
   }

   private void placeBlockDirect(BlockPos pos, net.minecraft.util.math.Direction side) {
      BlockPos neighbor = pos.offset(side);
      Vec3d hitVec = new Vec3d((double)neighbor.getX() + (double)0.5F + (double)side.getOpposite().getOffsetX() * (double)0.5F, (double)neighbor.getY() + (double)0.5F + (double)side.getOpposite().getOffsetY() * (double)0.5F, (double)neighbor.getZ() + (double)0.5F + (double)side.getOpposite().getOffsetZ() * (double)0.5F);
      BlockHitResult result = new BlockHitResult(hitVec, side.getOpposite(), neighbor, false);
      Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
      Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      BlockUtilPlus.placeList.add(pos);
   }

   private void applyAnticheatFlags() {
      AnticheatModule ac = AnticheatModule.getInstance();
      SnapRotation.grimFull = ac != null ? ac.isGrimRotation() : true;
      SnapRotation.doSnapBack = ac != null ? ac.isSnapBack() : false;
   }

   private void doRotate(BlockPos pos, net.minecraft.util.math.Direction side) {
      this.applyAnticheatFlags();
      SnapRotation.snapAt(pos.toCenterPos().add((double)side.getOffsetX() * (double)0.5F, (double)side.getOffsetY() * (double)0.5F, (double)side.getOffsetZ() * (double)0.5F));
   }

   private void pistonFacingRotation(net.minecraft.util.math.Direction pushDir, net.minecraft.util.math.Direction placeSide) {
      this.applyAnticheatFlags();
      float var10000;
      switch (pushDir) {
         case SOUTH -> var10000 = 0.0F;
         case WEST -> var10000 = 90.0F;
         case NORTH -> var10000 = 180.0F;
         case EAST -> var10000 = -90.0F;
         default -> var10000 = 0.0F;
      }

      float yaw = var10000;
      SnapRotation.snapAt(yaw, 0.0F);
   }

   private void doSwap(int slot) {
      switch (((SwapAction)this.swapAction.getValue()).ordinal()) {
         case 0:
            if (slot < 9) {
               Managers.INVENTORY.setSlot(slot);
            } else {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            }
            break;
         case 1:
            if (slot < 9) {
               Managers.INVENTORY.setSlot(slot);
            }
         case 2:
      }

   }

   private void restoreSlot(int slot) {
      if (this.swapAction.getValue() == PistonKickModule.SwapAction.SCREEN && slot >= 9) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
      }

   }

   private int findPower() {
      int slot = this.findBlock(Blocks.REDSTONE_BLOCK);
      return slot != -1 ? slot : this.findBlock(Blocks.REDSTONE_TORCH);
   }

   private int findBlock(Block block) {
      if (this.swapAction.getValue() != PistonKickModule.SwapAction.NONE) {
         int end = this.swapAction.getValue() == PistonKickModule.SwapAction.SCREEN ? 36 : 9;

         for(int i = 0; i < end; ++i) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty()) {
               Item var6 = s.getItem();
               if (var6 instanceof BlockItem) {
                  BlockItem bi = (BlockItem)var6;
                  if (bi.getBlock() == block) {
                     return i;
                  }
               }
            }
         }

         return -1;
      } else {
         ItemStack held = mc.player.getMainHandStack();
         int var10000;
         if (!held.isEmpty()) {
            Item var4 = held.getItem();
            if (var4 instanceof BlockItem) {
               BlockItem bi = (BlockItem)var4;
               if (bi.getBlock() == block) {
                  var10000 = mc.player.getInventory().selectedSlot;
                  return var10000;
               }
            }
         }

         var10000 = -1;
         return var10000;
      }
   }

   private int findClass(Class<?> clazz) {
      if (this.swapAction.getValue() != PistonKickModule.SwapAction.NONE) {
         int end = this.swapAction.getValue() == PistonKickModule.SwapAction.SCREEN ? 36 : 9;

         for(int i = 0; i < end; ++i) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty()) {
               Item var6 = s.getItem();
               if (var6 instanceof BlockItem) {
                  BlockItem bi = (BlockItem)var6;
                  if (clazz.isInstance(bi.getBlock())) {
                     return i;
                  }
               }
            }
         }

         return -1;
      } else {
         ItemStack held = mc.player.getMainHandStack();
         int var10000;
         if (!held.isEmpty()) {
            Item var4 = held.getItem();
            if (var4 instanceof BlockItem) {
               BlockItem bi = (BlockItem)var4;
               if (clazz.isInstance(bi.getBlock())) {
                  var10000 = mc.player.getInventory().selectedSlot;
                  return var10000;
               }
            }
         }

         var10000 = -1;
         return var10000;
      }
   }

   private List<PlayerEntity> getEnemies() {
      double r = (double)(Float)this.targetRange.getValue();
      return (List)mc.world.getPlayers().stream().filter((p) -> p != mc.player).filter((p) -> !p.isSpectator() && !p.isDead()).filter((p) -> !Managers.SOCIAL.isFriend(p.getName().getString())).filter((p) -> mc.player.squaredDistanceTo(p) <= r * r).sorted(Comparator.comparingDouble((p) -> mc.player.squaredDistanceTo(p))).collect(Collectors.toList());
   }

   public static enum SwapAction {
      SCREEN,
      HOTBAR,
      NONE;

      // $FF: synthetic method
      private static SwapAction[] $values() {
         return new SwapAction[]{SCREEN, HOTBAR, NONE};
      }
   }
}
