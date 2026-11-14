package net.hachimi.client.impl.module.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.SnapRotation;
import net.hachimi.client.util.world.BlockUtilPlus;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Direction.Type;

public class PistonCrystalModule extends CombatModule {
   private final Config<Float> minDamage = this.register(new NumberConfig("MinDamage", "Minimum damage to attempt a sequence", 1.0F, 6.0F, 20.0F));
   private final Config<Float> maxLocalDamage = this.register(new NumberConfig("MaxLocalDamage", "Max self-damage allowed", 1.0F, 12.0F, 20.0F));
   private final Config<Integer> delay = this.register(new NumberConfig("Delay", "Delay between place actions (ticks)", 0, 0, 10));
   private final Config<Sort> sort;
   private final Config<Float> targetRange;
   private final Config<Boolean> inAirTarget;
   private final Config<Boolean> breakCrystal;
   private final Config<Float> placeRange;
   private final Config<Float> breakRange;
   private final Config<Boolean> rotate;
   private final Config<Boolean> grim;
   private final Config<SwapAction> swapAction;
   private final Config<Boolean> airPlace;
   private final Config<Boolean> safety;
   private final Config<Boolean> blockDestruction;
   private final Config<Boolean> selfExtrapolate;
   private final Config<Integer> extrapolationTicks;
   private final Config<Boolean> assumeBestArmor;
   private BlockPos pistonPos;
   private BlockPos redstonePos;
   private BlockPos crystalPos;
   private BlockPos lastPiston;
   private net.minecraft.util.math.Direction face;
   private boolean usingTorch;
   private PlayerEntity target;
   private long lastActionMs;
   private long breakTimerMs;
   private int preSwapSlot;

   public PistonCrystalModule() {
      super("PistonCrystal", "Places piston + redstone to push a crystal into targets", ModuleCategory.COMBAT, 75);
      this.sort = this.register(new EnumConfig("Sort", "How to pick target", PistonCrystalModule.Sort.CLOSE_ANGLE, PistonCrystalModule.Sort.values()));
      this.targetRange = this.register(new NumberConfig("TargetRange", "Max target range", 1.0F, 6.0F, 20.0F));
      this.inAirTarget = this.register(new BooleanConfig("InAirTarget", "Target players in the air", true));
      this.breakCrystal = this.register(new BooleanConfig("BreakCrystal", "Attack crystal after placing it", true));
      this.placeRange = this.register(new NumberConfig("PlaceRange", "Range to place blocks", 0.1F, 4.5F, 6.0F));
      this.breakRange = this.register(new NumberConfig("BreakRange", "Range to break crystals", 0.1F, 4.5F, 6.0F));
      this.rotate = this.register(new BooleanConfig("Rotate", "Silently rotate to placed blocks", true));
      this.grim = this.register(new BooleanConfig("Grim", "YawDeceive — filter dir by player facing & override piston yaw", true, () -> this.rotate.getValue()));
      this.swapAction = this.register(new EnumConfig("SwapAction", "How to swap to required items", PistonCrystalModule.SwapAction.SCREEN, PistonCrystalModule.SwapAction.values()));
      this.airPlace = this.register(new BooleanConfig("AirPlace", "Allow placing when not on ground", false));
      this.safety = this.register(new BooleanConfig("Safety", "Skip if crystal would kill self", true));
      this.blockDestruction = this.register(new BooleanConfig("BlockDestruction", "Account for explosion block destruction", false));
      this.selfExtrapolate = this.register(new BooleanConfig("SelfExtrapolate", "Extrapolate self position for self-damage calc", false));
      this.extrapolationTicks = this.register(new NumberConfig("ExtrapolationTicks", "Ticks to extrapolate enemy position", 0, 0, 10));
      this.assumeBestArmor = this.register(new BooleanConfig("AssumeBestArmor", "Assume target has Prot 4 netherite", false));
      this.lastActionMs = 0L;
      this.breakTimerMs = 0L;
      this.preSwapSlot = -1;
   }

   public void onEnable() {
      super.onEnable();
      this.resetState();
      this.breakTimerMs = System.currentTimeMillis() + 99999999L;
      SnapRotation.INSTANCE.subscribe();
   }

