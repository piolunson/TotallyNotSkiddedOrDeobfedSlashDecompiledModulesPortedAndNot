package net.hachimi.client.impl.module.player;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BlockListConfig;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.BlockPlacerModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;

public final class ScaffoldModule extends BlockPlacerModule {
   Config<Boolean> rotateHoldConfig = this.register(new BooleanConfig("RotateHold", "Holds rotations to scaffold blocks", false, () -> this.rotateConfig.getValue()));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Uses grim interactions", false));
   Config<Boolean> grimNewConfig = this.register(new BooleanConfig("GrimV3", "Uses grim new interactions", false));
   Config<Selection> selectionConfig;
   Config<List<Block>> whitelistConfig;
   Config<List<Block>> blacklistConfig;
   Config<Boolean> keepYConfig;
   Config<Boolean> towerConfig;
   Config<BlockPicker> pickerConfig;
   Config<Boolean> renderConfig;
   Config<Integer> fadeTimeConfig;
   Config<Boolean> stopMotionConfig;
   private final Map<BlockPos, Animation> fadeList;
   private BlockData blockData;
   private BlockData renderData;
   private float[] lastAngles;
   private int groundPosY;

   public ScaffoldModule() {
      super("Scaffold", "Places blocks at the players feet", ModuleCategory.PLAYER, 790);
      this.selectionConfig = this.register(new EnumConfig("Selection", "The selection of blocks to use for scaffold", ScaffoldModule.Selection.ALL, ScaffoldModule.Selection.values()));
      this.whitelistConfig = this.register(new BlockListConfig("Whitelist", "Valid block whitelist", new Block[]{Blocks.DIRT, Blocks.OBSIDIAN}));
      this.blacklistConfig = this.register(new BlockListConfig("Blacklist", "Valid block blacklist", new Block[]{Blocks.SHULKER_BOX}));
      this.keepYConfig = this.register(new BooleanConfig("KeepY", "Keeps the same y-level", false));
      this.towerConfig = this.register(new BooleanConfig("Tower", "Goes up faster when holding down space", true, () -> !(Boolean)this.grimNewConfig.getValue()));
      this.pickerConfig = this.register(new EnumConfig("BlockSelection", "How to pick a block from the hotbar", ScaffoldModule.BlockPicker.NORMAL, ScaffoldModule.BlockPicker.values()));
      this.renderConfig = this.register(new BooleanConfig("Render", "Renders where scaffold is placing blocks", false));
      this.fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Timer for the fade", 0, 250, 1000, () -> false));
      this.stopMotionConfig = this.register(new BooleanConfig("StopMotion", "Stops player motion when placing blocks", false));
      this.fadeList = new HashMap<>();
   }

