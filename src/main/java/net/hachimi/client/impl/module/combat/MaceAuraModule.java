package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class MaceAuraModule extends CombatModule {
   private static MaceAuraModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig("Range", "Horizontal attack range (blocks)", 1.0F, 4.0F, 8.0F));
   Config<Float> approachRangeConfig = this.register(new NumberConfig("ApproachRange", "Horizontal acquisition radius", 6.0F, 24.0F, 50.0F));
   Config<Float> vertRangeConfig = this.register(new NumberConfig("VerticalRange", "Max Y-delta above target", 5.0F, 20.0F, 30.0F));
   Config<Float> minHeightConfig = this.register(new NumberConfig("MinHeight", "Min Y above target before folding", 1.0F, 2.5F, 15.0F));
   Config<Float> smashThresholdConfig = this.register(new NumberConfig("SmashThreshold", "Spoofed fall distance (server min 1.5)", 1.6F, 1.7F, 6.0F));
   Config<Float> foldDelayConfig = this.register(new NumberConfig("FoldDelay", "Ticks to wait after fold before striking", 1.0F, 2.0F, 8.0F));
   Config<Float> attackDelayConfig = this.register(new NumberConfig("Delay", "ms between attack cycles", 50.0F, 250.0F, 2000.0F));
   Config<Boolean> autoTargetConfig = this.register(new BooleanConfig("AutoTarget", "Auto-pick nearest player in range", true));
   Config<Boolean> silentSwapConfig = this.register(new BooleanConfig("SilentSwap", "Swap to mace silently for the hit, swap back", true));
   Config<Boolean> attributeSwapConfig = this.register(new BooleanConfig("AttributeSwap", "Pre-attack with sword for extended reach before mace smash", false));
   Config<Boolean> xzSpoofConfig = this.register(new BooleanConfig("XZSpoof", "Fake X/Z to the target so reach gate always passes", true));
   private DipState dipState;
   private int stateTicks;
   private PlayerEntity dipTarget;
   private long lastAttackMs;

   public MaceAuraModule() {
      super("MaceAura", "Mace smash via two-tick fold→strike spoof", ModuleCategory.COMBAT);
      this.dipState = MaceAuraModule.DipState.IDLE;
      this.stateTicks = 0;
      this.dipTarget = null;
      this.lastAttackMs = 0L;
      INSTANCE = this;
   }

   public static MaceAuraModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      this.lastAttackMs = System.currentTimeMillis();
      this.resetIdle();
   }

   public void onDisable() {
      this.resetIdle();
   }

   private void resetIdle() {
      this.dipState = MaceAuraModule.DipState.IDLE;
      this.dipTarget = null;
      this.stateTicks = 0;
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         if (mc.player.isFallFlying() || this.dipState != MaceAuraModule.DipState.APPROACH && this.dipState != MaceAuraModule.DipState.FOLD) {
            if (this.dipState == MaceAuraModule.DipState.APPROACH) {
               ++this.stateTicks;
               if (this.dipTarget != null && !this.dipTarget.isRemoved()) {
                  double dy = mc.player.getY() - this.dipTarget.getY();
                  if (!(dy < (double)(Float)this.minHeightConfig.getValue()) && !(dy > (double)(Float)this.vertRangeConfig.getValue()) && this.stateTicks <= 200) {
                     if (hDist(mc.player, this.dipTarget) <= (double)(Float)this.rangeConfig.getValue()) {
                        this.beginFold();
                     }

                  } else {
                     this.resetIdle();
                  }
               } else {
                  this.resetIdle();
               }
            } else if (this.dipState == MaceAuraModule.DipState.FOLD) {
               ++this.stateTicks;
               if (this.dipTarget != null && !this.dipTarget.isRemoved()) {
                  if (this.stateTicks >= ((Float)this.foldDelayConfig.getValue()).intValue()) {
                     this.dipState = MaceAuraModule.DipState.STRIKE;
                     this.stateTicks = 0;
                  }

               } else {
                  this.resetIdle();
               }
            } else if (this.dipState == MaceAuraModule.DipState.STRIKE) {
               if (this.dipTarget != null && !this.dipTarget.isRemoved()) {
                  this.doStrike(this.dipTarget);
                  this.lastAttackMs = System.currentTimeMillis();
               }

               this.resetIdle();
            } else {
               if ((Boolean)this.silentSwapConfig.getValue()) {
                  if (this.findMaceInHotbar() == -1) {
                     return;
                  }
               } else if (mc.player.getMainHandStack().getItem() != Items.MACE) {
                  return;
               }

               long now = System.currentTimeMillis();
               if (now - this.lastAttackMs >= ((Float)this.attackDelayConfig.getValue()).longValue()) {
                  PlayerEntity target = this.findTarget();
                  if (target != null) {
                     this.dipTarget = target;
                     this.stateTicks = 0;
                     if (hDist(mc.player, target) <= (double)(Float)this.rangeConfig.getValue()) {
                        this.beginFold();
                     } else {
                        this.dipState = MaceAuraModule.DipState.APPROACH;
                     }

                  }
               }
            }
         } else {
            this.resetIdle();
         }
      }
   }

   private void beginFold() {
      this.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true));
      this.dipState = MaceAuraModule.DipState.FOLD;
      this.stateTicks = 0;
   }

   private void doStrike(PlayerEntity target) {
      double realX = mc.player.getX();
      double realY = mc.player.getY();
      double realZ = mc.player.getZ();
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();
      double drop = (double)(Float)this.smashThresholdConfig.getValue() + 0.2;
      double fakeY = realY - drop;
      double fakeX = (Boolean)this.xzSpoofConfig.getValue() ? target.getX() : realX;
      double fakeZ = (Boolean)this.xzSpoofConfig.getValue() ? target.getZ() : realZ;
      this.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(fakeX, fakeY, fakeZ, yaw, pitch, false));
      this.doAttack(target);
      this.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
   }

   private void doAttack(PlayerEntity target) {
      if ((Boolean)this.attributeSwapConfig.getValue()) {
         int swordSlot = this.findSwordInHotbar();
         if (swordSlot != -1) {
            int prev = mc.player.getInventory().selectedSlot;
            boolean swap = swordSlot != prev;
            if (swap) {
               Managers.INVENTORY.setSlot(swordSlot);
            }

            mc.player.swingHand(Hand.MAIN_HAND);
            this.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            if (swap) {
               Managers.INVENTORY.setSlot(prev);
            }
         }
      }

      int maceSlot = (Boolean)this.silentSwapConfig.getValue() ? this.findMaceInHotbar() : -1;
      if (!(Boolean)this.silentSwapConfig.getValue() || maceSlot != -1) {
         int prev = mc.player.getInventory().selectedSlot;
         boolean swapped = maceSlot != -1 && maceSlot != prev;
         if (swapped) {
            Managers.INVENTORY.setSlot(maceSlot);
         }

         mc.player.swingHand(Hand.MAIN_HAND);
         this.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
         if (swapped) {
            Managers.INVENTORY.setSlot(prev);
         }

      }
   }

   private PlayerEntity findTarget() {
      double r = (double)(Float)this.approachRangeConfig.getValue();
      double vr = (double)(Float)this.vertRangeConfig.getValue();
      if (!(Boolean)this.autoTargetConfig.getValue()) {
         Entity var15 = mc.targetedEntity;
         if (var15 instanceof PlayerEntity) {
            PlayerEntity p = (PlayerEntity)var15;
            if (p != mc.player) {
               double dy = mc.player.getY() - p.getY();
               if (hDist(mc.player, p) <= r && dy >= (double)0.0F && dy <= vr) {
                  return p;
               }
            }
         }

         return null;
      } else {
         PlayerEntity best = null;
         double bestDist = Double.MAX_VALUE;

         for(PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && p.isAlive() && (Managers.SOCIAL == null || !Managers.SOCIAL.isFriend(p.getName().getString()))) {
               double dy = mc.player.getY() - p.getY();
               if (!(dy < (double)0.0F) && !(dy > vr)) {
                  double hd = hDist(mc.player, p);
                  if (!(hd > r) && hd < bestDist) {
                     bestDist = hd;
                     best = p;
                  }
               }
            }
         }

         return best;
      }
   }

   private int findMaceInHotbar() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
            return i;
         }
      }

      return -1;
   }

   private int findSwordInHotbar() {
      for(int i = 0; i < 9; ++i) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (!s.isEmpty() && s.getItem() != Items.MACE) {
            AttributeModifiersComponent amc = (AttributeModifiersComponent)s.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (amc != null) {
               boolean hasDamage = amc.modifiers().stream().anyMatch((e) -> e.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE));
               if (hasDamage) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   private static double hDist(PlayerEntity a, PlayerEntity b) {
      double dx = a.getX() - b.getX();
      double dz = a.getZ() - b.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(packet);
      }

   }

   private static enum DipState {
      IDLE,
      APPROACH,
      FOLD,
      STRIKE;

      // $FF: synthetic method
      private static DipState[] $values() {
         return new DipState[]{IDLE, APPROACH, FOLD, STRIKE};
      }
   }
}
