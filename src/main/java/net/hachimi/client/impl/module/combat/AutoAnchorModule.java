package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.BlockPlacerModule;
import net.hachimi.client.impl.module.player.AirPlaceModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.RespawnAnchorBlock;

public class AutoAnchorModule extends BlockPlacerModule {
   private static AutoAnchorModule INSTANCE;
   Config<Float> targetRangeConfig = this.register(new NumberConfig("EnemyRange", "Range to search for potential enemies", 1.0F, 10.0F, 13.0F));
   Config<Boolean> swingConfig = this.register(new BooleanConfig("Swing", "Swing hand when exploding anchors", true));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate before exploding", false));
   Config<Boolean> airPlaceConfig = this.register(new BooleanConfig("AirPlace", "Allows placing anchors against air when no valid surface is visible", false));
   Config<SwitchMode> placeSwitchConfig;
   Config<SwitchMode> explodeSwitchConfig;
   Config<Boolean> playersConfig;
   Config<Boolean> monstersConfig;
   Config<Boolean> neutralsConfig;
   Config<Boolean> animalsConfig;
   Config<Float> rangeConfig;
   Config<Float> explodeSpeedConfig;
   Config<Boolean> placeConfig;
   Config<Float> placeRangeConfig;
   Config<Float> placeSpeedConfig;
   Config<Float> placeDelayConfig;
   Config<Boolean> spamPlaceConfig;
   Config<Boolean> strictDirectionConfig;
   Config<Boolean> grimConfig;
   Config<Boolean> detectMiningConfig;
   Config<Boolean> breakCrystalConfig;
   Config<Float> breakRangeConfig;
   Config<Boolean> assumeArmorConfig;
   Config<Float> minDamageConfig;
   Config<Boolean> safetyConfig;
   Config<Float> maxLocalDamageConfig;
   Config<Float> damageRatioConfig;
   Config<Boolean> blockDestructionConfig;
   Config<Boolean> selfExtrapolateConfig;
   Config<Integer> extrapolateTicksConfig;
   Config<Boolean> asynchronousConfig;
   Config<Boolean> renderConfig;
   Config<BoxMode> boxModeConfig;
   Config<Color> boxColorConfig;
   Config<Color> fillColorConfig;
   Config<Float> widthConfig;
   Config<Boolean> shrinkConfig;
   Config<Float> sliderSpeedConfig;
   Config<Float> startFadeConfig;
   Config<Float> fadeSpeedConfig;
   Config<Boolean> renderDamageConfig;
   Config<Boolean> targetDamageConfig;
   Config<Boolean> selfDamageConfig;
   private volatile AnchorCalc anchorCalc;
   private final Timer explodeTimer;
   private final Timer placeTimer;
   private final Timer breakTimer;
   private final Map<BlockPos, Animation> fadeList;
   private final ExecutorService executor;
   private Future<?> calculationFuture;

