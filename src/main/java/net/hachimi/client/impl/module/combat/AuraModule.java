package net.hachimi.client.impl.module.combat;

import java.util.Iterator;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.EnchantmentUtil;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.MaceItem;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.apache.commons.lang3.mutable.MutableDouble;

public class AuraModule extends CombatModule {
   private static AuraModule INSTANCE;
   Config<Weapon> weaponConfig;
   Config<Boolean> rotateConfig;
   Config<SwingMode> swingConfig;
   Config<AutoSwitch> autoSwitchConfig;
   Config<SwapAction> swapActionConfig;
   Config<ShieldMode> shieldModeConfig;
   Config<SortPriority> sortPriorityConfig;
   Config<TargetMode> modeConfig;
   Config<Integer> maxTargetConfig;
   Config<Float> rangeConfig;
   Config<Float> wallRangeConfig;
   Config<MobAgeFilter> mobAgeFilterConfig;
   Config<Boolean> ignoreNamedConfig;
   Config<Boolean> ignorePassiveConfig;
   Config<Boolean> ignoreTamedConfig;
   Config<Boolean> reverseConfig;
   Config<Boolean> customDelayConfig;
   Config<Float> hitDelayConfig;
   Config<Boolean> attackCdConfig;
   Config<Float> switchDelayConfig;
   Config<Boolean> playersConfig;
   Config<Boolean> mobsConfig;
   Config<Boolean> animalsConfig;
   Config<Boolean> villagersConfig;
   Config<Boolean> slimesConfig;
   private Entity entityTarget;
   private long lastAttackTime;
   private final Timer autoSwapTimer;
   private final Timer switchTimer;

   public AuraModule() {
      super("Aura", "Attacks nearby entities", ModuleCategory.COMBAT, 700);
      this.weaponConfig = this.register(new EnumConfig("Weapon", "Weapon type to use for attacking", AuraModule.Weapon.ALL, AuraModule.Weapon.values()));
      this.rotateConfig = this.register(new BooleanConfig("Rotate", "Silently rotate to target before attacking", false));
      this.swingConfig = this.register(new EnumConfig("Swing", "Hand swing mode when attacking", AuraModule.SwingMode.NORMAL, AuraModule.SwingMode.values()));
      this.autoSwitchConfig = this.register(new EnumConfig("AutoSwitch", "Automatically switch to best weapon before attacking", AuraModule.AutoSwitch.NONE, AuraModule.AutoSwitch.values()));
      this.swapActionConfig = this.register(new EnumConfig("SwapAction", "Swap method when switching weapon", AuraModule.SwapAction.HOTBAR, AuraModule.SwapAction.values()));
      this.shieldModeConfig = this.register(new EnumConfig("ShieldMode", "How to handle the targets shield", AuraModule.ShieldMode.IGNORE, AuraModule.ShieldMode.values()));
      this.sortPriorityConfig = this.register(new EnumConfig("SortPriority", "Priority when selecting target", AuraModule.SortPriority.LOWEST_HEALTH, AuraModule.SortPriority.values()));
      this.modeConfig = this.register(new EnumConfig("Mode", "Target lock mode", AuraModule.TargetMode.SWITCH, AuraModule.TargetMode.values()));
      this.maxTargetConfig = this.register(new NumberConfig("MaxTarget", "Maximum targets to attack per tick", 1, 1, 5));
      this.rangeConfig = this.register(new NumberConfig("Range", "Range to attack entities", 1.0F, 4.5F, 6.0F));
      this.wallRangeConfig = this.register(new NumberConfig("WallsRange", "Range to attack through walls", 1.0F, 4.5F, 6.0F));
      this.mobAgeFilterConfig = this.register(new EnumConfig("MobAgeFilter", "Filter mobs by age", AuraModule.MobAgeFilter.BOTH, AuraModule.MobAgeFilter.values()));
      this.ignoreNamedConfig = this.register(new BooleanConfig("IgnoreNamed", "Ignore named entities", false));
      this.ignorePassiveConfig = this.register(new BooleanConfig("IgnorePassive", "Ignore passive entities", false));
      this.ignoreTamedConfig = this.register(new BooleanConfig("IgnoreTamed", "Ignore tamed entities", false));
      this.reverseConfig = this.register(new BooleanConfig("Reverse", "Reverse target priority", false));
      this.customDelayConfig = this.register(new BooleanConfig("CustomDelay", "Use custom hit delay instead of attack cooldown", false));
      this.hitDelayConfig = this.register(new NumberConfig("HitDelay", "Custom delay between hits (ms)", 0.0F, 50.0F, 500.0F, () -> this.customDelayConfig.getValue()));
      this.attackCdConfig = this.register(new BooleanConfig("AttackCD", "Wait for full attack cooldown before hitting", true, () -> !(Boolean)this.customDelayConfig.getValue()));
      this.switchDelayConfig = this.register(new NumberConfig("SwitchDelay", "Delay after swapping items before attacking (ms)", 0.0F, 0.0F, 500.0F));
      this.playersConfig = this.register(new BooleanConfig("Players", "Target players", true));
      this.mobsConfig = this.register(new BooleanConfig("Mobs", "Target hostile mobs", false));
      this.animalsConfig = this.register(new BooleanConfig("Animals", "Target passive animals", false));
      this.villagersConfig = this.register(new BooleanConfig("Villagers", "Target villagers", false));
      this.slimesConfig = this.register(new BooleanConfig("Slimes", "Target slimes and magma cubes", false));
      this.autoSwapTimer = new CacheTimer();
      this.switchTimer = new CacheTimer();
      INSTANCE = this;
   }