   public void onDisable() {
      super.onDisable();
      this.resetState();
      SnapRotation.INSTANCE.unsubscribe();
      SnapRotation.reset();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!mc.player.isUsingItem()) {
            long nowMs = System.currentTimeMillis();
            if (nowMs - this.lastActionMs >= (long)(Integer)this.delay.getValue() * 50L) {
               int crystalSlot = this.findItem(Items.END_CRYSTAL);
               int pistonSlot = this.findItemClass(PistonBlock.class);
               int redstoneSlot = this.findRedstone();
               if (crystalSlot != -1 && pistonSlot != -1 && redstoneSlot != -1) {
                  this.target = this.pickTarget();
                  if (this.target != null) {
                     if (this.crystalPos != null && this.redstonePos != null && this.pistonPos != null && (!BlockUtilPlus.canPlaceCrystal(this.crystalPos) || !BlockUtilPlus.canPlace(this.redstonePos) || !BlockUtilPlus.canPlace(this.pistonPos))) {
                        this.resetState();
                     }

                     if (this.pistonPos == null && this.crystalPos == null && this.redstonePos == null) {
                        this.doPistonCrystal(this.target);
                     }

                     if (this.lastPiston != null && this.face != null && mc.world.getBlockState(this.lastPiston.offset(this.face.getOpposite())).getBlock() instanceof PistonHeadBlock && mc.world.getBlockState(this.lastPiston).getBlock() instanceof PistonBlock) {
                        net.minecraft.util.math.Direction side = BlockUtilPlus.getClickSide(this.lastPiston);
                        if (side != null) {
                           mc.interactionManager.attackBlock(this.lastPiston, side);
                           this.resetState();
                           return;
                        }
                     }

                     if (this.pistonPos != null && BlockUtilPlus.canPlace(this.pistonPos)) {
                        this.placeBlock(pistonSlot, this.pistonPos, this.face);
                        this.lastPiston = this.pistonPos;
                     }

                     if (this.redstonePos != null && BlockUtilPlus.canPlace(this.redstonePos)) {
                        if (this.usingTorch) {
                           this.placeTorch(redstoneSlot, this.redstonePos);
                        } else {
                           this.placeBlock(redstoneSlot, this.redstonePos, (net.minecraft.util.math.Direction)null);
                        }
                     }

                     if (this.crystalPos != null && BlockUtilPlus.canPlaceCrystal(this.crystalPos)) {
                        this.placeCrystal(crystalSlot, this.crystalPos);
                        this.lastActionMs = nowMs;
                        this.breakTimerMs = nowMs;
                     } else {
                        if ((Boolean)this.breakCrystal.getValue() && nowMs - this.breakTimerMs >= (long)(Integer)this.delay.getValue() * 50L) {
                           for(int yOff = -1; yOff <= 2; ++yOff) {
                              BlockPos check = this.target.getBlockPos().up(yOff);
                              if (BlockUtilPlus.hasCrystal(check)) {
                                 this.attackCrystalAt(check);
                                 this.lastActionMs = nowMs;
                                 this.breakTimerMs = nowMs;
                                 this.resetState();
                                 break;
                              }
                           }
                        }

                     }
                  }
               } else {
                  this.resetState();
               }
            }
         }
      }
   }

   private void doPistonCrystal(PlayerEntity tgt) {
      BlockPos base = tgt.getBlockPos();
      int ticks = (Integer)this.extrapolationTicks.getValue();
      Vec3d vel = tgt.getVelocity();
      double ex = tgt.getX() + vel.x * (double)ticks;
      double ey = tgt.getY() + vel.y * (double)ticks;
      double ez = tgt.getZ() + vel.z * (double)ticks;
      base = BlockPos.ofFloored(ex, ey, ez);

      for(int h : new int[]{0, 1, 2}) {
         BlockPos standPos = base.up(h);
         Vec3d vec = new Vec3d((double)standPos.getX() + (double)0.5F, (double)standPos.getY(), (double)standPos.getZ() + (double)0.5F);
         float dmg = (float)ExplosionUtil.getDamageTo(tgt, vec, (Boolean)this.blockDestruction.getValue(), ticks, (Boolean)this.assumeBestArmor.getValue());
         float selfDmg = (float)ExplosionUtil.getDamageTo(mc.player, vec, (Boolean)this.blockDestruction.getValue(), (Boolean)this.selfExtrapolate.getValue() ? ticks : 0, (Boolean)this.assumeBestArmor.getValue());
         if (!(dmg <= (Float)this.minDamage.getValue()) && !(selfDmg > (Float)this.maxLocalDamage.getValue()) && (!(Boolean)this.safety.getValue() || !(selfDmg >= mc.player.getHealth() + mc.player.getAbsorptionAmount())) && this.trySearch(base, h)) {
            return;
         }
      }

   }

   private boolean trySearch(BlockPos base, int height) {
      List<net.minecraft.util.math.Direction> dirs = new ArrayList<>();
      net.minecraft.util.math.Direction facing = mc.player.getHorizontalFacing();
      dirs.add(facing);

      for(net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
         if (d != facing) {
            dirs.add(d);
         }
      }

      for(net.minecraft.util.math.Direction dir : dirs) {
         BlockPos temp1 = base.offset(dir).up(height);
         if (BlockUtilPlus.canPlaceCrystal(temp1) && this.inRange(temp1, (double)(Float)this.placeRange.getValue())) {
            BlockPos tempPiston = null;
            BlockPos tempRedstone = null;

            label129:
            for(net.minecraft.util.math.Direction dir2 : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
               if (dir2 != dir.getOpposite()) {
                  BlockPos temp2 = temp1.offset(dir).offset(dir2);
                  BlockPos extCheck = temp2.offset(dir.getOpposite());
                  if (mc.world.isAir(extCheck) || mc.world.getBlockState(extCheck).isReplaceable()) {
                     if (!BlockUtilPlus.canPlace(temp2) && !(mc.world.getBlockState(temp2).getBlock() instanceof PistonBlock)) {
                        for(net.minecraft.util.math.Direction help : net.minecraft.util.math.Direction.values()) {
                           if (help != dir.getOpposite()) {
                              BlockPos helpPos = temp2.offset(help);
                              if (BlockUtilPlus.isGrimDirection(helpPos, help.getOpposite()) && BlockUtilPlus.canPlace(helpPos) && this.inRange(helpPos, (double)(Float)this.placeRange.getValue())) {
                                 net.minecraft.util.math.Direction side = BlockUtilPlus.getPlaceSide(helpPos, (dx) -> true);
                                 if (side != null) {
                                    int redSlot = this.findRedstone();
                                    if (redSlot != -1) {
                                       this.doSwap(redSlot);
                                       if ((Boolean)this.rotate.getValue()) {
                                          this.doRotate(helpPos, side);
                                       }

                                       BlockUtilPlus.placeBlock(helpPos, side, false);
                                       if ((Boolean)this.rotate.getValue()) {
                                          this.doSnapBack();
                                       }

                                       this.restoreSlot(redSlot);
                                       return true;
                                    }
                                 }
                              }
                           }
                        }
                     } else if (this.inRange(temp2, (double)(Float)this.placeRange.getValue())) {
                        tempPiston = temp2;

                        for(net.minecraft.util.math.Direction dir3 : net.minecraft.util.math.Direction.values()) {
                           if (dir3 != dir.getOpposite()) {
                              BlockPos temp3 = temp2.offset(dir3);
                              if (mc.world.getBlockState(temp3).getBlock() instanceof RedstoneBlock || mc.world.getBlockState(temp3).getBlock() instanceof RedstoneTorchBlock) {
                                 tempRedstone = temp2.offset(dir3);
                                 break label129;
                              }

                              if (BlockUtilPlus.canPlace(temp3) && this.inRange(temp3, (double)(Float)this.placeRange.getValue())) {
                                 tempRedstone = temp2.offset(dir3);
                                 break label129;
                              }
                           }
                        }
                        break;
                     }
                  }
               }
            }

            if (tempPiston != null && tempRedstone != null) {
               this.face = dir;
               this.crystalPos = temp1;
               this.pistonPos = tempPiston;
               this.redstonePos = tempRedstone;
               return true;
            }
         }
      }

      return false;
   }

   private void placeBlock(int slot, BlockPos pos, net.minecraft.util.math.Direction pistonDir) {
      if (slot != -1) {
         net.minecraft.util.math.Direction side = BlockUtilPlus.getPlaceSide(pos, (d) -> true);
         if (side != null) {
            this.doSwap(slot);
            if ((Boolean)this.rotate.getValue()) {
               this.doRotate(pos, side);
               if (pistonDir != null && (Boolean)this.grim.getValue()) {
                  this.pistonFacingSnap(pistonDir);
               }
            }

            BlockUtilPlus.placeBlock(pos, side, false);
            if ((Boolean)this.rotate.getValue()) {
               this.doSnapBack();
            }

            this.restoreSlot(slot);
         }
      }
   }

   private void placeCrystal(int slot, BlockPos cp) {
      BlockPos base = cp.down();
      net.minecraft.util.math.Direction side = BlockUtilPlus.getClickSide(base);
      if (side == null) {
         side = net.minecraft.util.math.Direction.UP;
      }

      if (slot != -1) {
         this.doSwap(slot);
         if ((Boolean)this.rotate.getValue()) {
            this.doRotate(base, side);
         }

         BlockUtilPlus.clickBlock(base, side, false);
         if ((Boolean)this.rotate.getValue()) {
            this.doSnapBack();
         }

         this.restoreSlot(slot);
      }
   }

   private void placeTorch(int slot, BlockPos pos) {
      if (BlockUtilPlus.canPlace(pos)) {
         if (slot != -1) {
            List<net.minecraft.util.math.Direction> sides = BlockUtilPlus.getPlaceSides(pos, (Predicate)null);
            if (!sides.isEmpty()) {
               for(net.minecraft.util.math.Direction side : sides) {
                  if (!(mc.world.getBlockState(pos.offset(side)).getBlock() instanceof PistonBlock) && side != net.minecraft.util.math.Direction.UP) {
                     this.doSwap(slot);
                     if ((Boolean)this.rotate.getValue()) {
                        this.doRotate(pos, side);
                     }

                     BlockUtilPlus.placeBlock(pos, side, false);
                     if ((Boolean)this.rotate.getValue()) {
                        this.doSnapBack();
                     }

                     this.restoreSlot(slot);
                     return;
                  }
               }

            }
         }
      }
   }

   private void attackCrystalAt(BlockPos pos) {
      List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, (new Box(pos)).expand((double)1.0F), (e) -> e.isAlive());
      if (!crystals.isEmpty()) {
         double brSq = (double)((Float)this.breakRange.getValue() * (Float)this.breakRange.getValue());
         EndCrystalEntity crystal = (EndCrystalEntity)crystals.stream().filter((e) -> mc.player.squaredDistanceTo(e) <= brSq).min(Comparator.comparingDouble((e) -> mc.player.squaredDistanceTo(e))).orElse(null);
         if (crystal != null) {
            if ((Boolean)this.rotate.getValue()) {
               this.applyAnticheatFlags();
               SnapRotation.snapAt(crystal.getBoundingBox());
            }

            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            if ((Boolean)this.rotate.getValue()) {
               this.doSnapBack();
            }

         }
      }
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

   private void pistonFacingSnap(net.minecraft.util.math.Direction dir) {
      this.applyAnticheatFlags();
      if (dir == net.minecraft.util.math.Direction.EAST) {
         SnapRotation.snapAt(-90.0F, 5.0F);
      } else if (dir == net.minecraft.util.math.Direction.WEST) {
         SnapRotation.snapAt(90.0F, 5.0F);
      } else if (dir == net.minecraft.util.math.Direction.NORTH) {
         SnapRotation.snapAt(180.0F, 5.0F);
      } else {
         SnapRotation.snapAt(0.0F, 5.0F);
      }

   }

   private void doSnapBack() {
      AnticheatModule ac = AnticheatModule.getInstance();
      boolean sb = ac != null ? ac.isSnapBack() : false;
      if (sb) {
         SnapRotation.snapBack();
      } else {
         SnapRotation.holdActive = false;
      }

   }

   private void doSwap(int slot) {
      if (slot >= 0) {
         this.preSwapSlot = mc.player.getInventory().selectedSlot;
         switch (((SwapAction)this.swapAction.getValue()).ordinal()) {
            case 0:
               if (slot < 9) {
                  Managers.INVENTORY.setClientSlot(slot);
               } else {
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, this.preSwapSlot + 36, SlotActionType.SWAP, mc.player);
                  Managers.INVENTORY.setClientSlot(this.preSwapSlot);
               }
               break;
            case 1:
               if (slot < 9) {
                  Managers.INVENTORY.setClientSlot(slot);
               }
            case 2:
         }

      }
   }

   private void restoreSlot(int slot) {
      if (this.preSwapSlot != -1) {
         if (this.swapAction.getValue() == PistonCrystalModule.SwapAction.SCREEN && slot >= 9) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, this.preSwapSlot + 36, SlotActionType.SWAP, mc.player);
         } else if (this.swapAction.getValue() != PistonCrystalModule.SwapAction.NONE) {
            Managers.INVENTORY.setClientSlot(this.preSwapSlot);
         }

         this.preSwapSlot = -1;
      }
   }

   private int findItem(Item item) {
      if (this.swapAction.getValue() != PistonCrystalModule.SwapAction.NONE) {
         int end = this.swapAction.getValue() == PistonCrystalModule.SwapAction.SCREEN ? 36 : 9;

         for(int i = 0; i < end; ++i) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() == item) {
               return i;
            }
         }

         return -1;
      } else {
         ItemStack h = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
         return !h.isEmpty() && h.getItem() == item ? mc.player.getInventory().selectedSlot : -1;
      }
   }

   private int findItemClass(Class<?> c) {
      if (this.swapAction.getValue() == PistonCrystalModule.SwapAction.NONE) {
         ItemStack h = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
         if (!h.isEmpty()) {
            Item var9 = h.getItem();
            if (var9 instanceof BlockItem) {
               BlockItem bi = (BlockItem)var9;
               if (c.isInstance(bi.getBlock())) {
                  return mc.player.getInventory().selectedSlot;
               }
            }
         }

         return -1;
      } else {
         int end = this.swapAction.getValue() == PistonCrystalModule.SwapAction.SCREEN ? 36 : 9;

         for(int i = 0; i < end; ++i) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty()) {
               Item var6 = s.getItem();
               if (var6 instanceof BlockItem) {
                  BlockItem bi = (BlockItem)var6;
                  if (c.isInstance(bi.getBlock())) {
                     return i;
                  }
               }
            }
         }

         return -1;
      }
   }

   private int findRedstone() {
      int slot = this.findItem(Items.REDSTONE_BLOCK);
      if (slot != -1) {
         this.usingTorch = false;
         return slot;
      } else {
         slot = this.findItem(Items.REDSTONE_TORCH);
         if (slot != -1) {
            this.usingTorch = true;
            return slot;
         } else {
            return -1;
         }
      }
   }

   private PlayerEntity pickTarget() {
      double r = (double)(Float)this.targetRange.getValue();
      return (PlayerEntity)mc.world.getPlayers().stream().filter((e) -> !(e instanceof ClientPlayerEntity)).filter((e) -> !e.isSpectator() && !e.isDead()).filter((e) -> mc.player.squaredDistanceTo(e) <= r * r).filter((e) -> !Managers.SOCIAL.isFriend(e.getName().getString())).filter((e) -> (Boolean)this.inAirTarget.getValue() || e.isOnGround()).min(this.getComparator()).orElse(null);
   }

   private Comparator<PlayerEntity> getComparator() {
      Comparator<PlayerEntity> var10000;
      switch (((Sort)this.sort.getValue()).ordinal()) {
         case 0:
         case 1:
            var10000 = Comparator.comparingDouble((PlayerEntity e) -> mc.player.squaredDistanceTo(e));
            break;
         case 2:
            var10000 = Comparator.comparingDouble((PlayerEntity e) -> (double)(e.getHealth() + e.getAbsorptionAmount()));
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private boolean inRange(BlockPos pos, double r) {
      return mc.player.squaredDistanceTo((double)pos.getX() + (double)0.5F, (double)pos.getY() + (double)0.5F, (double)pos.getZ() + (double)0.5F) <= r * r;
   }

   private void resetState() {
      this.pistonPos = null;
      this.redstonePos = null;
      this.crystalPos = null;
      this.lastPiston = null;
      this.face = null;
      this.usingTorch = false;
   }

   public static enum Sort {
      CLOSE_ANGLE,
      LOWEST_DISTANCE,
      LOWEST_HEALTH;

      // $FF: synthetic method
      private static Sort[] $values() {
         return new Sort[]{CLOSE_ANGLE, LOWEST_DISTANCE, LOWEST_HEALTH};
      }
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