   public AutoAnchorModule() {
      super("AutoAnchor", "Automatically places and explodes respawn anchors", ModuleCategory.COMBAT, 740);
      this.placeSwitchConfig = this.register(new EnumConfig("PlaceSwitch", "How to switch to the anchor item when placing", AutoAnchorModule.SwitchMode.Hotbar, AutoAnchorModule.SwitchMode.values()));
      this.explodeSwitchConfig = this.register(new EnumConfig("ExplodeSwitch", "How to switch to the glowstone item when exploding", AutoAnchorModule.SwitchMode.Hotbar, AutoAnchorModule.SwitchMode.values()));
      this.playersConfig = this.register(new BooleanConfig("Players", "Target players", true));
      this.monstersConfig = this.register(new BooleanConfig("Monsters", "Target monsters", false));
      this.neutralsConfig = this.register(new BooleanConfig("Neutrals", "Target neutrals", false));
      this.animalsConfig = this.register(new BooleanConfig("Animals", "Target animals", false));
      this.rangeConfig = this.register(new NumberConfig("Range", "Range to explode anchors", 0.1F, 4.0F, 6.0F));
      this.explodeSpeedConfig = this.register(new NumberConfig("ExplodeSpeed", "Speed to explode anchors", 0.1F, 18.0F, 20.0F));
      this.placeConfig = this.register(new BooleanConfig("Place", "Places anchors to damage enemies", true));
      this.placeRangeConfig = this.register(new NumberConfig("PlaceRange", "Range to place anchors", 0.1F, 4.5F, 6.0F, () -> this.placeConfig.getValue()));
      this.placeSpeedConfig = this.register(new NumberConfig("PlaceSpeed", "Speed to place anchors", 0.1F, 18.0F, 20.0F, () -> this.placeConfig.getValue()));
      this.placeDelayConfig = this.register(new NumberConfig("PlaceDelay", "Additional delay before placing anchors", 0.0F, 0.0F, 20.0F, () -> this.placeConfig.getValue()));
      this.spamPlaceConfig = this.register(new BooleanConfig("SpamPlace", "Sends multiple place interactions to fight packet loss", true, () -> this.placeConfig.getValue()));
      this.strictDirectionConfig = this.register(new BooleanConfig("StrictDirection", "Interacts with only visible directions when placing crystals", false, () -> this.placeConfig.getValue()));
      this.grimConfig = this.register(new BooleanConfig("Grim", "Places using grim instant rotations", false, () -> this.placeConfig.getValue()));
      this.detectMiningConfig = this.register(new BooleanConfig("DetectMining", "Avoids placing anchors where an enemy is currently mining", true, () -> this.placeConfig.getValue()));
      this.breakCrystalConfig = this.register(new BooleanConfig("BreakCrystal", "Mines placed anchors with no charges left to reclaim them", false));
      this.breakRangeConfig = this.register(new NumberConfig("BreakRange", "Range to break anchors", 0.1F, 4.0F, 6.0F, () -> this.breakCrystalConfig.getValue()));
      this.assumeArmorConfig = this.register(new BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", false));
      this.minDamageConfig = this.register(new NumberConfig("MinDamage", "Minimum damage required to consider exploding anchors", 1.0F, 4.0F, 10.0F));
      this.safetyConfig = this.register(new BooleanConfig("Safety", "Accounts for total player safety when exploding anchors", true));
      this.maxLocalDamageConfig = this.register(new NumberConfig("MaxLocalDamage", "The maximum player damage", 4.0F, 12.0F, 20.0F));
      this.damageRatioConfig = this.register(new NumberConfig("DamageRatio", "Minimum ratio of enemy damage to self damage required to explode", 0.1F, 1.2F, 10.0F));
      this.blockDestructionConfig = this.register(new BooleanConfig("BlockDestruction", "Accounts for explosion block destruction when calculating damages", false));
      this.selfExtrapolateConfig = this.register(new BooleanConfig("SelfExtrapolate", "Accounts for motion when calculating self damage", false));
      this.extrapolateTicksConfig = this.register(new NumberConfig("ExtrapolationTicks", "Accounts for motion when calculating enemy positions, not fully accurate.", 0, 0, 10));
      this.asynchronousConfig = this.register(new BooleanConfig("Asynchronous", "Calculates anchor placements/explosions asynchronously", true));
      this.renderConfig = this.register(new BooleanConfig("Render", "Renders where anchors will be placed", true));
      this.boxModeConfig = this.register(new EnumConfig("BoxMode", "How the render box is displayed", AutoAnchorModule.BoxMode.Both, AutoAnchorModule.BoxMode.values(), () -> this.renderConfig.getValue()));
      this.boxColorConfig = this.register(new ColorConfig("BoxColor", "The color of the render box outline", new Color(245, 138, 231, 100), true, true, () -> this.renderConfig.getValue()));
      this.fillColorConfig = this.register(new ColorConfig("FillColor", "The color of the render box fill", new Color(245, 138, 231, 70), true, true, () -> this.renderConfig.getValue()));
      this.widthConfig = this.register(new NumberConfig("Width", "The line width of the render box", 0.5F, 1.0F, 5.0F, () -> this.renderConfig.getValue()));
      this.shrinkConfig = this.register(new BooleanConfig("Shrink", "Shrinks the render box slightly", false, () -> this.renderConfig.getValue()));
      this.sliderSpeedConfig = this.register(new NumberConfig("SliderSpeed", "The speed of the box fade animation", 0.05F, 0.5F, 2.0F, () -> this.renderConfig.getValue()));
      this.startFadeConfig = this.register(new NumberConfig("StartFade", "The starting opacity factor for the fade animation", 0.0F, 0.2F, 1.0F, NumberDisplay.PERCENT, () -> this.renderConfig.getValue()));
      this.fadeSpeedConfig = this.register(new NumberConfig("FadeSpeed", "The speed of the fade-out after the box disappears", 0.05F, 0.3F, 2.0F, () -> this.renderConfig.getValue()));
      this.renderDamageConfig = this.register(new BooleanConfig("RenderDamage", "Renders calculated damage values above the box", false, () -> this.renderConfig.getValue()));
      this.targetDamageConfig = this.register(new BooleanConfig("TargetDamage", "Renders the damage that would be dealt to the target", true, () -> (Boolean)this.renderConfig.getValue() && (Boolean)this.renderDamageConfig.getValue()));
      this.selfDamageConfig = this.register(new BooleanConfig("SelfDamage", "Renders the damage that would be dealt to yourself", true, () -> (Boolean)this.renderConfig.getValue() && (Boolean)this.renderDamageConfig.getValue()));
      this.explodeTimer = new CacheTimer();
      this.placeTimer = new CacheTimer();
      this.breakTimer = new CacheTimer();
      this.fadeList = new HashMap<>();
      this.executor = Executors.newSingleThreadExecutor();
      INSTANCE = this;
   }

   public static AutoAnchorModule getInstance() {
      return INSTANCE;
   }

   public void onDisable() {
      this.anchorCalc = null;
      this.fadeList.clear();
      if (this.calculationFuture != null) {
         this.calculationFuture.cancel(true);
         this.calculationFuture = null;
      }

   }

   @EventListener
   public void onPlayerTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         if (InventoryUtil.hasItemInHotbar(Items.RESPAWN_ANCHOR) && InventoryUtil.hasItemInHotbar(Items.GLOWSTONE)) {
            if (((Boolean)this.multitaskConfig.getValue() || !this.checkMultitask()) && (!(Boolean)this.stopMotionConfig.getValue() || mc.player.isOnGround())) {
               if ((Boolean)this.asynchronousConfig.getValue()) {
                  if (this.calculationFuture == null || this.calculationFuture.isDone()) {
                     this.calculationFuture = this.executor.submit(() -> this.anchorCalc = this.calculateAnchorExplosion());
                  }
               } else {
                  this.anchorCalc = this.calculateAnchorExplosion();
               }

               AnchorCalc calc = this.anchorCalc;
               if (calc != null) {
                  BlockPos anchorPos = calc.pos();
                  if (calc.isAnchor()) {
                     if ((Boolean)this.rotateConfig.getValue()) {
                        float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), anchorPos.toCenterPos());
                        this.setRotation(rotations[0], rotations[1]);
                     }

                     if (this.explodeTimer.passed(1000.0F - (Float)this.explodeSpeedConfig.getValue() * 50.0F)) {
                        this.setAnchor(anchorPos);
                        this.explodeTimer.reset();
                     }
                  } else {
                     if (mc.world.getBlockState(anchorPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                        return;
                     }

                     int slot = this.getBlockItemSlot(Blocks.RESPAWN_ANCHOR);
                     if (slot == -1) {
                        return;
                     }

                     if (this.placeTimer.passed(1000.0F - (Float)this.placeSpeedConfig.getValue() * 50.0F + (Float)this.placeDelayConfig.getValue() * 50.0F)) {
                        this.switchToSlot(slot, this.placeSwitchConfig.getValue());
                        Vec3d prevMotion = mc.player.getVelocity();
                        if ((Boolean)this.stopMotionConfig.getValue()) {
                           mc.player.setVelocity(Vec3d.ZERO);
                        }

                        Managers.INTERACT.placeBlock(anchorPos, slot, (Boolean)this.grimConfig.getValue(), (Boolean)this.strictDirectionConfig.getValue(), false, (Boolean)this.airPlaceConfig.getValue(), (state, angles) -> {
                           if ((Boolean)this.rotateConfig.getValue()) {
                              if (state) {
                                 Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                              } else if ((Boolean)this.grimConfig.getValue()) {
                                 Managers.ROTATION.setRotationSilentSync();
                              }
                           }

                        });
                        if ((Boolean)this.spamPlaceConfig.getValue()) {
                           BlockHitResult spamResult = new BlockHitResult(anchorPos.toCenterPos(), Managers.INTERACT.getInteractDirection(anchorPos, (Boolean)this.strictDirectionConfig.getValue()), anchorPos, true);

                           for(int i = 0; i < 2; ++i) {
                              mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, spamResult);
                           }
                        }

                        if ((Boolean)this.stopMotionConfig.getValue()) {
                           mc.player.setVelocity(prevMotion);
                        }

                        Managers.INVENTORY.syncToClient();
                        this.placeTimer.reset();
                     }
                  }

                  if ((Boolean)this.breakCrystalConfig.getValue()) {
                     this.handleBreakAnchors();
                  }

               }
            } else {
               this.anchorCalc = null;
            }
         } else {
            this.anchorCalc = null;
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if ((Boolean)this.renderConfig.getValue() && this.boxModeConfig.getValue() != AutoAnchorModule.BoxMode.None) {
         RenderBuffers.preRender();
         AnchorCalc calc = this.anchorCalc;

         for(Map.Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            if (calc == null || !((BlockPos)set.getKey()).equals(calc.pos())) {
               ((Animation)set.getValue()).setState(false);
               this.renderAnchorBox(event, (BlockPos)set.getKey(), (Animation)set.getValue(), (AnchorCalc)null);
            }
         }

         RenderBuffers.postRender();
         this.fadeList.entrySet().removeIf((e) -> ((Animation)e.getValue()).getFactor() == (double)0.0F);
         if (calc != null) {
            Animation animation = (Animation)this.fadeList.computeIfAbsent(calc.pos(), (p) -> new Animation(true, (Float)this.sliderSpeedConfig.getValue() * 1000.0F));
            animation.setLength((Float)this.sliderSpeedConfig.getValue() * 1000.0F);
            animation.setState(true);
            RenderBuffers.preRender();
            this.renderAnchorBox(event, calc.pos(), animation, calc);
            RenderBuffers.postRender();
         }
      }

   }

   private void renderAnchorBox(RenderWorldEvent event, BlockPos pos, Animation animation, AnchorCalc calc) {
      double factor = Math.max(animation.getFactor(), (double)(Float)this.startFadeConfig.getValue());
      int boxAlpha = (int)((double)((Color)this.boxColorConfig.getValue()).getAlpha() * factor);
      int fillAlpha = (int)((double)((Color)this.fillColorConfig.getValue()).getAlpha() * factor);
      ColorConfig boxColor = (ColorConfig)this.boxColorConfig;
      ColorConfig fillColor = (ColorConfig)this.fillColorConfig;
      Box box = new Box(pos);
      if ((Boolean)this.shrinkConfig.getValue()) {
         box = box.contract(0.05);
      }

      BoxMode mode = this.boxModeConfig.getValue();
      if (mode == AutoAnchorModule.BoxMode.Both || mode == AutoAnchorModule.BoxMode.Fill) {
         RenderManager.renderBox(event.getMatrices(), box, fillColor.getRgb(fillAlpha));
      }

      if (mode == AutoAnchorModule.BoxMode.Both || mode == AutoAnchorModule.BoxMode.Box) {
         RenderManager.renderBoundingBox(event.getMatrices(), box, (Float)this.widthConfig.getValue(), boxColor.getRgb(boxAlpha));
      }

      if ((Boolean)this.renderDamageConfig.getValue() && calc != null) {
         StringBuilder text = new StringBuilder();
         if ((Boolean)this.targetDamageConfig.getValue()) {
            text.append(String.format("%.1f", calc.targetDamage()));
         }

         if ((Boolean)this.selfDamageConfig.getValue()) {
            if (!text.isEmpty()) {
               text.append(" / ");
            }

            text.append(String.format("%.1f", calc.selfDamage()));
         }

         if (!text.isEmpty()) {
            RenderManager.renderSign(text.toString(), pos.toCenterPos().add((double)0.0F, 1.4, (double)0.0F), boxColor.getRgb(255));
         }
      }

   }

   private void setAnchor(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      if (state.getBlock() instanceof RespawnAnchorBlock) {
         int slot1 = this.findNonBlockSlot();
         if (slot1 != -1) {
            int charges = (Integer)state.get(RespawnAnchorBlock.CHARGES);
            if (charges <= 0) {
               int slot = this.getBlockItemSlot(Blocks.GLOWSTONE);
               if (slot == -1) {
                  return;
               }

               this.switchToSlot(slot, this.explodeSwitchConfig.getValue());
               BlockHitResult result = new BlockHitResult(pos.toCenterPos(), Managers.INTERACT.getInteractDirection(pos, (Boolean)this.strictDirectionConfig.getValue()), pos, true);
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
               Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               this.switchToSlot(slot1, this.explodeSwitchConfig.getValue());
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
               if ((Boolean)this.swingConfig.getValue()) {
                  mc.player.swingHand(Hand.MAIN_HAND);
               } else {
                  Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               }

               Managers.INVENTORY.syncToClient();
            } else {
               this.switchToSlot(slot1, this.explodeSwitchConfig.getValue());
               BlockHitResult result1 = new BlockHitResult(pos.toCenterPos(), Managers.INTERACT.getInteractDirection(pos, (Boolean)this.strictDirectionConfig.getValue()), pos, true);
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result1);
               if ((Boolean)this.swingConfig.getValue()) {
                  mc.player.swingHand(Hand.MAIN_HAND);
               } else {
                  Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
               }

               Managers.INVENTORY.syncToClient();
            }

         }
      }
   }

   private void switchToSlot(int slot, SwitchMode mode) {
      if (mode == AutoAnchorModule.SwitchMode.Inventory) {
         int syncId = mc.player.playerScreenHandler.syncId;
         int curHotbar = mc.player.getInventory().selectedSlot;
         if (slot != curHotbar) {
            int handlerSource = slot + 36;
            mc.interactionManager.clickSlot(syncId, handlerSource, curHotbar, SlotActionType.SWAP, mc.player);
         }
      } else {
         Managers.INVENTORY.setSlot(slot);
      }
   }

   private void handleBreakAnchors() {
      if (this.breakTimer.passed(200.0F)) {
         BlockPos origin = mc.player.getBlockPos();
         double rad = Math.ceil((double)(Float)this.breakRangeConfig.getValue());

         for(double x = -rad; x <= rad; ++x) {
            for(double y = -rad; y <= rad; ++y) {
               for(double z = -rad; z <= rad; ++z) {
                  BlockPos pos = new BlockPos((int)((double)origin.getX() + x), (int)((double)origin.getY() + y), (int)((double)origin.getZ() + z));
                  if (!(mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > ((NumberConfig)this.breakRangeConfig).getValueSq())) {
                     BlockState state = mc.world.getBlockState(pos);
                     if (state.getBlock() instanceof RespawnAnchorBlock && (Integer)state.get(RespawnAnchorBlock.CHARGES) <= 0) {
                        BlockHitResult result = new BlockHitResult(pos.toCenterPos(), Managers.INTERACT.getInteractDirection(pos, (Boolean)this.strictDirectionConfig.getValue()), pos, true);
                        mc.interactionManager.attackBlock(pos, result.getSide());
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.breakTimer.reset();
                        return;
                     }
                  }
               }
            }
         }

      }
   }

   private AnchorCalc calculateAnchorExplosion() {
      BlockPos data = null;
      double bestAnchorDamage = (double)0.0F;
      double bestSelfDamage = (double)0.0F;
      boolean isAnchor = false;
      Iterator var7 = this.getSphere(mc.player.getPos()).iterator();

      while(true) {
         BlockPos pos;
         boolean explosion;
         while(true) {
            if (!var7.hasNext()) {
               if (data != null && bestAnchorDamage >= (double)(Float)this.minDamageConfig.getValue()) {
                  return new AnchorCalc(data, isAnchor, bestAnchorDamage, bestSelfDamage);
               }

               return null;
            }

            pos = (BlockPos)var7.next();
            BlockState state = mc.world.getBlockState(pos);
            double dist1 = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
            if (!(dist1 > ((NumberConfig)this.rangeConfig).getValueSq())) {
               explosion = state.getBlock() instanceof RespawnAnchorBlock;
               if (state.isReplaceable() || explosion) {
                  if (!explosion && (Boolean)this.placeConfig.getValue()) {
                     if (!(dist1 > ((NumberConfig)this.placeRangeConfig).getValueSq()) && (!(Boolean)this.detectMiningConfig.getValue() || !Managers.BREAK.isMining(pos))) {
                        break;
                     }
                  } else if (explosion) {
                     break;
                  }
               }
            }
         }

         double selfDamage = ExplosionUtil.getDamageTo(mc.player, pos.toCenterPos(), (Boolean)this.blockDestructionConfig.getValue(), 10.0F, Set.of(pos), (Boolean)this.selfExtrapolateConfig.getValue() ? (Integer)this.extrapolateTicksConfig.getValue() : 0, false);
         boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
         if (!unsafeToPlayer && (AirPlaceModule.getInstance().isEnabled() || (Boolean)this.airPlaceConfig.getValue() || Managers.INTERACT.getInteractDirectionInternal(pos, false) != null) && Managers.INTERACT.canPlace(pos, Blocks.RESPAWN_ANCHOR)) {
            for(Entity entity : mc.world.getEntities()) {
               if (!entity.getBoundingBox().intersects(new Box(pos)) && entity != null && entity.isAlive() && entity != mc.player && this.isValidTarget(entity) && !Managers.SOCIAL.isFriend(entity.getName())) {
                  double blockDist = pos.getSquaredDistance(entity.getPos());
                  double dist = mc.player.squaredDistanceTo(entity);
                  if (!(blockDist > (double)144.0F) && !(dist > (double)((Float)this.targetRangeConfig.getValue() * (Float)this.targetRangeConfig.getValue())) && ExplosionUtil.getDamageTo(entity, pos.toCenterPos(), (Boolean)this.blockDestructionConfig.getValue(), 10.0F, Set.of(pos), (Integer)this.extrapolateTicksConfig.getValue(), (Boolean)this.assumeArmorConfig.getValue()) > bestAnchorDamage) {
                     double damage = ExplosionUtil.getDamageTo(entity, pos.toCenterPos(), (Boolean)this.blockDestructionConfig.getValue(), 10.0F, Set.of(pos), (Integer)this.extrapolateTicksConfig.getValue(), (Boolean)this.assumeArmorConfig.getValue());
                     if (!(damage < selfDamage * (double)(Float)this.damageRatioConfig.getValue()) || !(selfDamage > (double)0.5F)) {
                        data = pos;
                        bestAnchorDamage = damage;
                        bestSelfDamage = selfDamage;
                        isAnchor = explosion;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean playerDamageCheck(double playerDamage) {
      if (!mc.player.isCreative()) {
         float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
         if ((Boolean)this.safetyConfig.getValue() && playerDamage >= (double)(health + 0.5F)) {
            return true;
         } else {
            return playerDamage > (double)(Float)this.maxLocalDamageConfig.getValue();
         }
      } else {
         return false;
      }
   }

   private List<BlockPos> getSphere(Vec3d origin) {
      List<BlockPos> sphere = new ArrayList<>();
      double rad = Math.ceil((double)Math.max((Float)this.rangeConfig.getValue(), (Float)this.placeRangeConfig.getValue()));

      for(double x = -rad; x <= rad; ++x) {
         for(double y = -rad; y <= rad; ++y) {
            for(double z = -rad; z <= rad; ++z) {
               Vec3i pos = new Vec3i((int)(origin.getX() + x), (int)(origin.getY() + y), (int)(origin.getZ() + z));
               sphere.add(new BlockPos(pos));
            }
         }
      }

      return sphere;
   }

   private boolean isValidTarget(Entity e) {
      return e instanceof PlayerEntity && (Boolean)this.playersConfig.getValue() || EntityUtil.isMonster(e) && (Boolean)this.monstersConfig.getValue() || EntityUtil.isNeutral(e) && (Boolean)this.neutralsConfig.getValue() || EntityUtil.isPassive(e) && (Boolean)this.animalsConfig.getValue();
   }

   private int findNonBlockSlot() {
      int slot = -1;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!(stack.getItem() instanceof BlockItem)) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   public static enum SwitchMode {
      Hotbar,
      Inventory;

      // $FF: synthetic method
      private static SwitchMode[] $values() {
         return new SwitchMode[]{Hotbar, Inventory};
      }
   }

   public static enum BoxMode {
      Both,
      Box,
      Fill,
      None;

      // $FF: synthetic method
      private static BoxMode[] $values() {
         return new BoxMode[]{Both, Box, Fill, None};
      }
   }

   private static record AnchorCalc(BlockPos pos, boolean isAnchor, double targetDamage, double selfDamage) {
   }
}
