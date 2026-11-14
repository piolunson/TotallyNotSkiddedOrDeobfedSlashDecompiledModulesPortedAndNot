package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.Comparator;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class SpearKillModule extends CombatModule {
   Config<Float> rangeConfig = this.register(new NumberConfig("Range", "Range to search for players", 1.0F, 12.0F, 100.0F));
   Config<Float> wallRangeConfig = this.register(new NumberConfig("WallRange", "Range to search through walls", 0.0F, 4.0F, 100.0F));
   Config<Float> hitRangeConfig = this.register(new NumberConfig("HitRange", "Range to send spear attacks", 1.0F, 4.2F, 100.0F));
   Config<Integer> delayConfig = this.register(new NumberConfig("Delay", "Minimum delay in milliseconds between attacks", 0, 100, 1000));
   Config<Boolean> attackDelayConfig = this.register(new BooleanConfig("AttackDelay", "Uses vanilla attack cooldown before attacking", true));
   Config<Float> attackSpeedConfig = this.register(new NumberConfig("AttackSpeed", "Attacks per second when AttackDelay is off", 1.0F, 8.0F, 20.0F, () -> !(Boolean)this.attackDelayConfig.getValue()));
   Config<Swap> autoSwapConfig;
   Config<Boolean> rotateConfig;
   Config<Boolean> lungeConfig;
   Config<Float> lungeSpeedConfig;
   Config<Float> lungeYConfig;
   Config<Float> stopDistanceConfig;
   Config<Integer> lungeDelayConfig;
   Config<Boolean> swingConfig;
   Config<Boolean> renderConfig;
   Config<Boolean> renderLineConfig;
   Config<Color> fillColorConfig;
   Config<Color> lineColorConfig;
   private final Timer attackTimer;
   private final Timer lungeTimer;
   private PlayerEntity target;

   public SpearKillModule() {
      super("SpearKill", "Lunges at players and attacks with a spear", ModuleCategory.COMBAT, 660);
      this.autoSwapConfig = this.register(new EnumConfig("AutoSwap", "Swaps to a spear before attacking", SpearKillModule.Swap.NORMAL, SpearKillModule.Swap.values()));
      this.rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to the target before lunging and attacking", true));
      this.lungeConfig = this.register(new BooleanConfig("Lunge", "Pushes you toward the target", true));
      this.lungeSpeedConfig = this.register(new NumberConfig("LungeSpeed", "Horizontal lunge speed", 0.1F, 1.25F, 5.0F, () -> this.lungeConfig.getValue()));
      this.lungeYConfig = this.register(new NumberConfig("LungeY", "Upward velocity added while lunging", 0.0F, 0.08F, 1.0F, () -> this.lungeConfig.getValue()));
      this.stopDistanceConfig = this.register(new NumberConfig("StopDistance", "Stops lunging inside this distance", 0.0F, 2.4F, 6.0F, () -> this.lungeConfig.getValue()));
      this.lungeDelayConfig = this.register(new NumberConfig("LungeDelay", "Delay in milliseconds between lunges", 0, 150, 1000, () -> this.lungeConfig.getValue()));
      this.swingConfig = this.register(new BooleanConfig("Swing", "Swings the hand after attacking", true));
      this.renderConfig = this.register(new BooleanConfig("Render", "Renders the current SpearKill target", true));
      this.renderLineConfig = this.register(new BooleanConfig("RenderLine", "Renders a line to the current target", true, () -> this.renderConfig.getValue()));
      this.fillColorConfig = this.register(new ColorConfig("FillColor", "Fill color for the target render", new Color(255, 210, 90, 45), true, false, () -> this.renderConfig.getValue()));
      this.lineColorConfig = this.register(new ColorConfig("LineColor", "Outline and line color for the target render", new Color(255, 210, 90, 180), true, false, () -> this.renderConfig.getValue()));
      this.attackTimer = new CacheTimer();
      this.lungeTimer = new CacheTimer();
   }

   public String getModuleData() {
      return this.target == null ? "Spear" : this.target.getName().getString();
   }

   public void onDisable() {
      this.target = null;
      Managers.INVENTORY.syncToClient();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null && !mc.player.isSpectator()) {
         if ((Boolean)this.multitaskConfig.getValue() || !this.checkMultitask(true)) {
            this.target = this.findTarget();
            if (this.target != null) {
               int spearSlot = this.getSpearSlot();
               if (spearSlot != -1) {
                  boolean silentSwap = false;
                  if (!this.isHoldingSpear()) {
                     switch (((Swap)this.autoSwapConfig.getValue()).ordinal()) {
                        case 0:
                           return;
                        case 1:
                           Managers.INVENTORY.setClientSlot(spearSlot);
                           break;
                        case 2:
                           Managers.INVENTORY.setSlot(spearSlot);
                           silentSwap = true;
                     }
                  }

                  if ((Boolean)this.rotateConfig.getValue() && !this.isRotationBlocked()) {
                     Vec3d aim = this.target.getBoundingBox().getCenter();
                     float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), aim);
                     this.setRotation(rot[0], rot[1]);
                  }

                  double dist = (double)mc.player.distanceTo(this.target);
                  if ((Boolean)this.lungeConfig.getValue() && dist > (double)(Float)this.stopDistanceConfig.getValue() && this.lungeTimer.passed((Number)this.lungeDelayConfig.getValue())) {
                     this.lunge(this.target);
                     this.lungeTimer.reset();
                  }

                  if (dist <= (double)(Float)this.hitRangeConfig.getValue() && this.canAttack()) {
                     this.attack(this.target);
                     this.attackTimer.reset();
                  }

                  if (silentSwap) {
                     Managers.INVENTORY.syncToClient();
                  }

               }
            }
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent.Game event) {
      if ((Boolean)this.renderConfig.getValue() && this.target != null && this.target.isAlive() && !this.target.isRemoved()) {
         Box box = Interpolation.getInterpolatedEntityBox(this.target);
         RenderManager.renderBox(event.getMatrices(), box, ((Color)this.fillColorConfig.getValue()).getRGB());
         RenderManager.renderBoundingBox(event.getMatrices(), box, 1.5F, ((Color)this.lineColorConfig.getValue()).getRGB());
         if ((Boolean)this.renderLineConfig.getValue()) {
            Vec3d start = Interpolation.getInterpolatedPosition(mc.player, event.getTickDelta()).add((double)0.0F, (double)mc.player.getEyeHeight(mc.player.getPose()), (double)0.0F);
            RenderManager.renderLine(event.getMatrices(), start, box.getCenter(), 1.5F, ((Color)this.lineColorConfig.getValue()).getRGB());
         }

      }
   }

   private PlayerEntity findTarget() {
      return (PlayerEntity)mc.world.getPlayers().stream().filter(this::isValidTarget).min(Comparator.comparingDouble((p) -> mc.player.squaredDistanceTo(p))).orElse(null);
   }

   private boolean isValidTarget(PlayerEntity player) {
      if (player != mc.player && player.isAlive() && !player.isRemoved() && !player.isSpectator()) {
         if (Managers.SOCIAL.isFriend(player.getName().getString())) {
            return false;
         } else {
            double dist = (double)mc.player.distanceTo(player);
            if (dist > (double)(Float)this.rangeConfig.getValue()) {
               return false;
            } else {
               return mc.player.canSee(player) || dist <= (double)(Float)this.wallRangeConfig.getValue();
            }
         }
      } else {
         return false;
      }
   }

   private boolean canAttack() {
      if (!this.attackTimer.passed((Number)this.delayConfig.getValue())) {
         return false;
      } else if ((Boolean)this.attackDelayConfig.getValue()) {
         return mc.player.getAttackCooldownProgress(0.5F) >= 1.0F;
      } else {
         return this.attackTimer.passed(1000.0F / Math.max((Float)this.attackSpeedConfig.getValue(), 0.1F));
      }
   }

   private void lunge(PlayerEntity player) {
      Vec3d delta = player.getPos().subtract(mc.player.getPos());
      Vec3d horizontal = new Vec3d(delta.x, (double)0.0F, delta.z);
      if (!(horizontal.length() < 1.0E-4)) {
         Vec3d dir = horizontal.normalize();
         double vy = Math.max(mc.player.getVelocity().y, (double)(Float)this.lungeYConfig.getValue());
         mc.player.setVelocity(dir.x * (double)(Float)this.lungeSpeedConfig.getValue(), vy, dir.z * (double)(Float)this.lungeSpeedConfig.getValue());
      }
   }

   private void attack(PlayerEntity player) {
      Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(player, mc.player.isSneaking()));
      if ((Boolean)this.swingConfig.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      } else {
         Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }

   }

   private boolean isHoldingSpear() {
      return mc.player.getMainHandStack().getItem() instanceof TridentItem;
   }

   private int getSpearSlot() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof TridentItem) {
            return i;
         }
      }

      return -1;
   }

   public static enum Swap {
      OFF,
      NORMAL,
      SILENT;

      // $FF: synthetic method
      private static Swap[] $values() {
         return new Swap[]{OFF, NORMAL, SILENT};
      }
   }
}
