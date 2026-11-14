package net.hachimi.client.impl.module.player;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.network.AttackBlockEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorClientPlayerInteractionManager;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.collection.FirstOutQueue;
import net.hachimi.client.util.player.EnchantmentUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.render.ColorUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public class SpeedmineModule extends CombatModule {
   private static SpeedmineModule INSTANCE;
   Config<SpeedmineMode> modeConfig;
   Config<Boolean> multitaskConfig;
   Config<Boolean> doubleBreakConfig;
   Config<Float> rangeConfig;
   Config<Float> speedConfig;
   Config<Boolean> instantConfig;
   Config<Swap> swapConfig;
   Config<Boolean> rotateConfig;
   Config<Boolean> switchResetConfig;
   Config<Boolean> grimConfig;
   Config<Boolean> grimNewConfig;
   Config<Color> colorConfig;
   Config<Color> colorDoneConfig;
   Config<Boolean> smoothColorConfig;
   Config<Boolean> renderConfig;
   Config<BoxMode> boxModeConfig;
   Config<Color> startSideConfig;
   Config<Color> startLineConfig;
   Config<Easing> easingConfig;
   Config<Boolean> blockOutlineConfig;
   Config<Boolean> boxAnimationConfig;
   Config<Integer> fadeTimeConfig;
   Config<Boolean> showProgressConfig;
   Config<Boolean> progressBarConfig;
   Config<Float> progressScaleConfig;
   private final Map<MiningData, Animation> fadeList;
   private FirstOutQueue<MiningData> miningQueue;
   private long lastBreak;

   public SpeedmineModule() {
      super("Speedmine", "Mines blocks faster", ModuleCategory.PLAYER, 900);
      this.modeConfig = this.register(new EnumConfig("Mode", "The mining mode for speedmine", SpeedmineModule.SpeedmineMode.PACKET, SpeedmineModule.SpeedmineMode.values()));
      this.multitaskConfig = this.register(new BooleanConfig("Multitask", "Allows mining while using items", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.doubleBreakConfig = this.register(new BooleanConfig("DoubleBreak", "Allows you to mine two blocks at once", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.rangeConfig = this.register(new NumberConfig("Range", "The range to mine blocks", 0.1F, 4.0F, 6.0F, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.speedConfig = this.register(new NumberConfig("Speed", "The speed to mine blocks", 0.1F, 1.0F, 1.0F));
      this.instantConfig = this.register(new BooleanConfig("Instant", "Instantly mines already broken blocks", false));
      this.swapConfig = this.register(new EnumConfig("AutoSwap", "Swaps to the best tool once the mining is complete", SpeedmineModule.Swap.SILENT, SpeedmineModule.Swap.values(), () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates when mining the block", true, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.switchResetConfig = this.register(new BooleanConfig("SwitchReset", "Resets mining after switching items", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.grimConfig = this.register(new BooleanConfig("Grim", "Uses grim block breaking speeds", false));
      this.grimNewConfig = this.register(new BooleanConfig("GrimV3", "Uses new grim block breaking speeds", false, () -> this.grimConfig.getValue()));
      this.colorConfig = this.register(new ColorConfig("MineColor", "The mine render color", Color.RED, false, false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.colorDoneConfig = this.register(new ColorConfig("DoneColor", "The done render color", Color.GREEN, false, false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.smoothColorConfig = this.register(new BooleanConfig("ColorSmooth", "Interpolates from start to done color", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.renderConfig = this.register(new BooleanConfig("Render", "Renders the mining progress box", true, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET));
      this.boxModeConfig = this.register(new EnumConfig("BoxMode", "Which boxes are rendered", SpeedmineModule.BoxMode.END, SpeedmineModule.BoxMode.values(), () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue()));
      this.startSideConfig = this.register(new ColorConfig("StartSide", "The fill color of the start box", new Color(255, 255, 0), false, false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue() && (this.boxModeConfig.getValue() == SpeedmineModule.BoxMode.BOTH || this.boxModeConfig.getValue() == SpeedmineModule.BoxMode.START)));
      this.startLineConfig = this.register(new ColorConfig("StartLine", "The outline color of the start box", new Color(255, 255, 0), false, false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue() && (this.boxModeConfig.getValue() == SpeedmineModule.BoxMode.BOTH || this.boxModeConfig.getValue() == SpeedmineModule.BoxMode.START)));
      this.easingConfig = this.register(new EnumConfig("Easing", "The animation easing curve for the progress box", SpeedmineModule.Easing.LINEAR, SpeedmineModule.Easing.values(), () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue()));
      this.blockOutlineConfig = this.register(new BooleanConfig("BlockOutline", "Renders the outline of the mining boxes", true, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue()));
      this.boxAnimationConfig = this.register(new BooleanConfig("BoxAnimation", "Animates the progress box size based on mining progress", true, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue()));
      this.fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "The time in milliseconds to fade the render in/out", 0, 250, 1000, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue()));
      this.showProgressConfig = this.register(new BooleanConfig("ShowProgress", "Shows the mining progress percentage above the block", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue()));
      this.progressBarConfig = this.register(new BooleanConfig("ProgressBar", "Shows a text-based progress bar with the percentage", false, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue() && (Boolean)this.showProgressConfig.getValue()));
      this.progressScaleConfig = this.register(new NumberConfig("ProgressScale", "The scale of the progress text", 0.5F, 1.0F, 3.0F, () -> this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue() && (Boolean)this.showProgressConfig.getValue()));
      this.fadeList = new HashMap<>();
      this.miningQueue = new FirstOutQueue<MiningData>(2);
      INSTANCE = this;
   }

   public static SpeedmineModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      if (this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         MiningData miningData = (MiningData)this.miningQueue.peek();
         if (miningData != null) {
            return String.format("%.1f", Math.min(miningData.getBlockDamage(), 1.0F));
         }
      }

      return super.getModuleData();
   }

   protected void onDisable() {
      this.miningQueue.clear();
      this.fadeList.clear();
      Managers.INVENTORY.syncToClient();
   }

   public void onEnable() {
      if ((Boolean)this.doubleBreakConfig.getValue()) {
         this.miningQueue = new FirstOutQueue<MiningData>(2);
      } else {
         this.miningQueue = new FirstOutQueue<MiningData>(1);
      }

   }

   @EventListener
   public void onPlayerTick(TickEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator() && event.getStage() == StageEvent.EventStage.PRE) {
         if (this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.DAMAGE) {
            AccessorClientPlayerInteractionManager interactionManager = (AccessorClientPlayerInteractionManager)(Object)mc.interactionManager;
            if (interactionManager.hookGetCurrentBreakingProgress() >= (Float)this.speedConfig.getValue()) {
               interactionManager.hookSetCurrentBreakingProgress(1.0F);
            }

         } else if (!this.miningQueue.isEmpty()) {
            for(MiningData data : this.miningQueue) {
               if (data.getState().isAir()) {
                  data.resetBreakTime();
               }

               if (!this.isDataPacketMine(data) || !data.getState().isAir() && (!data.hasAttemptedBreak() || !data.passedAttemptedBreakTime(500L))) {
                  float damageDelta = this.calcBlockBreakingDelta(data.getState(), mc.world, data.getPos());
                  data.damage(damageDelta);
                  if (this.isDataPacketMine(data) && data.getBlockDamage() >= 1.0F && data.getSlot() != -1) {
                     if (mc.player.isUsingItem() && !(Boolean)this.multitaskConfig.getValue()) {
                        return;
                     }

                     if (data.getSlot() != Managers.INVENTORY.getServerSlot()) {
                        Managers.INVENTORY.setSlot(data.getSlot());
                     }

                     if (!data.hasAttemptedBreak()) {
                        data.setAttemptedBreak(true);
                     }
                  }
               } else {
                  Managers.INVENTORY.syncToClient();
                  this.miningQueue.remove(data);
               }
            }

            MiningData miningData2 = this.miningQueue.getFirst();
            double distance = mc.player.getEyePos().squaredDistanceTo(miningData2.getPos().toCenterPos());
            if (distance > ((NumberConfig)this.rangeConfig).getValueSq()) {
               this.miningQueue.remove(miningData2);
            } else if (!miningData2.getState().isAir()) {
               if (miningData2.getBlockDamage() >= (Float)this.speedConfig.getValue() && miningData2.hasAttemptedBreak() && miningData2.passedAttemptedBreakTime(500L)) {
                  this.abortMining(miningData2);
                  this.miningQueue.remove(miningData2);
               }

               if (miningData2.getBlockDamage() >= (Float)this.speedConfig.getValue()) {
                  if (mc.player.isUsingItem() && !(Boolean)this.multitaskConfig.getValue()) {
                     return;
                  }

                  this.stopMining(miningData2);
                  if (!(Boolean)this.instantConfig.getValue()) {
                     this.miningQueue.remove(miningData2);
                  }

                  if (!miningData2.hasAttemptedBreak()) {
                     miningData2.setAttemptedBreak(true);
                  }
               }

            }
         }
      }
   }

   @EventListener
   public void onAttackBlock(AttackBlockEvent event) {
      if (!mc.player.isCreative() && !mc.player.isSpectator() && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         event.cancel();
         if (event.getState().getBlock().getHardness() != -1.0F && !event.getState().isAir()) {
            this.startManualMine(event.getPos(), event.getDirection());
            mc.player.swingHand(Hand.MAIN_HAND);
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket packet) {
         if (packet.getAction() == net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.DAMAGE && (Boolean)this.grimConfig.getValue()) {
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos().up(500), packet.getDirection()));
         }
      }

      if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket && (Boolean)this.switchResetConfig.getValue() && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         for(MiningData data : this.miningQueue) {
            data.resetDamage();
         }
      }

   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET) {
         Packet var4 = event.getPacket();
         if (var4 instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket)var4;
            this.handleBlockUpdatePacket(packet);
         } else {
            var4 = event.getPacket();
            if (var4 instanceof BundleS2CPacket) {
               BundleS2CPacket packet = (BundleS2CPacket)var4;

               for(Packet<?> packet1 : packet.getPackets()) {
                  if (packet1 instanceof BlockUpdateS2CPacket) {
                     BlockUpdateS2CPacket packet2 = (BlockUpdateS2CPacket)packet1;
                     this.handleBlockUpdatePacket(packet2);
                  }
               }
            }
         }

      }
   }

   private void handleBlockUpdatePacket(BlockUpdateS2CPacket packet) {
      if (packet.getState().isAir()) {
         for(MiningData data : this.miningQueue) {
            if (data.hasAttemptedBreak() && data.getPos().equals(packet.getPos())) {
               data.setAttemptedBreak(false);
            }
         }

      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && event.getConfig() == this.doubleBreakConfig) {
         if ((Boolean)this.doubleBreakConfig.getValue()) {
            this.miningQueue = new FirstOutQueue<MiningData>(2);
         } else {
            this.miningQueue = new FirstOutQueue<MiningData>(1);
         }
      }

   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (!mc.player.isCreative() && this.modeConfig.getValue() == SpeedmineModule.SpeedmineMode.PACKET && (Boolean)this.renderConfig.getValue()) {
         RenderBuffers.preRender();

         for(Map.Entry<MiningData, Animation> set : this.fadeList.entrySet()) {
            MiningData data = (MiningData)set.getKey();
            Animation animation = (Animation)set.getValue();
            animation.setState(false);
            int boxAlpha = (int)((double)40.0F * animation.getFactor());
            int lineAlpha = (int)((double)100.0F * animation.getFactor());
            BlockPos mining = data.getPos();
            VoxelShape outlineShape = data.getState().getOutlineShape(mc.world, mining);
            outlineShape = outlineShape.isEmpty() ? VoxelShapes.fullCube() : outlineShape;
            Box render1 = outlineShape.getBoundingBox();
            Box render = new Box((double)mining.getX() + render1.minX, (double)mining.getY() + render1.minY, (double)mining.getZ() + render1.minZ, (double)mining.getX() + render1.maxX, (double)mining.getY() + render1.maxY, (double)mining.getZ() + render1.maxZ);
            Vec3d center = render.getCenter();
            double dx = (render1.maxX - render1.minX) / (double)2.0F;
            double dy = (render1.maxY - render1.minY) / (double)2.0F;
            double dz = (render1.maxZ - render1.minZ) / (double)2.0F;
            float total = this.isDataPacketMine(data) ? 1.0F : (Float)this.speedConfig.getValue();
            float rawProgress = data.getState().isAir() ? 1.0F : MathHelper.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * event.getTickDelta()) / total, 0.0F, 1.0F);
            float progress = this.applyEasing(rawProgress);
            BoxMode boxMode = this.boxModeConfig.getValue();
            if (boxMode == SpeedmineModule.BoxMode.BOTH || boxMode == SpeedmineModule.BoxMode.START) {
               int sideColor = ((ColorConfig)this.startSideConfig).getRgb(boxAlpha);
               int lineColor = ((ColorConfig)this.startLineConfig).getRgb(lineAlpha);
               RenderManager.renderBox(event.getMatrices(), render, sideColor);
               if ((Boolean)this.blockOutlineConfig.getValue()) {
                  RenderManager.renderBoundingBox(event.getMatrices(), render, 1.5F, lineColor);
               }
            }

            if (boxMode == SpeedmineModule.BoxMode.BOTH || boxMode == SpeedmineModule.BoxMode.END) {
               int sideColor;
               int lineColor;
               if ((Boolean)this.smoothColorConfig.getValue()) {
                  sideColor = data.getState().isAir() ? ((ColorConfig)this.colorDoneConfig).getRgb(boxAlpha) : ColorUtil.interpolateColor(Math.min(data.getBlockDamage(), 1.0F), ((ColorConfig)this.colorDoneConfig).getValue(boxAlpha), ((ColorConfig)this.colorConfig).getValue(boxAlpha)).getRGB();
                  lineColor = data.getState().isAir() ? ((ColorConfig)this.colorDoneConfig).getRgb(lineAlpha) : ColorUtil.interpolateColor(Math.min(data.getBlockDamage(), 1.0F), ((ColorConfig)this.colorDoneConfig).getValue(lineAlpha), ((ColorConfig)this.colorConfig).getValue(lineAlpha)).getRGB();
               } else {
                  sideColor = !(data.getBlockDamage() >= 0.95F) && !data.getState().isAir() ? ((ColorConfig)this.colorConfig).getRgb(boxAlpha) : ((ColorConfig)this.colorDoneConfig).getRgb(boxAlpha);
                  lineColor = !(data.getBlockDamage() >= 0.95F) && !data.getState().isAir() ? ((ColorConfig)this.colorConfig).getRgb(lineAlpha) : ((ColorConfig)this.colorDoneConfig).getRgb(lineAlpha);
               }

               float scale = (Boolean)this.boxAnimationConfig.getValue() ? progress : 1.0F;
               Box scaled = (new Box(center, center)).expand(dx * (double)scale, dy * (double)scale, dz * (double)scale);
               RenderManager.renderBox(event.getMatrices(), scaled, sideColor);
               if ((Boolean)this.blockOutlineConfig.getValue()) {
                  RenderManager.renderBoundingBox(event.getMatrices(), scaled, 1.5F, lineColor);
               }
            }

            if ((Boolean)this.showProgressConfig.getValue()) {
               String text;
               if (!(Boolean)this.progressBarConfig.getValue()) {
                  text = String.format("%.0f%%", rawProgress * 100.0F);
               } else {
                  int barLength = 10;
                  int filled = Math.round(rawProgress * (float)barLength);
                  StringBuilder bar = new StringBuilder("[");

                  for(int i = 0; i < barLength; ++i) {
                     bar.append((char)(i < filled ? '|' : ' '));
                  }

                  bar.append("] ").append(String.format("%.0f%%", rawProgress * 100.0F));
                  text = bar.toString();
               }

               Vec3d textPos = center.add((double)0.0F, (double)0.0F, (double)0.0F);
               int textColor = ((ColorConfig)this.colorDoneConfig).getRgb(255);
               float scaling = 0.025F * (Float)this.progressScaleConfig.getValue();
               RenderManager.renderSign(text, textPos, scaling, textColor);
            }
         }

         for(MiningData data : this.miningQueue) {
            if (!data.getState().isAir()) {
               Animation animation = new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue());
               this.fadeList.put(data, animation);
            }
         }

         this.fadeList.entrySet().removeIf((e) -> ((Animation)e.getValue()).getFactor() == (double)0.0F);
         RenderBuffers.postRender();
      }
   }

   private float applyEasing(float t) {
      t = MathHelper.clamp(t, 0.0F, 1.0F);
      float var10000;
      switch (((Easing)this.easingConfig.getValue()).ordinal()) {
         case 0 -> var10000 = t;
         case 1 -> var10000 = t * t;
         case 2 -> var10000 = 1.0F - (1.0F - t) * (1.0F - t);
         case 3 -> var10000 = t < 0.5F ? 2.0F * t * t : 1.0F - (float)Math.pow((double)-2.0F * (double)t + (double)2.0F, (double)2.0F) / 2.0F;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private void startManualMine(BlockPos pos, Direction direction) {
      this.clickMine(new MiningData(pos, direction));
   }

   public void clickMine(MiningData miningData) {
      int queueSize = this.miningQueue.size();
      if (queueSize <= 2) {
         this.queueMiningData(miningData);
      }

   }

   private void queueMiningData(MiningData data) {
      if (!data.getState().isAir()) {
         if (this.startMining(data)) {
            if (this.miningQueue.stream().anyMatch((p1) -> data.getPos().equals(p1.getPos()))) {
               return;
            }

            this.miningQueue.addFirst(data);
         }

      }
   }

   private boolean startMining(MiningData data) {
      if (data.isStarted()) {
         return false;
      } else {
         data.setStarted();
         if ((Boolean)this.grimNewConfig.getValue()) {
            if (!AnticheatModule.getInstance().getMiningFix()) {
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            } else {
               Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            }

            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return true;
         } else {
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return true;
         }
      }
   }

   private void abortMining(MiningData data) {
      if (data.isStarted() && !data.getState().isAir()) {
         Managers.NETWORK.sendSequencedPacket((id) -> new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection(), id));
         Managers.INVENTORY.syncToClient();
      }
   }

   private void stopMining(MiningData data) {
      if (data.isStarted() && !data.getState().isAir()) {
         if ((Boolean)this.rotateConfig.getValue()) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), data.getPos().toCenterPos());
            if ((Boolean)this.grimConfig.getValue()) {
               this.setRotationSilent(rotations[0], rotations[1]);
            } else {
               this.setRotation(rotations[0], rotations[1]);
            }
         }

         int slot = data.getSlot();
         boolean canSwap = slot != -1 && slot != Managers.INVENTORY.getServerSlot();
         if (canSwap) {
            this.swapTo(slot);
         }

         this.stopMiningInternal(data);
         this.lastBreak = System.currentTimeMillis();
         if (canSwap) {
            this.swapSync(slot);
         }

         if ((Boolean)this.rotateConfig.getValue()) {
            Managers.ROTATION.setRotationSilentSync();
         }

      }
   }

   private void swapTo(int slot) {
      switch (((Swap)this.swapConfig.getValue()).ordinal()) {
         case 0 -> Managers.INVENTORY.setClientSlot(slot);
         case 1 -> Managers.INVENTORY.setSlot(slot);
         case 2 -> Managers.INVENTORY.setSlotAlt(slot);
      }

   }

   private void swapSync(int slot) {
      switch (((Swap)this.swapConfig.getValue()).ordinal()) {
         case 1 -> Managers.INVENTORY.syncToClient();
         case 2 -> Managers.INVENTORY.setSlotAlt(slot);
      }

   }

   private void stopMiningInternal(MiningData data) {
      Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
      Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
   }

   public boolean isBlockDelayGrim() {
      return System.currentTimeMillis() - this.lastBreak <= 280L && (Boolean)this.grimConfig.getValue();
   }

   private boolean isDataPacketMine(MiningData data) {
      return this.miningQueue.size() == 2 && data == this.miningQueue.getLast();
   }

   public float calcBlockBreakingDelta(BlockState state, BlockView world, BlockPos pos) {
      if (this.swapConfig.getValue() == SpeedmineModule.Swap.OFF) {
         return state.calcBlockBreakingDelta(mc.player, mc.world, pos);
      } else {
         float f = state.getHardness(world, pos);
         if (f == -1.0F) {
            return 0.0F;
         } else {
            int i = this.canHarvest(state) ? 30 : 100;
            return this.getBlockBreakingSpeed(state) / f / (float)i;
         }
      }
   }

   private float getBlockBreakingSpeed(BlockState block) {
      int tool = AutoToolModule.getInstance().getBestTool(block);
      float f = mc.player.getInventory().getStack(tool).getMiningSpeedMultiplier(block);
      if (f > 1.0F) {
         ItemStack stack = mc.player.getInventory().getStack(tool);
         int i = EnchantmentUtil.getLevel(stack, Enchantments.EFFICIENCY);
         if (i > 0 && !stack.isEmpty()) {
            f += (float)(i * i + 1);
         }
      }

      if (StatusEffectUtil.hasHaste(mc.player)) {
         f *= 1.0F + (float)(StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
      }

      if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         float var10000;
         switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> var10000 = 0.3F;
            case 1 -> var10000 = 0.09F;
            case 2 -> var10000 = 0.0027F;
            default -> var10000 = 8.1E-4F;
         }

         float g = var10000;
         f *= g;
      }

      if (!mc.player.isOnGround()) {
         f /= 5.0F;
      }

      return f;
   }

   private boolean canHarvest(BlockState state) {
      if (state.isToolRequired()) {
         int tool = AutoToolModule.getInstance().getBestTool(state);
         return mc.player.getInventory().getStack(tool).isSuitableFor(state);
      } else {
         return true;
      }
   }

   public boolean isMining() {
      return !this.miningQueue.isEmpty();
   }

   public static class MiningData {
      private boolean attemptedBreak;
      private long breakTime;
      private final BlockPos pos;
      private final Direction direction;
      private float lastDamage;
      private float blockDamage;
      private boolean started;

      public MiningData(BlockPos pos, Direction direction) {
         this.pos = pos;
         this.direction = direction;
      }

      public void setAttemptedBreak(boolean attemptedBreak) {
         this.attemptedBreak = attemptedBreak;
         if (attemptedBreak) {
            this.resetBreakTime();
         }

      }

      public void resetBreakTime() {
         this.breakTime = System.currentTimeMillis();
      }

      public boolean hasAttemptedBreak() {
         return this.attemptedBreak;
      }

      public boolean passedAttemptedBreakTime(long time) {
         return System.currentTimeMillis() - this.breakTime >= time;
      }

      public float damage(float dmg) {
         this.lastDamage = this.blockDamage;
         this.blockDamage += dmg;
         return this.blockDamage;
      }

      public void setDamage(float blockDamage) {
         this.blockDamage = blockDamage;
      }

      public void resetDamage() {
         this.started = false;
         this.blockDamage = 0.0F;
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public Direction getDirection() {
         return this.direction;
      }

      public int getSlot() {
         return AutoToolModule.getInstance().getBestToolNoFallback(this.getState());
      }

      public BlockState getState() {
         return Globals.mc.world.getBlockState(this.pos);
      }

      public float getBlockDamage() {
         return this.blockDamage;
      }

      public float getLastDamage() {
         return this.lastDamage;
      }

      public boolean isStarted() {
         return this.started;
      }

      public void setStarted() {
         this.started = true;
      }
   }

   public static enum SpeedmineMode {
      PACKET,
      DAMAGE;

      // $FF: synthetic method
      private static SpeedmineMode[] $values() {
         return new SpeedmineMode[]{PACKET, DAMAGE};
      }
   }

   public static enum Swap {
      NORMAL,
      SILENT,
      SILENT_ALT,
      OFF;

      // $FF: synthetic method
      private static Swap[] $values() {
         return new Swap[]{NORMAL, SILENT, SILENT_ALT, OFF};
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

   public static enum BoxMode {
      BOTH,
      START,
      END,
      NONE;

      // $FF: synthetic method
      private static BoxMode[] $values() {
         return new BoxMode[]{BOTH, START, END, NONE};
      }
   }

   public static enum Easing {
      LINEAR,
      EASE_IN,
      EASE_OUT,
      EASE_IN_OUT;

      // $FF: synthetic method
      private static Easing[] $values() {
         return new Easing[]{LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT};
      }
   }
}