   protected void onDisable() {
      if (mc.player != null) {
         Managers.INVENTORY.syncToClient();
      }

      this.groundPosY = -1;
      this.lastAngles = null;
      this.blockData = null;
      this.renderData = null;
      this.fadeList.clear();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!(Boolean)this.multitaskConfig.getValue() && this.checkMultitask()) {
         this.blockData = null;
         this.renderData = null;
      } else {
         BlockSlot blockSlot = this.getBlockSlot();
         int slot = blockSlot.slot();
         if (slot == -1) {
            this.blockData = null;
            this.renderData = null;
         } else {
            this.renderData = this.getBlockData(false);
            this.blockData = this.getBlockData((Boolean)this.rotateHoldConfig.getValue());
            if (this.blockData == null) {
               if ((Boolean)this.grimNewConfig.getValue() && (Boolean)this.rotateConfig.getValue()) {
                  float yaw = mc.player.getYaw();
                  if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
                     if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
                        yaw -= 45.0F;
                     } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
                        yaw += 45.0F;
                     }
                  } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
                     yaw += 180.0F;
                     if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
                        yaw += 45.0F;
                     } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
                        yaw -= 45.0F;
                     }
                  } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
                     yaw -= 90.0F;
                  } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
                     yaw += 90.0F;
                  }

                  this.setRotation(MathHelper.wrapDegrees(yaw), 90.0F);
               }

            } else {
               this.calcRotations(this.blockData);
               if (this.blockData.getAngles() == null) {
                  if (!this.isGrim() && (Boolean)this.rotateConfig.getValue() && this.lastAngles != null) {
                     this.setRotation(this.lastAngles[0], this.lastAngles[1]);
                  }

               } else {
                  if (!this.isGrim() && Managers.INVENTORY.getServerSlot() != slot) {
                     Managers.INVENTORY.setSlot(slot);
                  }

                  Vec3d prevMotion = mc.player.getVelocity();
                  if ((Boolean)this.stopMotionConfig.getValue()) {
                     mc.player.setVelocity((double)0.0F, (double)0.0F, (double)0.0F);
                  }

                  boolean result = Managers.INTERACT.placeBlock((BlockPos)this.blockData.getBlockPos(), slot, false, false, false, (state, angles) -> {
                     if ((Boolean)this.rotateConfig.getValue()) {
                        float[] rotations = this.blockData.getAngles();
                        if (rotations == null) {
                           return;
                        }

                        this.lastAngles = rotations;
                        if (state) {
                           if ((Boolean)this.grimConfig.getValue()) {
                              Managers.ROTATION.setRotationSilent(rotations[0], rotations[1]);
                           } else {
                              this.setRotation(rotations[0], rotations[1]);
                           }
                        } else if ((Boolean)this.grimConfig.getValue()) {
                           Managers.ROTATION.setRotationSilentSync();
                        }
                     }

                  });
                  if ((Boolean)this.stopMotionConfig.getValue()) {
                     mc.player.setVelocity(prevMotion);
                  }

                  if (result && !this.isGrim() && (Boolean)this.towerConfig.getValue() && mc.options.jumpKey.isPressed()) {
                     Vec3d velocity = mc.player.getVelocity();
                     double velocityY = velocity.y;
                     if (mc.player.isOnGround() || velocityY < 0.1 || velocityY <= 0.16477328182606651) {
                        mc.player.setVelocity(velocity.x, (double)0.42F, velocity.z);
                     }
                  }

               }
            }
         }
      }
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
         if (this.renderData == null || this.renderData.getHitResult() == null) {
            return;
         }

         if ((Boolean)this.renderConfig.getValue()) {
            Animation animation = new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue());
            this.fadeList.put(this.renderData.getBlockPos(), animation);
         }

         this.fadeList.entrySet().removeIf((e) -> ((Animation)e.getValue()).getFactor() == (double)0.0F);
      }

   }

   private void calcRotations(BlockData blockData) {
      BlockPos pos = blockData.getHitResult().getBlockPos();
      Direction side = blockData.getHitResult().getSide();
      Vec3d basicHitVec = pos.toCenterPos().add((double)((float)side.getOffsetX() * 0.5F), (double)((float)side.getOffsetY() * 0.5F), (double)((float)side.getOffsetZ() * 0.5F));
      blockData.setAngles(RotationUtil.getRotationsTo(mc.player.getEyePos(), basicHitVec));
      blockData.setHitResult(new BlockHitResult(basicHitVec, side, pos, false));
   }

   private BlockData getBlockData(boolean hold) {
      int posY = (int)Math.round(mc.player.getY()) - 1;
      if ((Boolean)this.keepYConfig.getValue() && MovementUtil.isInputtingMovement()) {
         if (mc.player.isOnGround() || this.groundPosY == -1) {
            this.groundPosY = (int)Math.floor(mc.player.getY()) - 1;
         }

         posY = this.groundPosY;
      }

      BlockPos pos = PositionUtil.getRoundedBlockPos(mc.player.getX(), (double)posY, mc.player.getZ());
      if (!hold && !mc.world.getBlockState(pos).isReplaceable()) {
         return null;
      } else {
         for(Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            if (!mc.world.getBlockState(neighbor).isReplaceable()) {
               return ScaffoldModule.BlockData.basic(neighbor, direction.getOpposite());
            }
         }

         for(Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            if (mc.world.getBlockState(neighbor).isReplaceable()) {
               for(Direction direction1 : Direction.values()) {
                  BlockPos neighbor1 = neighbor.offset(direction1);
                  if (!mc.world.getBlockState(neighbor1).isReplaceable()) {
                     return ScaffoldModule.BlockData.basic(neighbor1, direction1.getOpposite());
                  }
               }
            }
         }

         return null;
      }
   }

   private BlockSlot getBlockSlot() {
      ItemStack serverStack = Managers.INVENTORY.getServerItem();
      if (!serverStack.isEmpty()) {
         Item var3 = serverStack.getItem();
         if (var3 instanceof BlockItem) {
            BlockItem blockItem = (BlockItem)var3;
            if (this.validScaffoldBlock(blockItem.getBlock())) {
               return new BlockSlot(blockItem.getBlock(), Managers.INVENTORY.getServerSlot());
            }
         }
      }

      Block block = null;
      int blockSlot = -1;
      int count = 0;

      for(int i = 0; i < 9; ++i) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (!itemStack.isEmpty()) {
            Item var8 = itemStack.getItem();
            if (var8 instanceof BlockItem) {
               BlockItem blockItem = (BlockItem)var8;
               if (this.validScaffoldBlock(blockItem.getBlock())) {
                  if (this.pickerConfig.getValue() == ScaffoldModule.BlockPicker.NORMAL) {
                     return new BlockSlot(blockItem.getBlock(), i);
                  }

                  if (blockSlot == -1 || itemStack.getCount() > count) {
                     block = blockItem.getBlock();
                     blockSlot = i;
                     count = itemStack.getCount();
                  }
               }
            }
         }
      }

      return new BlockSlot(block, blockSlot);
   }

   private boolean validScaffoldBlock(Block block) {
      boolean var10000;
      switch (((Selection)this.selectionConfig.getValue()).ordinal()) {
         case 0 -> var10000 = ((BlockListConfig)this.whitelistConfig).contains(block);
         case 1 -> var10000 = !((BlockListConfig)this.blacklistConfig).contains(block);
         case 2 -> var10000 = true;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public boolean isGrim() {
      return (Boolean)this.grimConfig.getValue() || (Boolean)this.grimNewConfig.getValue();
   }

   private static class BlockData {
      private BlockHitResult hitResult;
      private float[] angles;

      public BlockData(BlockHitResult hitResult, float[] angles) {
         this.hitResult = hitResult;
         this.angles = angles;
      }

      public BlockHitResult getHitResult() {
         return this.hitResult;
      }

      public BlockPos getBlockPos() {
         return this.hitResult.getBlockPos().offset(this.hitResult.getSide());
      }

      public void setHitResult(BlockHitResult hitResult) {
         this.hitResult = hitResult;
      }

      public float[] getAngles() {
         return this.angles;
      }

      public void setAngles(float[] angles) {
         this.angles = angles;
      }

      public static BlockData basic(BlockPos pos, Direction direction) {
         return new BlockData(new BlockHitResult(pos.toCenterPos(), direction, pos, false), (float[])null);
      }
   }

   public static enum Selection {
      WHITELIST,
      BLACKLIST,
      ALL;

      // $FF: synthetic method
      private static Selection[] $values() {
         return new Selection[]{WHITELIST, BLACKLIST, ALL};
      }
   }

   private static enum BlockPicker {
      NORMAL,
      GREATEST;

      // $FF: synthetic method
      private static BlockPicker[] $values() {
         return new BlockPicker[]{NORMAL, GREATEST};
      }
   }

   private static record BlockSlot(Block block, int slot) {
   }
}
