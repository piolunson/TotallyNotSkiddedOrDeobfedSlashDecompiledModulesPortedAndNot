package net.hachimi.client.impl.manager.player.interaction;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.impl.module.player.AirPlaceModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.world.SneakBlocks;
import net.hachimi.eventbus.EventBus;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public final class InteractionManager implements Globals {
   private final Map<Integer, Integer> placedOnEntities = new ConcurrentHashMap<>();

   public InteractionManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   public boolean canPlace(BlockPos pos, Block block) {
      BlockState state = block.getDefaultState();
      VoxelShape shape = block.getOutlineShape(state, mc.world, pos, ShapeContext.absent()).offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      if (!shape.isEmpty()) {
         for(Entity entity : mc.world.getOtherEntities((Entity)null, shape.getBoundingBox())) {
            if (!entity.isSpectator() && entity.isAlive() && VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND) && (!(entity instanceof ItemFrameEntity) || this.placedOnEntities.containsKey(entity.getId()) && (Integer)this.placedOnEntities.get(entity.getId()) > AnticheatModule.getInstance().getEntityPlaceThreshold())) {
               return false;
            }
         }
      }

      return true;
   }

   public boolean placeBlock(BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, RotationCallback rotationCallback) {
      return this.placeBlock(pos, slot, strictDirection, clientSwing, rotationCallback, false);
   }

   public boolean placeBlock(BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, RotationCallback rotationCallback, boolean airPlace) {
      BlockState airState = Blocks.AIR.getDefaultState();
      VoxelShape shape = airState.getOutlineShape(mc.world, pos, ShapeContext.absent()).offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      boolean isEntityBlockingPlacement = false;
      if (!shape.isEmpty()) {
         label61: {
            Iterator var10 = mc.world.getOtherEntities((Entity)null, shape.getBoundingBox()).iterator();

            while(true) {
               if (!var10.hasNext()) {
                  break label61;
               }

               Entity entity = (Entity)var10.next();
               if (!entity.isSpectator() && entity.isAlive() && VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND)) {
                  if (!(entity instanceof ItemFrameEntity)) {
                     break;
                  }

                  this.placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
                  if ((Integer)this.placedOnEntities.getOrDefault(entity.getId(), 0) > AnticheatModule.getInstance().getEntityPlaceThreshold()) {
                     break;
                  }
               }
            }

            isEntityBlockingPlacement = true;
         }
      }

      if (isEntityBlockingPlacement) {
         return false;
      } else {
         Direction direction = this.getInteractDirectionInternal(pos, strictDirection);
         if (!airPlace && (!AirPlaceModule.getInstance().isEnabled() || direction != null)) {
            if (direction == null) {
               return false;
            } else {
               BlockPos neighbor = pos.offset(direction.getOpposite());
               return this.placeBlock(neighbor, direction, slot, clientSwing, false, rotationCallback);
            }
         } else {
            direction = Direction.DOWN;
            return this.placeBlock(pos, direction, slot, clientSwing, AnticheatModule.getInstance().isGrim(), rotationCallback);
         }
      }
   }

   public boolean placeBlock(BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, boolean packet, RotationCallback rotationCallback) {
      return this.placeBlock(pos, slot, strictDirection, clientSwing, packet, false, rotationCallback);
   }

   public boolean placeBlock(BlockPos pos, int slot, boolean strictDirection, boolean clientSwing, boolean packet, boolean airPlace, RotationCallback rotationCallback) {
      BlockState airState = Blocks.AIR.getDefaultState();
      VoxelShape shape = airState.getOutlineShape(mc.world, pos, ShapeContext.absent()).offset((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      boolean isEntityBlockingPlacement = false;
      if (!shape.isEmpty()) {
         label61: {
            Iterator var11 = mc.world.getOtherEntities((Entity)null, shape.getBoundingBox()).iterator();

            while(true) {
               if (!var11.hasNext()) {
                  break label61;
               }

               Entity entity = (Entity)var11.next();
               if (!entity.isSpectator() && entity.isAlive() && VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND)) {
                  if (!(entity instanceof ItemFrameEntity)) {
                     break;
                  }

                  this.placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
                  if ((Integer)this.placedOnEntities.getOrDefault(entity.getId(), 0) > AnticheatModule.getInstance().getEntityPlaceThreshold()) {
                     break;
                  }
               }
            }

            isEntityBlockingPlacement = true;
         }
      }

      if (isEntityBlockingPlacement) {
         return false;
      } else {
         Direction direction = this.getInteractDirectionInternal(pos, strictDirection);
         if (!airPlace && (!AirPlaceModule.getInstance().isEnabled() || direction != null)) {
            if (direction == null) {
               return false;
            } else {
               BlockPos neighbor = pos.offset(direction.getOpposite());
               return this.placeBlock(neighbor, direction, slot, clientSwing, false, packet, rotationCallback);
            }
         } else {
            direction = Direction.DOWN;
            return this.placeBlock(pos, direction, slot, clientSwing, AnticheatModule.getInstance().isGrim(), rotationCallback);
         }
      }
   }

   public boolean placeBlock(BlockPos pos, Direction direction, int slot, boolean clientSwing, boolean grimAirPlace, boolean packet, RotationCallback rotationCallback) {
      Vec3d hitVec = pos.toCenterPos().add((new Vec3d(direction.getUnitVector())).multiply((double)0.5F));
      return this.placeBlock(new BlockHitResult(hitVec, direction, pos, false), slot, clientSwing, grimAirPlace, packet, rotationCallback);
   }

   public boolean placeBlock(BlockPos pos, Direction direction, int slot, boolean clientSwing, boolean grimAirPlace, RotationCallback rotationCallback) {
      Vec3d hitVec = pos.toCenterPos().add((new Vec3d(direction.getUnitVector())).multiply((double)0.5F));
      return this.placeBlock(new BlockHitResult(hitVec, direction, pos, false), slot, clientSwing, grimAirPlace, rotationCallback);
   }

   public boolean placeBlock(BlockHitResult hitResult, int slot, boolean clientSwing, boolean grimAirPlace, boolean packet, RotationCallback rotationCallback) {
      boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
      if (isSpoofing) {
         Managers.INVENTORY.setSlot(slot);
      }

      if (grimAirPlace) {
         Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }

      boolean isRotating = rotationCallback != null;
      if (isRotating) {
         float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitResult.getPos());
         rotationCallback.handleRotation(true, angles);
      }

      boolean result = this.placeBlockImmediately(hitResult, grimAirPlace ? Hand.OFF_HAND : Hand.MAIN_HAND, clientSwing, packet);
      if (isRotating) {
         float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitResult.getPos());
         rotationCallback.handleRotation(false, angles);
      }

      if (grimAirPlace) {
         Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }

      if (isSpoofing) {
         Managers.INVENTORY.syncToClient();
      }

      return result;
   }

   public boolean placeBlock(BlockHitResult hitResult, int slot, boolean clientSwing, boolean grimAirPlace, RotationCallback rotationCallback) {
      boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
      if (isSpoofing) {
         Managers.INVENTORY.setSlot(slot);
      }

      if (grimAirPlace) {
         Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }

      boolean isRotating = rotationCallback != null;
      if (isRotating) {
         float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitResult.getPos());
         rotationCallback.handleRotation(true, angles);
      }

      boolean result = this.placeBlockImmediately(hitResult, grimAirPlace ? Hand.OFF_HAND : Hand.MAIN_HAND, clientSwing, true);
      if (isRotating) {
         float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitResult.getPos());
         rotationCallback.handleRotation(false, angles);
      }

      if (grimAirPlace) {
         Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
      }

      if (isSpoofing) {
         Managers.INVENTORY.syncToClient();
      }

      return result;
   }

   public boolean placeBlockImmediately(BlockHitResult result, Hand hand, boolean clientSwing, boolean packet) {
      AnticheatModule ac = AnticheatModule.getInstance();
      BlockState state = mc.world.getBlockState(result.getBlockPos());
      boolean shouldSneak = (ac.isSneakingSpoof() || SneakBlocks.isSneakBlock(state)) && !mc.player.isSneaking();
      if (shouldSneak) {
         Managers.MOVEMENT.setPacketSneaking(true);
         MovementUtil.applySneak();
      }

      boolean usePacket = packet || ac.getPlacePacketMode() == AnticheatModule.PlacePacketMode.Packet;
      ActionResult actionResult = usePacket ? this.placeBlockPacket(result, hand) : this.placeBlockInternally(result, hand);
      if (actionResult.isAccepted() && actionResult.shouldSwingHand()) {
         switch (ac.getPlaceSwing()) {
            case Normal:
               if (clientSwing) {
                  mc.player.swingHand(Hand.MAIN_HAND);
               } else {
                  Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               }
               break;
            case Packet:
               Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            case None:
         }
      }

      if (shouldSneak) {
         Managers.MOVEMENT.setPacketSneaking(false);
      }

      return actionResult.isAccepted();
   }

   private ActionResult placeBlockInternally(BlockHitResult hitResult, Hand hand) {
      return mc.interactionManager.interactBlock(mc.player, hand, hitResult);
   }

   public ActionResult placeBlockPacket(BlockHitResult hitResult, Hand hand) {
      Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(hand, hitResult, id));
      return ActionResult.SUCCESS;
   }

   public Direction getInteractDirection(BlockPos blockPos, boolean strictDirection) {
      Direction direction = this.getInteractDirectionInternal(blockPos, strictDirection);
      return direction == null ? Direction.UP : direction;
   }

   public Direction getInteractDirectionInternal(BlockPos blockPos, boolean strictDirection) {
      AnticheatModule ac = AnticheatModule.getInstance();
      Vec3d eyePos = mc.player.getEyePos();
      Set<Direction> validDirections = this.getPlaceDirectionsNCP(eyePos, blockPos.toCenterPos());
      Direction interactDirection = null;
      double bestDot = (double)-2.0F;
      Direction[] candidates = Direction.values();

      for(Direction direction : candidates) {
         BlockState state = mc.world.getBlockState(blockPos.offset(direction));
         if (!state.isAir() && state.getFluidState().isEmpty() && state.getBlock() != Blocks.ANVIL && state.getBlock() != Blocks.CHIPPED_ANVIL && state.getBlock() != Blocks.DAMAGED_ANVIL && (!strictDirection || validDirections.contains(direction.getOpposite()))) {
            if (!ac.isClosestDirection()) {
               interactDirection = direction;
               break;
            }

            Vec3d faceCenter = blockPos.offset(direction).toCenterPos();
            Vec3d toEye = eyePos.subtract(faceCenter).normalize();
            Vec3d normal = new Vec3d(direction.getUnitVector());
            double dot = toEye.dotProduct(normal);
            if (dot > bestDot) {
               bestDot = dot;
               interactDirection = direction;
            }
         }
      }

      return interactDirection == null ? null : interactDirection.getOpposite();
   }

   public Direction getPlaceDirectionNCP(BlockPos blockPos, boolean visible) {
      Vec3d eyePos = new Vec3d(mc.player.getX(), mc.player.getY() + (double)mc.player.getStandingEyeHeight(), mc.player.getZ());
      if ((double)blockPos.getX() == eyePos.getX() && (double)blockPos.getY() == eyePos.getY() && (double)blockPos.getZ() == eyePos.getZ()) {
         return Direction.DOWN;
      } else {
         for(Direction dir : this.getPlaceDirectionsNCP(eyePos, blockPos.toCenterPos())) {
            if (!visible || mc.world.isAir(blockPos.offset(dir))) {
               return dir;
            }
         }

         return Direction.UP;
      }
   }

   public Set<Direction> getPlaceDirectionsNCP(Vec3d eyePos, Vec3d blockPos) {
      return this.getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
   }

   public Set<Direction> getPlaceDirectionsNCP(double x, double y, double z, double dx, double dy, double dz) {
      double xdiff = x - dx;
      double ydiff = y - dy;
      double zdiff = z - dz;
      Set<Direction> dirs = new HashSet<>(6);
      if (ydiff > (double)0.5F) {
         dirs.add(Direction.UP);
      } else if (ydiff < (double)-0.5F) {
         dirs.add(Direction.DOWN);
      } else {
         dirs.add(Direction.UP);
         dirs.add(Direction.DOWN);
      }

      if (xdiff > (double)0.5F) {
         dirs.add(Direction.EAST);
      } else if (xdiff < (double)-0.5F) {
         dirs.add(Direction.WEST);
      } else {
         dirs.add(Direction.EAST);
         dirs.add(Direction.WEST);
      }

      if (zdiff > (double)0.5F) {
         dirs.add(Direction.SOUTH);
      } else if (zdiff < (double)-0.5F) {
         dirs.add(Direction.NORTH);
      } else {
         dirs.add(Direction.SOUTH);
         dirs.add(Direction.NORTH);
      }

      return dirs;
   }

   public boolean isInEyeRange(BlockPos pos) {
      return (double)pos.getY() > mc.player.getY() + (double)mc.player.getStandingEyeHeight();
   }
}