   public static AuraModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   public void onDisable() {
      this.entityTarget = null;
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            this.switchTimer.reset();
         }

      }
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!AutoCrystalModule.getInstance().isAttacking() && !AutoCrystalModule.getInstance().isPlacing() && !mc.player.isSpectator()) {
            if ((Boolean)this.multitaskConfig.getValue() || !this.checkMultitask(true)) {
               Vec3d eyepos = Managers.POSITION.getEyePos();
               Entity var10001;
               switch (((TargetMode)this.modeConfig.getValue()).ordinal()) {
                  case 0 -> var10001 = this.entityTarget != null && this.entityTarget.isAlive() && this.isInAttackRange(eyepos, this.entityTarget) ? this.entityTarget : this.getAttackTarget(eyepos);
                  case 1 -> var10001 = this.getAttackTarget(eyepos);
                  default -> throw new MatchException((String)null, (Throwable)null);
               }

               this.entityTarget = var10001;
               if (this.entityTarget != null && this.switchTimer.passed((Float)this.switchDelayConfig.getValue() * 25.0F)) {
                  if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND || mc.options.attackKey.isPressed() || PlayerUtil.isHotbarKeysPressed()) {
                     this.autoSwapTimer.reset();
                  }

                  int slot = this.getWeaponSlot();
                  boolean silentSwapped = false;
                  if (!this.isHoldingWeapon() && slot != -1) {
                     label159:
                     switch (((AutoSwitch)this.autoSwitchConfig.getValue()).ordinal()) {
                        case 0:
                        default:
                           break;
                        case 1:
                           if (this.autoSwapTimer.passed(500)) {
                              Managers.INVENTORY.setClientSlot(slot);
                           }
                           break;
                        case 2:
                           switch (((SwapAction)this.swapActionConfig.getValue()).ordinal()) {
                              case 0:
                                 if (slot < 9) {
                                    Managers.INVENTORY.setSlot(slot);
                                    silentSwapped = true;
                                 }
                                 break label159;
                              case 1:
                                 if (slot < 9) {
                                    Managers.INVENTORY.setSlot(slot);
                                 } else {
                                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                                 }

                                 silentSwapped = true;
                              case 2:
                                 break label159;
                              default:
                                 Managers.INVENTORY.setSlot(slot);
                                 silentSwapped = true;
                                 break label159;
                           }
                        case 3:
                           if (this.swapActionConfig.getValue() == AuraModule.SwapAction.HOTBAR && slot < 9) {
                              Managers.INVENTORY.setSlot(slot);
                              silentSwapped = true;
                           } else if (this.swapActionConfig.getValue() == AuraModule.SwapAction.SCREEN) {
                              if (slot < 9) {
                                 Managers.INVENTORY.setSlot(slot);
                              } else {
                                 mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                              }

                              silentSwapped = true;
                           }
                     }
                  }

                  if (this.isHoldingWeapon() || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT_ALT) {
                     if ((Boolean)this.rotateConfig.getValue()) {
                        float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), this.entityTarget.getEyePos());
                        this.setRotationSilent(rot[0], rot[1]);
                     }

                     if (!this.isInAttackRange(eyepos, this.entityTarget)) {
                        Managers.INVENTORY.syncToClient();
                     } else {
                        if (this.shieldModeConfig.getValue() == AuraModule.ShieldMode.BREAK) {
                           Entity var6 = this.entityTarget;
                           if (var6 instanceof LivingEntity) {
                              LivingEntity living = (LivingEntity)var6;
                              if (living.isBlocking()) {
                                 int axeSlot = this.getAxeSlot();
                                 if (axeSlot != -1) {
                                    Managers.INVENTORY.setSlot(axeSlot);
                                    Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(this.entityTarget, mc.player.isSneaking()));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                    Managers.INVENTORY.syncToClient();
                                    if ((this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT_ALT) && silentSwapped) {
                                       Managers.INVENTORY.syncToClient();
                                    }

                                    return;
                                 }
                              }
                           }
                        }

                        if (this.shieldModeConfig.getValue() == AuraModule.ShieldMode.WAIT) {
                           Entity var16 = this.entityTarget;
                           if (var16 instanceof LivingEntity) {
                              LivingEntity living = (LivingEntity)var16;
                              if (living.isBlocking()) {
                                 if ((this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT_ALT) && silentSwapped) {
                                    Managers.INVENTORY.syncToClient();
                                 }

                                 return;
                              }
                           }
                        }

                        boolean didAttack = false;
                        if ((Boolean)this.customDelayConfig.getValue()) {
                           if ((float)(System.currentTimeMillis() - this.lastAttackTime) >= (Float)this.hitDelayConfig.getValue()) {
                              didAttack = this.attackTarget(this.entityTarget);
                           }
                        } else if ((Boolean)this.attackCdConfig.getValue()) {
                           int useSlot = slot == -1 ? mc.player.getInventory().selectedSlot : slot;
                           ItemStack stack = mc.player.getInventory().getStack(useSlot);
                           MutableDouble attackSpeed = new MutableDouble(mc.player.getAttributeBaseValue(EntityAttributes.GENERIC_ATTACK_SPEED));
                           AttributeModifiersComponent attrMod = (AttributeModifiersComponent)stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                           if (attrMod != null) {
                              attrMod.applyModifiers(EquipmentSlot.MAINHAND, (entry, modifier) -> {
                                 if (entry == EntityAttributes.GENERIC_ATTACK_SPEED) {
                                    attackSpeed.add(modifier.value());
                                 }

                              });
                           }

                           double cooldownTicks = (double)1.0F / attackSpeed.getValue() * (double)20.0F;
                           if ((double)((float)(System.currentTimeMillis() - this.lastAttackTime) / 50.0F) >= cooldownTicks) {
                              didAttack = this.attackTarget(this.entityTarget);
                           }
                        } else {
                           didAttack = this.attackTarget(this.entityTarget);
                        }

                        if (didAttack) {
                           this.lastAttackTime = System.currentTimeMillis();
                        }

                        if ((this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT || this.autoSwitchConfig.getValue() == AuraModule.AutoSwitch.SILENT_ALT) && silentSwapped) {
                           Managers.INVENTORY.syncToClient();
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private boolean attackTarget(Entity entity) {
      Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
      switch (((SwingMode)this.swingConfig.getValue()).ordinal()) {
         case 0:
         default:
            break;
         case 1:
            mc.player.swingHand(Hand.MAIN_HAND);
            break;
         case 2:
            mc.player.swingHand(Hand.MAIN_HAND);
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }

      return true;
   }

   private int getWeaponSlot() {
      float best = 0.0F;
      int slot = -1;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty()) {
            Item item = stack.getItem();
            if (this.isWeaponAllowed(item)) {
               float dmg = this.getItemDamage(stack, item);
               if (dmg > best) {
                  best = dmg;
                  slot = i;
               }
            }
         }
      }

      return slot;
   }

   private int getAxeSlot() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof AxeItem) {
            return i;
         }
      }

      return -1;
   }

   private float getItemDamage(ItemStack stack, Item item) {
      float sharp = (float)EnchantmentUtil.getLevel(stack, Enchantments.SHARPNESS) * 0.5F + 0.5F;
      if (item instanceof SwordItem s) {
         return s.getMaterial().getAttackDamage() + sharp;
      } else if (item instanceof AxeItem a) {
         return a.getMaterial().getAttackDamage() + sharp;
      } else if (item instanceof TridentItem) {
         return 8.0F + sharp;
      } else {
         return item instanceof MaceItem ? 5.0F + sharp : 0.0F;
      }
   }

   private boolean isWeaponAllowed(Item item) {
      boolean var10000;
      switch (((Weapon)this.weaponConfig.getValue()).ordinal()) {
         case 0 -> var10000 = item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
         case 1 -> var10000 = item instanceof SwordItem;
         case 2 -> var10000 = item instanceof AxeItem;
         case 3 -> var10000 = item instanceof MaceItem;
         case 4 -> var10000 = item instanceof TridentItem;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public boolean isHoldingWeapon() {
      return this.isWeaponAllowed(mc.player.getMainHandStack().getItem());
   }

   public boolean isInAttackRange(Vec3d pos, Entity entity) {
      if (entity != null && entity.getPos() != null) {
         Vec3d entityPos = entity.getEyePos();
         double dist = pos.distanceTo(entityPos);
         if (dist > (double)(Float)this.rangeConfig.getValue()) {
            return false;
         } else {
            BlockHitResult result = mc.world.raycast(new net.minecraft.world.RaycastContext(pos, entityPos, net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
            return result == null || result.getBlockPos().equals(BlockPos.ofFloored(entityPos)) || !(dist > (double)(Float)this.wallRangeConfig.getValue());
         }
      } else {
         return false;
      }
   }

   private Entity getAttackTarget(Vec3d pos) {
      double best = (Boolean)this.reverseConfig.getValue() ? -Double.MAX_VALUE : Double.MAX_VALUE;
      Entity found = null;
      Iterator var5 = mc.world.getEntities().iterator();

      while(true) {
         Entity entity;
         boolean enemy;
         while(true) {
            if (!var5.hasNext()) {
               return found;
            }

            entity = (Entity)var5.next();
            if (entity != null && entity != mc.player && entity.isAlive() && entity.getPos() != null && !(entity instanceof EndCrystalEntity) && !(entity instanceof ItemEntity) && !(entity instanceof ArrowEntity) && !(entity instanceof ExperienceBottleEntity)) {
               try {
                  enemy = this.isEnemy(entity);
                  break;
               } catch (Exception var15) {
               }
            }
         }

         if (enemy && !Managers.SOCIAL.isFriend(entity.getName()) && (!(Boolean)this.ignoreNamedConfig.getValue() || !entity.hasCustomName())) {
            if ((Boolean)this.ignoreTamedConfig.getValue() && entity instanceof TameableEntity) {
               TameableEntity t = (TameableEntity)entity;
               if (t.isTamed()) {
                  continue;
               }
            }

            if ((Boolean)this.ignorePassiveConfig.getValue()) {
               boolean passive;
               try {
                  passive = EntityUtil.isPassive(entity);
               } catch (Exception var14) {
                  passive = false;
               }

               if (passive) {
                  continue;
               }
            }

            if (entity instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)entity;
               if (this.mobAgeFilterConfig.getValue() == AuraModule.MobAgeFilter.BABY && !living.isBaby() || this.mobAgeFilterConfig.getValue() == AuraModule.MobAgeFilter.ADULT && living.isBaby()) {
                  continue;
               }
            }

            double dist = pos.distanceTo(entity.getPos());
            if (!(dist > (double)(Float)this.rangeConfig.getValue())) {
               double val;
               try {
                  double var10000;
                  switch (((SortPriority)this.sortPriorityConfig.getValue()).ordinal()) {
                     case 0:
                        float[] rots = RotationUtil.getRotationsTo(pos, entity.getPos());
                        var10000 = (double)Math.abs(MathHelper.wrapDegrees(mc.player.getYaw()) - rots[0]);
                        break;
                     case 1:
                        var10000 = dist;
                        break;
                     case 2:
                        if (entity instanceof LivingEntity) {
                           LivingEntity le = (LivingEntity)entity;
                           var10000 = (double)(le.getHealth() + le.getAbsorptionAmount());
                        } else {
                           var10000 = dist;
                        }
                        break;
                     default:
                        throw new MatchException((String)null, (Throwable)null);
                  }

                  val = var10000;
               } catch (Exception var13) {
                  val = dist;
               }

               boolean better = (Boolean)this.reverseConfig.getValue() ? val > best : val < best;
               if (better) {
                  best = val;
                  found = entity;
               }
            }
         }
      }
   }

   private boolean isEnemy(Entity e) {
      if (e instanceof PlayerEntity) {
         return (Boolean)this.playersConfig.getValue();
      } else if (e instanceof SlimeEntity) {
         return (Boolean)this.slimesConfig.getValue();
      } else if (e instanceof VillagerEntity) {
         return (Boolean)this.villagersConfig.getValue();
      } else if (e instanceof AnimalEntity) {
         return (Boolean)this.animalsConfig.getValue();
      } else {
         return EntityUtil.isMonster(e) ? (Boolean)this.mobsConfig.getValue() : false;
      }
   }

   public Entity getEntityTarget() {
      return this.entityTarget;
   }

   public static enum Weapon {
      ALL,
      SWORD,
      AXE,
      MACE,
      TRIDENT;

      // $FF: synthetic method
      private static Weapon[] $values() {
         return new Weapon[]{ALL, SWORD, AXE, MACE, TRIDENT};
      }
   }

   public static enum SwingMode {
      NONE,
      NORMAL,
      ALL;

      // $FF: synthetic method
      private static SwingMode[] $values() {
         return new SwingMode[]{NONE, NORMAL, ALL};
      }
   }

   public static enum AutoSwitch {
      NONE,
      NORMAL,
      SILENT,
      SILENT_ALT;

      // $FF: synthetic method
      private static AutoSwitch[] $values() {
         return new AutoSwitch[]{NONE, NORMAL, SILENT, SILENT_ALT};
      }
   }

   public static enum SwapAction {
      HOTBAR,
      SCREEN,
      NONE;

      // $FF: synthetic method
      private static SwapAction[] $values() {
         return new SwapAction[]{HOTBAR, SCREEN, NONE};
      }
   }

   public static enum ShieldMode {
      IGNORE,
      BREAK,
      WAIT;

      // $FF: synthetic method
      private static ShieldMode[] $values() {
         return new ShieldMode[]{IGNORE, BREAK, WAIT};
      }
   }

   public static enum SortPriority {
      CLOSE_ANGLE,
      LOWEST_DISTANCE,
      LOWEST_HEALTH;

      // $FF: synthetic method
      private static SortPriority[] $values() {
         return new SortPriority[]{CLOSE_ANGLE, LOWEST_DISTANCE, LOWEST_HEALTH};
      }
   }

   public static enum TargetMode {
      SINGLE,
      SWITCH;

      // $FF: synthetic method
      private static TargetMode[] $values() {
         return new TargetMode[]{SINGLE, SWITCH};
      }
   }

   public static enum MobAgeFilter {
      BOTH,
      BABY,
      ADULT;

      // $FF: synthetic method
      private static MobAgeFilter[] $values() {
         return new MobAgeFilter[]{BOTH, BABY, ADULT};
      }
   }
}
