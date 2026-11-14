package net.hachimi.client.impl.module.combat;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.RunTickEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.world.AddEntityEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.exploit.FastLatencyModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.collection.EvictingQueue;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.PerSecondCounter;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.client.util.player.PlayerUtil;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.world.BlastResistantBlocks;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.block.ShapeContext;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoCrystalModule extends CombatModule {
   private static AutoCrystalModule INSTANCE;
   Config<Page> pageConfig;
   Config<Boolean> placeConfig;
   Config<Float> placeDelayConfig;
   Config<Float> placeRangeConfig;
   Config<Float> placeWallRangeConfig;
   Config<Swap> autoSwapConfig;
   Config<SwapAction> swapActionConfig;
   Config<Placements> placementsConfig;
   Config<Sequential> sequentialConfig;
   Config<Boolean> forcePlaceEnabledConfig;
   Config<Float> minHealthConfig;
   Config<Float> forceDamageConfig;
   Config<SwingMode> placeSwingConfig;
   Config<Boolean> instantConfig;
   Config<Float> instantDelayConfig;
   Config<Boolean> inhibitConfig;
   Config<Boolean> antiSurroundConfig;
   Config<Boolean> prePlacementConfig;
   Config<Boolean> cevBomberConfig;
   Config<Float> cevDamageConfig;
   Config<Float> miningProgressConfig;
   Config<Boolean> resetCrystalConfig;
   Config<Float> breakDelayFloatConfig;
   Config<Float> attackDelayConfig;
   Config<Integer> attackFactorConfig;
   Config<Float> attackLimitConfig;
   Config<Boolean> breakDelayConfig;
   Config<Float> breakTimeoutConfig;
   Config<Float> minTimeoutConfig;
   Config<Integer> ticksExistedConfig;
   Config<Float> breakRangeConfig;
   Config<Float> breakWallRangeConfig;
   Config<SwingMode> breakSwingConfig;
   Config<Boolean> rotateConfig;
   Config<Rotate> strictRotateConfig;
   Config<Integer> rotateLimitConfig;
   Config<Boolean> whileUsingConfig;
   Config<Boolean> whileMiningConfig;
   Config<Float> swapPenaltyConfig;
   Config<ShieldMode> shieldModeConfig;
   Config<Float> targetRangeConfig;
   Config<Boolean> ignoreNakedsConfig;
   Config<Boolean> playersConfig;
   Config<Boolean> monstersConfig;
   Config<Boolean> neutralsConfig;
   Config<Boolean> animalsConfig;
   Config<Boolean> shulkersConfig;
   Config<Boolean> supportConfig;
   Config<Boolean> airPlaceConfig;
   Config<Float> minDamageConfig;
   Config<Float> maxLocalDamageConfig;
   Config<Float> damageRatioConfig;
   Config<Boolean> instantCalcConfig;
   Config<Float> instantDamageConfig;
   Config<Boolean> instantMaxConfig;
   Config<Boolean> ignoreItemConfig;
   Config<Boolean> assumeArmorConfig;
   Config<Float> lethalMultiplier;
   Config<Boolean> antiTotemConfig;
   Config<Boolean> lethalDamageConfig;
   Config<Boolean> safetyConfig;
   Config<Boolean> safetyOverride;
   Config<Boolean> blockDestructionConfig;
   Config<Boolean> selfExtrapolateConfig;
   Config<Integer> extrapolateTicksConfig;
   Config<Boolean> armorBreakerConfig;
   Config<Float> armorScaleConfig;
   Config<Boolean> asyncConfig;
   Config<Float> maxOverrideConfig;
   Config<RenderMode> renderModeConfig;
   Config<BoxMode> boxModeConfig;
   Config<Color> boxColorConfig;
   Config<Color> fillColorConfig;
   Config<Float> widthConfig;
   Config<Boolean> shrinkConfig;
   Config<Float> sliderSpeedConfig;
   Config<Float> startFadeConfig;
   Config<Float> fadeSpeedConfig;
   Config<Integer> fadeTimeConfig;
   Config<Boolean> renderDamageConfig;
   Config<Boolean> targetDamageConfig;
   Config<Boolean> selfDamageDisplayConfig;
   Config<Boolean> targetNameConfig;
   Config<Boolean> damageDisplayConfig;
   Config<Boolean> breakMsConfig;
   Config<Boolean> suffixConfig;
   Config<Boolean> cpsConfig;
   Config<Boolean> baseConfig;
   Config<Boolean> baseRotateConfig;
   Config<Integer> baseDelayConfig2;
   Config<Float> baseEnemyRangeConfig;
   Config<SwapAction> baseSwapActionConfig;
   Config<Float> baseShiftDelayConfig;
   Config<Float> baseMinDamageConfig;
   Config<Boolean> baseAssumeArmorConfig;
   Config<Boolean> baseRenderConfig;
   Config<Integer> baseFadeTimeConfig;
   Config<Boolean> debugConfig;
   Config<Boolean> debugDamageConfig;
   private DamageData<EndCrystalEntity> attackCrystal;
   private DamageData<BlockPos> placeCrystal;
   private BlockPos renderPos;
   private double renderDamage;
   private BlockPos renderSpawnPos;
   private Vec3d crystalRotation;
   private boolean attackRotate;
   private boolean rotated;
   private float[] silentRotations;
   private float calculatePlaceCrystalTime;
   private static final Box FULL_CRYSTAL_BB = new Box((double)0.0F, (double)0.0F, (double)0.0F, (double)1.0F, (double)2.0F, (double)1.0F);
   private static final Box HALF_CRYSTAL_BB = new Box((double)0.0F, (double)0.0F, (double)0.0F, (double)1.0F, (double)1.0F, (double)1.0F);
   private final CacheTimer lastAttackTimer;
   private final Timer lastPlaceTimer;
   private final Timer lastBaseTimer;
   private final Timer lastSwapTimer;
   private final Timer autoSwapTimer;
   private final Deque<Long> attackLatency;
   private final Map<Integer, Long> attackPackets;
   private final Map<BlockPos, Long> placePackets;
   private final PerSecondCounter crystalCounter;
   private final Map<BlockPos, Animation> fadeList;
   private long predictId;
   private final Map<Integer, Integer> antiStuckCrystals;
   private final List<AntiStuckData> stuckCrystals;
   private final ExecutorService executor;
   private final Map<Integer, int[][]> sphereOffsetCache;

   public AutoCrystalModule() {
      super("AutoCrystal", "Attacks entities with end crystals", ModuleCategory.COMBAT, 750);
      this.pageConfig = this.register(new EnumConfig("Page", "Settings page", AutoCrystalModule.Page.PLACE, AutoCrystalModule.Page.values()));
      this.placeConfig = this.register(new BooleanConfig("Place", "Places crystals to damage enemies", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE));
      this.placeDelayConfig = this.register(new NumberConfig("PlaceDelay", "Delay between placements in ms", 0.0F, 0.0F, 500.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.placeRangeConfig = this.register(new NumberConfig("PlaceRange", "Range to place crystals", 0.1F, 6.0F, 6.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.placeWallRangeConfig = this.register(new NumberConfig("PlaceWallRange", "Range to place crystals through walls", 0.1F, 6.0F, 6.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.autoSwapConfig = this.register(new EnumConfig("Swap", "Swaps to end crystal before placing", AutoCrystalModule.Swap.SILENT, AutoCrystalModule.Swap.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.swapActionConfig = this.register(new EnumConfig("SwapAction", "How to swap to crystal slot", AutoCrystalModule.SwapAction.Hotbar, AutoCrystalModule.SwapAction.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue() && this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.OFF));
      this.placementsConfig = this.register(new EnumConfig("Placements", "Version standard for placing end crystals", AutoCrystalModule.Placements.NATIVE, AutoCrystalModule.Placements.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.sequentialConfig = this.register(new EnumConfig("Sequential", "Places a crystal after spawn", AutoCrystalModule.Sequential.NORMAL, AutoCrystalModule.Sequential.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.forcePlaceEnabledConfig = this.register(new BooleanConfig("ForcePlace", "Force place even if no damage", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.minHealthConfig = this.register(new NumberConfig("MinHealth", "Minimum enemy health to start placing", 0.0F, 5.0F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.forceDamageConfig = this.register(new NumberConfig("ForceDamage", "Damage threshold to force place ignoring ratio", 0.0F, 1.5F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.placeSwingConfig = this.register(new EnumConfig("PlaceSwing", "Swing mode when placing crystals", AutoCrystalModule.SwingMode.Normal, AutoCrystalModule.SwingMode.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.instantConfig = this.register(new BooleanConfig("Instant", "Instantly attacks crystals when they spawn", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE));
      this.instantDelayConfig = this.register(new NumberConfig("InstantDelay", "Delay in ms before instant attack", 0.0F, 50.0F, 500.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.instantConfig.getValue()));
      this.inhibitConfig = this.register(new BooleanConfig("Inhibit", "Prevents excessive attacks", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE));
      this.antiSurroundConfig = this.register(new BooleanConfig("AntiSurround", "Places on mining blocks to damage enemies", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.prePlacementConfig = this.register(new BooleanConfig("PrePlacement", "Pre-places crystal on predicted positions", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.cevBomberConfig = this.register(new BooleanConfig("CevBomber", "CEV bomber mode", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue()));
      this.cevDamageConfig = this.register(new NumberConfig("CevDamage", "Minimum damage for CEV bomber", 1.0F, 8.0F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.placeConfig.getValue() && (Boolean)this.cevBomberConfig.getValue()));
      this.miningProgressConfig = this.register(new NumberConfig("MiningProgress", "Progress % before placing on mining block", 0.0F, 90.0F, 100.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE && (Boolean)this.antiSurroundConfig.getValue()));
      this.resetCrystalConfig = this.register(new BooleanConfig("ResetCrystal", "Reset crystal position tracking", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.PLACE));
      this.breakDelayFloatConfig = this.register(new NumberConfig("BreakDelay", "Delay between break attempts in ms", 0.0F, 0.0F, 500.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK));
      this.attackDelayConfig = this.register(new NumberConfig("AttackDelay", "Added attack delay", 0.0F, 0.0F, 5.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK));
      this.attackFactorConfig = this.register(new NumberConfig("AttackFactor", "Factor of attack delay", 0, 0, 3, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK && (double)(Float)this.attackDelayConfig.getValue() > (double)0.0F));
      this.attackLimitConfig = this.register(new NumberConfig("AttackLimit", "Max attacks before crystal considered unbreakable", 0.5F, 3.5F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK));
      this.breakDelayConfig = this.register(new BooleanConfig("Timeout", "Uses attack latency to calculate break delays", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK));
      this.breakTimeoutConfig = this.register(new NumberConfig("BreakTimeout", "Time after average break time before considered failed", 0.0F, 4.8F, 10.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK && (Boolean)this.breakDelayConfig.getValue()));
      this.minTimeoutConfig = this.register(new NumberConfig("MinTimeout", "Minimum time before considering crystal failed", 0.0F, 9.7F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK && (Boolean)this.breakDelayConfig.getValue()));
      this.ticksExistedConfig = this.register(new NumberConfig("TicksExisted", "Min ticks alive to consider crystals for attack", 0, 0, 10, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK));
      this.breakRangeConfig = this.register(new NumberConfig("BreakRange", "Range to break crystals", 0.1F, 3.0F, 6.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK));
      this.breakWallRangeConfig = this.register(new NumberConfig("BreakWallRange", "Range to break crystals through walls", 0.1F, 3.0F, 6.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK));
      this.breakSwingConfig = this.register(new EnumConfig("BreakSwing", "Swing mode when breaking crystals", AutoCrystalModule.SwingMode.Normal, AutoCrystalModule.SwingMode.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BREAK));
      this.rotateConfig = this.register(new BooleanConfig("Rotate", "Rotate before placing and breaking", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE));
      this.strictRotateConfig = this.register(new EnumConfig("YawStep", "Rotates yaw over multiple ticks", AutoCrystalModule.Rotate.OFF, AutoCrystalModule.Rotate.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE && (Boolean)this.rotateConfig.getValue()));
      this.rotateLimitConfig = this.register(new NumberConfig("YawStep-Limit", "Maximum yaw rotation per tick", 1, 180, 180, NumberDisplay.DEGREES, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE && (Boolean)this.rotateConfig.getValue() && this.strictRotateConfig.getValue() != AutoCrystalModule.Rotate.OFF));
      this.whileUsingConfig = this.register(new BooleanConfig("WhileUsing", "Rotate while using items", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE));
      this.whileMiningConfig = this.register(new BooleanConfig("WhileMining", "Allows attacking while mining blocks", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE));
      this.swapPenaltyConfig = this.register(new NumberConfig("SwapPenalty", "Delay for attacking after swapping items", 0.0F, 0.0F, 10.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE));
      this.shieldModeConfig = this.register(new EnumConfig("ShieldMode", "How to handle shields", AutoCrystalModule.ShieldMode.Bypass, AutoCrystalModule.ShieldMode.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.ROTATE));
      this.targetRangeConfig = this.register(new NumberConfig("EnemyRange", "Range to search for potential enemies", 1.0F, 13.0F, 15.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.ignoreNakedsConfig = this.register(new BooleanConfig("IgnoreNakeds", "Ignore players with no armor", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.playersConfig = this.register(new BooleanConfig("Players", "Target players", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.monstersConfig = this.register(new BooleanConfig("Monsters", "Target monsters", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.neutralsConfig = this.register(new BooleanConfig("Neutrals", "Target neutrals", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.animalsConfig = this.register(new BooleanConfig("Animals", "Target animals", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.shulkersConfig = this.register(new BooleanConfig("Shulkers", "Target shulker boxes", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.supportConfig = this.register(new BooleanConfig("Support", "Support mode for team play", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.airPlaceConfig = this.register(new BooleanConfig("AirPlace", "Allow placing crystals in air", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.minDamageConfig = this.register(new NumberConfig("MinDamage", "Minimum damage to consider attacking or placing", 1.0F, 3.0F, 10.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.maxLocalDamageConfig = this.register(new NumberConfig("MaxLocalDamage", "Maximum player damage", 4.0F, 10.0F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.damageRatioConfig = this.register(new NumberConfig("DamageRatio", "Required ratio of target damage to self damage", 0.0F, 1.5F, 5.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.instantCalcConfig = this.register(new BooleanConfig("Instant-Calc", "Calculates crystal on spawn and attacks if meets requirements", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.instantDamageConfig = this.register(new NumberConfig("InstantDamage", "Min damage to attack crystals instantly", 1.0F, 4.5F, 10.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC && (Boolean)this.instantCalcConfig.getValue()));
      this.instantMaxConfig = this.register(new BooleanConfig("InstantMax", "Attacks instantly if exceeds previous max damage", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC && (Boolean)this.instantCalcConfig.getValue()));
      this.ignoreItemConfig = this.register(new BooleanConfig("IgnoreItem", "Ignore item in hand when calculating", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.assumeArmorConfig = this.register(new BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.lethalMultiplier = this.register(new NumberConfig("LethalMultiplier", "Kill multiplier to disregard damage values", 0.0F, 1.5F, 4.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.antiTotemConfig = this.register(new BooleanConfig("Lethal-Totem", "Predicts totems and places crystals to double pop", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.lethalDamageConfig = this.register(new BooleanConfig("Lethal-DamageTick", "Places lethal crystals only on damage ticks", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.safetyConfig = this.register(new BooleanConfig("Safety", "Accounts for total player safety", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.safetyOverride = this.register(new BooleanConfig("SafetyOverride", "Overrides safety if crystal will kill enemy", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.blockDestructionConfig = this.register(new BooleanConfig("BlockDestruction", "Accounts for explosion block destruction", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.selfExtrapolateConfig = this.register(new BooleanConfig("SelfExtrapolate", "Accounts for motion when calculating self damage", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.extrapolateTicksConfig = this.register(new NumberConfig("ExtrapolationTicks", "Ticks to extrapolate enemy positions", 0, 2, 10, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.armorBreakerConfig = this.register(new BooleanConfig("ArmorBreaker", "Attempts to break enemy armor with crystals", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.armorScaleConfig = this.register(new NumberConfig("ArmorScale", "Armor damage scale before breaking armor", 1.0F, 5.0F, 20.0F, NumberDisplay.PERCENT, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC && (Boolean)this.armorBreakerConfig.getValue()));
      this.asyncConfig = this.register(new BooleanConfig("Asynchronous", "Runs calculations asynchronously", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.maxOverrideConfig = this.register(new NumberConfig("MaxOverride", "Max override damage threshold", 0.0F, 7.0F, 20.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.CALC));
      this.renderModeConfig = this.register(new EnumConfig("Render", "Render mode for placements", AutoCrystalModule.RenderMode.SLIDER, AutoCrystalModule.RenderMode.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER));
      this.boxModeConfig = this.register(new EnumConfig("BoxMode", "Box render mode", AutoCrystalModule.BoxMode.Both, AutoCrystalModule.BoxMode.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.boxColorConfig = this.register(new ColorConfig("BoxColor", "Box outline color", new Color(-51016985, true), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.fillColorConfig = this.register(new ColorConfig("FillColor", "Box fill color", new Color(452299495, true), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.widthConfig = this.register(new NumberConfig("Width", "Box outline width", 0.1F, 1.0F, 5.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.shrinkConfig = this.register(new BooleanConfig("Shrink", "Shrinks box on placement", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.sliderSpeedConfig = this.register(new NumberConfig("SliderSpeed", "Speed of slider animation", 0.1F, 1.0F, 5.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() == AutoCrystalModule.RenderMode.SLIDER));
      this.startFadeConfig = this.register(new NumberConfig("StartFade", "Opacity at start of fade", 0.0F, 0.1F, 1.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.fadeSpeedConfig = this.register(new NumberConfig("FadeSpeed", "Speed of fade out", 0.1F, 1.0F, 5.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Timer for the fade", 0, 197, 1000, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.renderDamageConfig = this.register(new BooleanConfig("RenderDamage", "Renders damage numbers", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER));
      this.targetDamageConfig = this.register(new BooleanConfig("TargetDamage", "Shows target damage", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && (Boolean)this.renderDamageConfig.getValue()));
      this.selfDamageDisplayConfig = this.register(new BooleanConfig("SelfDamage", "Shows self damage", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER && (Boolean)this.renderDamageConfig.getValue()));
      this.targetNameConfig = this.register(new BooleanConfig("TargetName", "Shows target name", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER));
      this.damageDisplayConfig = this.register(new BooleanConfig("Damage", "Shows damage in HUD", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER));
      this.breakMsConfig = this.register(new BooleanConfig("BreakMs", "Shows break milliseconds", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER));
      this.suffixConfig = this.register(new BooleanConfig("Suffix", "Shows suffix on damage numbers", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER));
      this.cpsConfig = this.register(new BooleanConfig("CPS", "Shows crystals per second", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.RENDER));
      this.baseConfig = this.register(new BooleanConfig("Base", "Auto destroys enemy obsidian/bedrock bases in range", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE));
      this.baseRotateConfig = this.register(new BooleanConfig("Base-Rotate", "Rotate when placing base crystals", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue()));
      this.baseDelayConfig2 = this.register(new NumberConfig("Base-Delay", "Delay between base crystal placements in ticks", 0, 3, 20, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue()));
      this.baseEnemyRangeConfig = this.register(new NumberConfig("Base-EnemyRange", "Range to scan for base blocks", 0.1F, 10.0F, 15.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue()));
      this.baseSwapActionConfig = this.register(new EnumConfig("Base-SwapAction", "Swap action for base mode", AutoCrystalModule.SwapAction.Hotbar, AutoCrystalModule.SwapAction.values(), () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue()));
      this.baseShiftDelayConfig = this.register(new NumberConfig("Base-ShiftDelay", "Shift delay for base placements", 0.0F, 1.0F, 5.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue()));
      this.baseMinDamageConfig = this.register(new NumberConfig("Base-MinDamage", "Min damage required to place base crystal", 1.0F, 4.0F, 10.0F, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue()));
      this.baseAssumeArmorConfig = this.register(new BooleanConfig("Base-AssumeBestArmor", "Assumes Prot 0 armor is max armor", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue()));
      this.baseRenderConfig = this.register(new BooleanConfig("Base-Render", "Renders base placement positions", true, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue()));
      this.baseFadeTimeConfig = this.register(new NumberConfig("Base-FadeTime", "Fade time for base render", 0, 250, 1000, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.BASE && (Boolean)this.baseConfig.getValue() && (Boolean)this.baseRenderConfig.getValue()));
      this.debugConfig = this.register(new BooleanConfig("Debug", "Adds extra debug info to arraylist", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC));
      this.debugDamageConfig = this.register(new BooleanConfig("Debug-Damage", "Renders damage", false, () -> this.pageConfig.getValue() == AutoCrystalModule.Page.MISC && this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE));
      this.calculatePlaceCrystalTime = 0.0F;
      this.lastAttackTimer = new CacheTimer();
      this.lastPlaceTimer = new CacheTimer();
      this.lastBaseTimer = new CacheTimer();
      this.lastSwapTimer = new CacheTimer();
      this.autoSwapTimer = new CacheTimer();
      this.attackLatency = new EvictingQueue<Long>(20);
      this.attackPackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
      this.placePackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
      this.crystalCounter = new PerSecondCounter();
      this.fadeList = new HashMap<>();
      this.antiStuckCrystals = new HashMap<>();
      this.stuckCrystals = new CopyOnWriteArrayList();
      this.executor = Executors.newFixedThreadPool(2);
      this.sphereOffsetCache = new ConcurrentHashMap<>();
      INSTANCE = this;
   }

   public static AutoCrystalModule getInstance() {
      return INSTANCE;
   }

   public String getModuleData() {
      return (Boolean)this.debugConfig.getValue() ? String.format("%sms, %.0f, %dms, %d".formatted((new DecimalFormat("0.00")).format((double)this.calculatePlaceCrystalTime / (double)1000000.0F), this.placeCrystal == null ? (double)0.0F : (double)this.lastAttackTimer.getLastResetTime() / (double)1000000.0F, this.lastAttackTimer.passed((Float)this.breakDelayFloatConfig.getValue() / 50.0F * 50.0F + 2000.0F) ? 0 : this.getBreakMs(), this.crystalCounter.getPerSecond())) : String.format("%dms, %d", this.lastAttackTimer.passed((Float)this.breakDelayFloatConfig.getValue() / 50.0F * 50.0F + 2000.0F) ? 0 : this.getBreakMs(), this.crystalCounter.getPerSecond());
   }

   public void onDisable() {
      this.renderPos = null;
      this.attackCrystal = null;
      this.placeCrystal = null;
      this.crystalRotation = null;
      this.silentRotations = null;
      this.calculatePlaceCrystalTime = 0.0F;
      this.stuckCrystals.clear();
      this.attackPackets.clear();
      this.antiStuckCrystals.clear();
      this.placePackets.clear();
      this.attackLatency.clear();
      this.fadeList.clear();
      this.setStage("NONE");
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      if (!mc.player.isSpectator() && (!this.isSilentSwap(this.autoSwapConfig.getValue()) || !AutoMineModule.getInstance().isSilentSwapping())) {
         for(AntiStuckData d : this.stuckCrystals) {
            double dist = mc.player.squaredDistanceTo(d.pos());
            double diff = d.stuckDist() - dist;
            if (diff > (double)0.5F) {
               this.stuckCrystals.remove(d);
            }
         }

         if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND || mc.options.attackKey.isPressed() || PlayerUtil.isHotbarKeysPressed()) {
            this.autoSwapTimer.reset();
         }

         this.renderPos = null;
         ArrayList<net.minecraft.entity.Entity> entities = Lists.newArrayList(mc.world.getEntities());
         Set<net.minecraft.entity.Entity> entitySet = Collections.newSetFromMap(new IdentityHashMap(entities.size() + 8));
         entitySet.addAll(entities);

         for(PlayerEntity p : mc.world.getPlayers()) {
            if (entitySet.add(p)) {
               entities.add(p);
            }
         }

         List<net.minecraft.entity.Entity> validTargets = new ArrayList<>();

         for(net.minecraft.entity.Entity entity : entities) {
            if (entity != null && entity != mc.player && entity.isAlive() && this.isValidTarget(entity) && !Managers.SOCIAL.isFriend(entity.getName())) {
               validTargets.add(entity);
            }
         }

         boolean hasPlaceMaterials = (Boolean)this.placeConfig.getValue() && InventoryUtil.count(Items.END_CRYSTAL) > 0;
         List<BlockPos> blocks = !validTargets.isEmpty() && hasPlaceMaterials ? this.getSphere(mc.player.getPos()) : Collections.emptyList();
         if ((Boolean)this.baseConfig.getValue() && !validTargets.isEmpty()) {
            this.handleBaseDestroy(validTargets);
         }

         long timePre = System.nanoTime();
         if (hasPlaceMaterials && !validTargets.isEmpty()) {
            this.placeCrystal = this.calculatePlaceCrystal(blocks, validTargets);
         } else {
            this.placeCrystal = null;
         }

         this.attackCrystal = this.calculateAttackCrystal(entities, validTargets);
         if (this.attackCrystal == null) {
            if (this.placeCrystal != null) {
               EndCrystalEntity crystalEntity = this.intersectingCrystalCheck(this.placeCrystal.getDamageData());
               if (crystalEntity != null) {
                  double self = ExplosionUtil.getDamageTo(mc.player, crystalEntity.getPos(), (Boolean)this.blockDestructionConfig.getValue(), (Boolean)this.selfExtrapolateConfig.getValue() ? (Integer)this.extrapolateTicksConfig.getValue() : 0, false);
                  if (!(Boolean)this.safetyConfig.getValue() || !this.playerDamageCheck(self)) {
                     this.attackCrystal = new DamageData<EndCrystalEntity>(crystalEntity, this.placeCrystal.getAttackTarget(), this.placeCrystal.getDamage(), self, crystalEntity.getBlockPos().down(), false);
                  }
               }
            }

            this.calculatePlaceCrystalTime = (float)(System.nanoTime() - timePre);
         }

         if ((Boolean)this.inhibitConfig.getValue() && this.attackCrystal != null && this.attackPackets.containsKey(((EndCrystalEntity)this.attackCrystal.getDamageData()).getId())) {
            float delay;
            if ((double)(Float)this.attackDelayConfig.getValue() > (double)0.0F) {
               float attackFactor = 50.0F / Math.max(1.0F, (float)(Integer)this.attackFactorConfig.getValue());
               delay = (Float)this.attackDelayConfig.getValue() * attackFactor;
            } else {
               delay = Math.max(0.0F, (Float)this.breakDelayFloatConfig.getValue());
            }

            this.lastAttackTimer.setDelay(delay + 100.0F);
            this.attackPackets.remove(((EndCrystalEntity)this.attackCrystal.getDamageData()).getId());
         }

         float breakDelay = this.getBreakDelay();
         if ((Boolean)this.breakDelayConfig.getValue()) {
            breakDelay = Math.max((Float)this.minTimeoutConfig.getValue() * 50.0F, (float)this.getBreakMs() + (Float)this.breakTimeoutConfig.getValue() * 50.0F);
         }

         this.attackRotate = this.attackCrystal != null && (double)(Float)this.attackDelayConfig.getValue() <= (double)0.0F && this.lastAttackTimer.passed(breakDelay);
         if (this.attackCrystal != null) {
            this.crystalRotation = ((EndCrystalEntity)this.attackCrystal.damageData).getPos();
         } else if (this.placeCrystal != null) {
            this.crystalRotation = ((BlockPos)this.placeCrystal.damageData).toCenterPos().add((double)0.0F, (double)0.5F, (double)0.0F);
         }

         if ((Boolean)this.rotateConfig.getValue() && this.crystalRotation != null && (this.placeCrystal == null || this.canHoldCrystal())) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), this.crystalRotation);
            if (this.strictRotateConfig.getValue() == AutoCrystalModule.Rotate.FULL || this.strictRotateConfig.getValue() == AutoCrystalModule.Rotate.SEMI && this.attackRotate) {
               float serverYaw = Managers.ROTATION.getWrappedYaw();
               float diff = serverYaw - rotations[0];
               float diff1 = Math.abs(diff);
               if (diff1 > 180.0F) {
                  diff += diff > 0.0F ? -360.0F : 360.0F;
               }

               int dir = diff > 0.0F ? -1 : 1;
               float deltaYaw = (float)(dir * (Integer)this.rotateLimitConfig.getValue());
               float yaw;
               if (diff1 > (float)(Integer)this.rotateLimitConfig.getValue()) {
                  yaw = serverYaw + deltaYaw;
                  this.rotated = false;
               } else {
                  yaw = rotations[0];
                  this.rotated = true;
                  this.crystalRotation = null;
               }

               rotations[0] = yaw;
            } else {
               this.rotated = true;
               this.crystalRotation = null;
            }

            this.setRotation(rotations[0], rotations[1]);
         } else {
            this.silentRotations = null;
         }

         if (!this.isRotationBlocked() && (this.rotated || !(Boolean)this.rotateConfig.getValue())) {
            Hand hand = this.getCrystalHand();
            if (this.attackCrystal != null && this.attackRotate) {
               this.attackCrystal(this.attackCrystal.getDamageData(), hand);
               this.setStage("ATTACKING");
               this.lastAttackTimer.reset();
            }

            boolean placeRotate = this.lastPlaceTimer.passed(1000.0F - (Float)this.placeDelayConfig.getValue() * 50.0F);
            if (this.placeCrystal != null) {
               this.renderPos = this.placeCrystal.getDamageData();
               this.renderDamage = this.placeCrystal.getDamage();
               if (placeRotate) {
                  this.placeCrystal(this.placeCrystal.getDamageData(), hand);
                  this.setStage("PLACING");
                  this.lastPlaceTimer.reset();
               }
            }

         }
      }
   }

   private int getBlockItemSlotLocal(Block block) {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         Item var5 = stack.getItem();
         if (var5 instanceof BlockItem blockItem) {
            if (blockItem.getBlock() == block) {
               return i;
            }
         }
      }

      return -1;
   }

   private void handleBaseDestroy(List<net.minecraft.entity.Entity> validTargets) {
      if (this.lastBaseTimer.passed((Float)this.baseShiftDelayConfig.getValue() * 1000.0F)) {
         if (this.attackCrystal == null) {
            if (this.getCrystalSlot() != -1) {
               double rad = Math.ceil((double)(Float)this.baseEnemyRangeConfig.getValue());
               double rangeSq = (double)((Float)this.baseEnemyRangeConfig.getValue() * (Float)this.baseEnemyRangeConfig.getValue());
               BlockPos existingPos = null;
               double bestExistingDist = Double.MAX_VALUE;

               for(BlockPos pos : this.getSphere(rad, mc.player.getPos())) {
                  if (this.canUseCrystalOnBlock(pos)) {
                     double dist = mc.player.squaredDistanceTo(pos.toCenterPos());
                     if (!(dist > rangeSq)) {
                        double enemyDamage = (double)0.0F;

                        for(net.minecraft.entity.Entity entity : validTargets) {
                           double dmg = ExplosionUtil.getDamageTo(entity, this.crystalDamageVec(pos), (Boolean)this.baseAssumeArmorConfig.getValue());
                           if (dmg > enemyDamage) {
                              enemyDamage = dmg;
                           }
                        }

                        if (!(enemyDamage < (double)(Float)this.baseMinDamageConfig.getValue())) {
                           double selfDamage = ExplosionUtil.getDamageTo(mc.player, this.crystalDamageVec(pos), (Boolean)this.blockDestructionConfig.getValue(), Set.of(pos), (Boolean)this.selfExtrapolateConfig.getValue() ? (Integer)this.extrapolateTicksConfig.getValue() : 0, (Boolean)this.baseAssumeArmorConfig.getValue());
                           if (!this.playerDamageCheck(selfDamage) && dist < bestExistingDist) {
                              bestExistingDist = dist;
                              existingPos = pos;
                           }
                        }
                     }
                  }
               }

               if (existingPos != null) {
                  float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), existingPos.toCenterPos());
                  this.setRotation(rotations[0], rotations[1]);
                  if (!this.isRotationBlocked()) {
                     this.placeCrystal(existingPos, this.getCrystalHand());
                     if ((Boolean)this.baseRenderConfig.getValue()) {
                        this.fadeList.put(existingPos, new Animation(true, (float)(Integer)this.baseFadeTimeConfig.getValue()));
                     }

                     this.lastBaseTimer.reset();
                  }
               } else {
                  int obsidianSlot = this.getBlockItemSlotLocal(Blocks.OBSIDIAN);
                  if (obsidianSlot != -1) {
                     BlockPos placePos = null;
                     double bestPlaceDamage = (double)0.0F;

                     for(BlockPos pos : this.getSphere(rad, mc.player.getEyePos())) {
                        BlockPos basePos = pos.down();
                        if (basePos.getY() < mc.player.getBlockY() && mc.world.getBlockState(basePos).isReplaceable() && this.isCrystalHitboxClear(pos)) {
                           double dist = mc.player.squaredDistanceTo(basePos.toCenterPos());
                           if (!(dist > rangeSq)) {
                              double selfDamage = ExplosionUtil.getDamageTo(mc.player, this.crystalDamageVec(pos), (Boolean)this.blockDestructionConfig.getValue(), Set.of(pos), (Boolean)this.selfExtrapolateConfig.getValue() ? (Integer)this.extrapolateTicksConfig.getValue() : 0, (Boolean)this.baseAssumeArmorConfig.getValue());
                              if (!this.playerDamageCheck(selfDamage)) {
                                 double enemyDamage = (double)0.0F;

                                 for(net.minecraft.entity.Entity entity : validTargets) {
                                    double dmg = ExplosionUtil.getDamageTo(entity, this.crystalDamageVec(pos), (Boolean)this.baseAssumeArmorConfig.getValue());
                                    if (dmg > enemyDamage) {
                                       enemyDamage = dmg;
                                    }
                                 }

                                 if (!(enemyDamage < (double)(Float)this.baseMinDamageConfig.getValue()) && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), basePos, ShapeContext.absent()) && enemyDamage > bestPlaceDamage) {
                                    bestPlaceDamage = enemyDamage;
                                    placePos = basePos;
                                 }
                              }
                           }
                        }
                     }

                     if (placePos != null) {
                        Managers.INTERACT.placeBlock((BlockPos)placePos, obsidianSlot, false, false, true, (state, angles) -> {
                           if ((Boolean)this.baseRotateConfig.getValue() && state) {
                              this.setRotation(angles[0], angles[1]);
                           }

                        });
                        if ((Boolean)this.baseRenderConfig.getValue()) {
                           this.fadeList.put(placePos, new Animation(true, (float)(Integer)this.baseFadeTimeConfig.getValue()));
                        }

                        this.lastBaseTimer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   @EventListener
   public void onRunTick(RunTickEvent event) {
      if (mc.player != null) {
         Hand hand = this.getCrystalHand();
         if ((double)(Float)this.attackDelayConfig.getValue() > (double)0.0F) {
            float attackFactor = 50.0F / Math.max(1.0F, (float)(Integer)this.attackFactorConfig.getValue());
            if (this.attackCrystal != null && this.lastAttackTimer.passed((Float)this.attackDelayConfig.getValue() * attackFactor)) {
               this.attackCrystal(this.attackCrystal.getDamageData(), hand);
               this.lastAttackTimer.reset();
            }
         }

      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.renderModeConfig.getValue() != AutoCrystalModule.RenderMode.NONE) {
         RenderBuffers.preRender();
         BlockPos renderPos1 = null;
         double factor = (double)0.0F;

         for(Map.Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            if (set.getKey() != this.renderPos) {
               if (((Animation)set.getValue()).getFactor() > factor) {
                  renderPos1 = (BlockPos)set.getKey();
                  factor = ((Animation)set.getValue()).getFactor();
               }

               ((Animation)set.getValue()).setState(false);
               int boxAlpha = (int)((double)40.0F * ((Animation)set.getValue()).getFactor());
               int lineAlpha = (int)((double)100.0F * ((Animation)set.getValue()).getFactor());
               Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
               Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
               RenderManager.renderBox(event.getMatrices(), (BlockPos)set.getKey(), boxColor.getRGB());
               RenderManager.renderBoundingBox(event.getMatrices(), (BlockPos)set.getKey(), 1.5F, lineColor.getRGB());
            }
         }

         if ((Boolean)this.debugDamageConfig.getValue() && renderPos1 != null) {
            RenderManager.renderSign(String.format("%.1f", this.renderDamage), renderPos1.toCenterPos(), (new Color(255, 255, 255, (int)((double)255.0F * factor))).getRGB());
         }

         RenderBuffers.postRender();
         this.fadeList.entrySet().removeIf((e) -> ((Animation)e.getValue()).getFactor() == (double)0.0F);
         if (this.renderPos != null && this.isHoldingCrystal()) {
            Animation animation = new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue());
            this.fadeList.put(this.renderPos, animation);
         }
      }

   }

   @EventListener(
      priority = Integer.MAX_VALUE
   )
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof BundleS2CPacket) {
            BundleS2CPacket packet = (BundleS2CPacket)var3;

            for(Packet<?> packet1 : packet.getPackets()) {
               this.handleServerPackets(packet1);
            }
         } else {
            this.handleServerPackets(event.getPacket());
         }

      }
   }

   private void handleServerPackets(Packet<?> serverPacket) {
      if (serverPacket instanceof ExplosionS2CPacket packet) {
         for(net.minecraft.entity.Entity entity : Lists.newArrayList(mc.world.getEntities())) {
            if (entity instanceof EndCrystalEntity && entity.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ()) < (double)144.0F) {
               mc.executeSync(() -> mc.world.removeEntity(entity.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED));
               this.antiStuckCrystals.remove(entity.getId());
               Long attackTime = (Long)this.attackPackets.remove(entity.getId());
               if (attackTime != null) {
                  this.attackLatency.add(System.currentTimeMillis() - attackTime);
               }
            }
         }
      }

      if (serverPacket instanceof PlaySoundS2CPacket packet) {
         if (packet.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE.value() && packet.getCategory() == SoundCategory.BLOCKS) {
            for(net.minecraft.entity.Entity entity : Lists.newArrayList(mc.world.getEntities())) {
               if (entity instanceof EndCrystalEntity && entity.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ()) < (double)144.0F) {
                  mc.executeSync(() -> mc.world.removeEntity(entity.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED));
                  this.antiStuckCrystals.remove(entity.getId());
                  Long attackTime = (Long)this.attackPackets.remove(entity.getId());
                  if (attackTime != null) {
                     this.attackLatency.add(System.currentTimeMillis() - attackTime);
                  }
               }
            }
         }
      }

      if (serverPacket instanceof EntitiesDestroyS2CPacket packet) {
         IntListIterator var11 = packet.getEntityIds().iterator();

         while(var11.hasNext()) {
            int id = (Integer)var11.next();
            this.antiStuckCrystals.remove(id);
            Long attackTime = (Long)this.attackPackets.remove(id);
            if (attackTime != null) {
               this.attackLatency.add(System.currentTimeMillis() - attackTime);
            }
         }
      }

      if (serverPacket instanceof ExperienceOrbSpawnS2CPacket packet) {
         if ((long)packet.getEntityId() > this.predictId) {
            this.predictId = (long)packet.getEntityId();
         }
      }

      if (serverPacket instanceof EntitySpawnS2CPacket packet) {
         if ((long)packet.getEntityId() > this.predictId) {
            this.predictId = (long)packet.getEntityId();
         }
      }

   }

   @EventListener
   public void onAddEntity(AddEntityEvent event) {
      net.minecraft.entity.Entity var3 = event.getEntity();
      if (var3 instanceof EndCrystalEntity crystalEntity) {
         Vec3d crystalPos = crystalEntity.getPos();
         BlockPos blockPos = BlockPos.ofFloored(crystalPos.add((double)0.0F, (double)-1.0F, (double)0.0F));
         this.renderSpawnPos = blockPos;
         Long time = (Long)this.placePackets.remove(blockPos);
         this.attackRotate = time != null;
         if (this.attackRotate) {
            this.crystalCounter.updateCounter();
         }

         if ((Boolean)this.instantConfig.getValue()) {
            if (this.attackRotate) {
               Hand hand = this.getCrystalHand();
               this.attackInternal(crystalEntity, hand);
               this.setStage("ATTACKING");
               this.lastAttackTimer.reset();
               if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.NORMAL) {
                  this.placeSequentialCrystal(hand);
               }
            } else if ((Boolean)this.instantCalcConfig.getValue()) {
               if (this.attackRangeCheck(crystalPos)) {
                  return;
               }

               double selfDamage = ExplosionUtil.getDamageTo(mc.player, crystalPos, (Boolean)this.blockDestructionConfig.getValue(), (Boolean)this.selfExtrapolateConfig.getValue() ? (Integer)this.extrapolateTicksConfig.getValue() : 0, false);
               if (this.playerDamageCheck(selfDamage)) {
                  return;
               }

               for(net.minecraft.entity.Entity entity : mc.world.getEntities()) {
                  if (entity != null && entity.isAlive() && entity != mc.player && this.isValidTarget(entity) && !Managers.SOCIAL.isFriend(entity.getName())) {
                     double crystalDist = crystalPos.squaredDistanceTo(entity.getPos());
                     if (!(crystalDist > (double)144.0F)) {
                        double dist = mc.player.squaredDistanceTo(entity);
                        if (!(dist > (double)((Float)this.targetRangeConfig.getValue() * (Float)this.targetRangeConfig.getValue()))) {
                           boolean var10001;
                           label104: {
                              double damage = ExplosionUtil.getDamageTo(entity, crystalPos, (Boolean)this.blockDestructionConfig.getValue(), (Integer)this.extrapolateTicksConfig.getValue(), (Boolean)this.assumeArmorConfig.getValue());
                              DamageData<EndCrystalEntity> data = new DamageData<EndCrystalEntity>(crystalEntity, entity, damage, selfDamage, crystalEntity.getBlockPos().down(), false);
                              if (!(damage > (double)(Float)this.instantDamageConfig.getValue()) && (this.attackCrystal == null || !(damage >= this.attackCrystal.getDamage()) || !(Boolean)this.instantMaxConfig.getValue())) {
                                 label107: {
                                    if (entity instanceof LivingEntity) {
                                       LivingEntity entity1 = (LivingEntity)entity;
                                       if (this.isCrystalLethalTo(data, entity1)) {
                                          break label107;
                                       }
                                    }

                                    var10001 = false;
                                    break label104;
                                 }
                              }

                              var10001 = true;
                           }

                           this.attackRotate = var10001;
                           if (this.attackRotate) {
                              Hand hand = this.getCrystalHand();
                              this.attackInternal(crystalEntity, hand);
                              this.setStage("ATTACKING");
                              this.lastAttackTimer.reset();
                              if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.NORMAL) {
                                 this.placeSequentialCrystal(hand);
                              }
                              break;
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            this.lastSwapTimer.reset();
         }

      }
   }

   public boolean isAttacking() {
      return this.attackCrystal != null;
   }

   public boolean isPlacing() {
      return this.placeCrystal != null && this.isHoldingCrystal();
   }

   public void attackCrystal(EndCrystalEntity entity, Hand hand) {
      if (!this.attackCheckPre(hand)) {
         StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
         StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
         if (weakness != null && (strength == null || weakness.getAmplifier() > strength.getAmplifier())) {
            int slot = -1;

            for(int i = 0; i < 9; ++i) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (!stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof PickaxeItem)) {
                  slot = i;
                  break;
               }
            }

            if (slot != -1) {
               boolean canSwap = slot != Managers.INVENTORY.getServerSlot() && this.autoSwapTimer.passed(500);
               this.attackInternal(entity, Hand.MAIN_HAND);
               if (canSwap) {
               }

               if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.STRICT) {
                  this.placeSequentialCrystal(hand);
               }
            }
         } else {
            this.attackInternal(entity, hand);
            if (this.sequentialConfig.getValue() == AutoCrystalModule.Sequential.STRICT) {
               this.placeSequentialCrystal(hand);
            }
         }

      }
   }

   private void attackInternal(EndCrystalEntity crystalEntity, Hand hand) {
      this.attackInternal(crystalEntity.getId(), hand);
   }

   private void attackInternal(int crystalEntity, Hand hand) {
      hand = hand != null ? hand : Hand.MAIN_HAND;
      EndCrystalEntity entity2 = new EndCrystalEntity(mc.world, (double)0.0F, (double)0.0F, (double)0.0F);
      entity2.setId(crystalEntity);
      PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(entity2, mc.player.isSneaking());
      Managers.NETWORK.sendPacket(packet);
      if (this.placeSwingConfig.getValue() != AutoCrystalModule.SwingMode.None) {
         mc.player.swingHand(hand);
      } else {
         Managers.NETWORK.sendPacket(new HandSwingC2SPacket(hand));
      }

      this.attackPackets.put(crystalEntity, System.currentTimeMillis());
      Integer antiStuckCount = (Integer)this.antiStuckCrystals.get(crystalEntity);
      if (antiStuckCount != null) {
         this.antiStuckCrystals.replace(crystalEntity, antiStuckCount + 1);
      } else {
         this.antiStuckCrystals.put(crystalEntity, 1);
      }

   }

   private void placeSequentialCrystal(Hand hand) {
      if (this.placeCrystal != null) {
         int latency = FastLatencyModule.getInstance().isEnabled() ? (int)FastLatencyModule.getInstance().getLatency() : Managers.NETWORK.getClientLatency();
         if (!Managers.NETWORK.is2b2t() || latency >= 50) {
            this.placeCrystal(this.placeCrystal.getBlockPos(), hand);
         }

      }
   }

   private void placeCrystal(BlockPos blockPos, Hand hand) {
      if (!this.isRotationBlocked() && (this.rotated || !(Boolean)this.rotateConfig.getValue())) {
         this.placeCrystal(blockPos, hand, true);
      }
   }

   public void placeCrystal(BlockPos blockPos, Hand hand, boolean checkPlacement) {
      if (!checkPlacement || !this.checkCanUseCrystal()) {
         Direction sidePlace = this.getPlaceDirection(blockPos);
         BlockHitResult result = new BlockHitResult(blockPos.toCenterPos(), sidePlace, blockPos, false);
         if (this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.OFF && hand != Hand.OFF_HAND && this.getCrystalHand() == null) {
            if (this.isSilentSwap(this.autoSwapConfig.getValue()) && InventoryUtil.count(Items.END_CRYSTAL) == 0) {
               return;
            }

            int crystalSlot = this.getCrystalSlot();
            if (crystalSlot != -1) {
               boolean canSwap = crystalSlot != Managers.INVENTORY.getServerSlot() && (this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.NORMAL || this.autoSwapTimer.passed(500));
               if (canSwap) {
                  if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT_ALT) {
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, crystalSlot + 36, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                  } else if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT) {
                     Managers.INVENTORY.setSlot(crystalSlot);
                  } else {
                     Managers.INVENTORY.setClientSlot(crystalSlot);
                  }
               }

               this.placeInternal(result, Hand.MAIN_HAND);
               this.placePackets.put(blockPos, System.currentTimeMillis());
               if (canSwap) {
                  if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT_ALT) {
                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, crystalSlot + 36, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                  } else if (this.autoSwapConfig.getValue() == AutoCrystalModule.Swap.SILENT) {
                     Managers.INVENTORY.syncToClient();
                  }
               }
            }
         } else if (this.isHoldingCrystal()) {
            this.placeInternal(result, hand);
            this.placePackets.put(blockPos, System.currentTimeMillis());
         }

      }
   }

   private void placeInternal(BlockHitResult result, Hand hand) {
      if (hand != null) {
         Managers.NETWORK.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(hand, result, id));
         if (this.placeSwingConfig.getValue() != AutoCrystalModule.SwingMode.None) {
            mc.player.swingHand(hand);
         } else {
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(hand));
         }

      }
   }

   private boolean isSilentSwap(Swap swap) {
      return swap == AutoCrystalModule.Swap.SILENT || swap == AutoCrystalModule.Swap.SILENT_ALT;
   }

   private int getCrystalSlot() {
      int slot = -1;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof EndCrystalItem) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   private Direction getPlaceDirection(BlockPos blockPos) {
      int x = blockPos.getX();
      int y = blockPos.getY();
      int z = blockPos.getZ();
      if (mc.world.isInBuildLimit(blockPos)) {
         return Direction.DOWN;
      } else {
         BlockHitResult result = mc.world.raycast(new net.minecraft.world.RaycastContext(mc.player.getEyePos(), new Vec3d((double)x + (double)0.5F, (double)y + (double)0.5F, (double)z + (double)0.5F), net.minecraft.world.RaycastContext.ShapeType.OUTLINE, net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
         return result != null && result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK ? result.getSide() : Direction.UP;
      }
   }

   private DamageData<EndCrystalEntity> calculateAttackCrystal(List<net.minecraft.entity.Entity> entities, List<net.minecraft.entity.Entity> validTargets) {
      if (entities.isEmpty()) {
         return null;
      } else {
         List<DamageData<EndCrystalEntity>> validData = new ArrayList<>();
         DamageData<EndCrystalEntity> data = null;

         for(net.minecraft.entity.Entity crystal : entities) {
            if (crystal instanceof EndCrystalEntity) {
               EndCrystalEntity crystal1 = (EndCrystalEntity)crystal;
               if (crystal.isAlive() && !this.stuckCrystals.stream().anyMatch((d) -> d.id() == crystal.getId())) {
                  Long time = (Long)this.attackPackets.get(crystal.getId());
                  boolean attacked = time != null && time < (long)this.getBreakMs();
                  if ((crystal.age >= (Integer)this.ticksExistedConfig.getValue() && !attacked || !(Boolean)this.inhibitConfig.getValue()) && !this.attackRangeCheck(crystal1)) {
                     double selfDamage = ExplosionUtil.getDamageTo(mc.player, crystal.getPos(), (Boolean)this.blockDestructionConfig.getValue(), (Boolean)this.selfExtrapolateConfig.getValue() ? (Integer)this.extrapolateTicksConfig.getValue() : 0, false);
                     boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
                     if (!unsafeToPlayer || (Boolean)this.safetyOverride.getValue()) {
                        for(net.minecraft.entity.Entity entity : validTargets) {
                           double crystalDist = crystal.squaredDistanceTo(entity);
                           if (!(crystalDist > (double)144.0F)) {
                              double dist = mc.player.squaredDistanceTo(entity);
                              if (!(dist > (double)((Float)this.targetRangeConfig.getValue() * (Float)this.targetRangeConfig.getValue()))) {
                                 boolean antiSurround = false;
                                 if ((Boolean)this.antiSurroundConfig.getValue() && entity instanceof PlayerEntity) {
                                    PlayerEntity player = (PlayerEntity)entity;
                                    if (!BlastResistantBlocks.isUnbreakable(player.getBlockPos())) {
                                       Set<BlockPos> miningPositions = new HashSet<>();
                                       BlockPos miningBlock = AutoMineModule.getInstance().getMiningBlock();
                                       if (AutoMineModule.getInstance().isEnabled() && miningBlock != null) {
                                          miningPositions.add(miningBlock);
                                       }

                                       if (Managers.BLOCK.getMines(0.75F).contains(player.getBlockPos().up())) {
                                          miningPositions.add(player.getBlockPos().up());
                                       }

                                       for(BlockPos miningBlockPos : miningPositions) {
                                          if (SurroundModule.getInstance().getSurroundNoDown(player).contains(miningBlockPos)) {
                                             for(Direction direction : Direction.values()) {
                                                BlockPos pos1 = miningBlockPos.offset(direction);
                                                if (crystal.getBlockPos().equals(pos1.down())) {
                                                   antiSurround = true;
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }

                                 double damage = ExplosionUtil.getDamageTo(entity, crystal.getPos(), (Boolean)this.blockDestructionConfig.getValue(), (Integer)this.extrapolateTicksConfig.getValue(), (Boolean)this.assumeArmorConfig.getValue());
                                 if (!this.checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                                    DamageData<EndCrystalEntity> currentData = new DamageData<EndCrystalEntity>(crystal1, entity, damage, selfDamage, crystal1.getBlockPos().down(), antiSurround);
                                    validData.add(currentData);
                                    if (data == null || damage > data.getDamage()) {
                                       data = currentData;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (data != null && !this.targetDamageCheck(data)) {
            return data;
         } else if ((Boolean)this.antiSurroundConfig.getValue()) {
            return (DamageData)validData.stream().filter(DamageData::isAntiSurround).min(Comparator.comparingDouble((d) -> mc.player.squaredDistanceTo(d.getBlockPos().toCenterPos()))).orElse(null);
         } else {
            return null;
         }
      }
   }

   private boolean attackRangeCheck(EndCrystalEntity entity) {
      return this.attackRangeCheck(entity.getPos());
   }

   private boolean attackRangeCheck(Vec3d entityPos) {
      double breakRange = (double)(Float)this.breakRangeConfig.getValue();
      double breakWallRange = (double)(Float)this.breakWallRangeConfig.getValue();
      Vec3d playerPos = mc.player.getEyePos();
      double dist = playerPos.squaredDistanceTo(entityPos);
      if (dist > breakRange * breakRange) {
         return true;
      } else {
         double yOff = Math.abs(entityPos.getY() - mc.player.getY());
         if (yOff > (double)5.0F) {
            return true;
         } else {
            BlockHitResult result = mc.world.raycast(new net.minecraft.world.RaycastContext(playerPos, entityPos, net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
            return result.getType() != net.minecraft.util.hit.HitResult.Type.MISS && dist > breakWallRange * breakWallRange;
         }
      }
   }

   private DamageData<BlockPos> calculatePlaceCrystal(List<BlockPos> placeBlocks, List<net.minecraft.entity.Entity> validTargets) {
      if (!placeBlocks.isEmpty() && !validTargets.isEmpty()) {
         List<DamageData<BlockPos>> validData = new ArrayList<>();
         DamageData<BlockPos> data = null;

         for(BlockPos pos : placeBlocks) {
            if (this.canUseCrystalOnBlock(pos) && !this.placeRangeCheck(pos) && !this.intersectingAntiStuckCheck(pos)) {
               double selfDamage = ExplosionUtil.getDamageTo(mc.player, this.crystalDamageVec(pos), (Boolean)this.blockDestructionConfig.getValue(), (Boolean)this.selfExtrapolateConfig.getValue() ? (Integer)this.extrapolateTicksConfig.getValue() : 0, false);
               boolean unsafeToPlayer = this.playerDamageCheck(selfDamage);
               if (!unsafeToPlayer || (Boolean)this.safetyOverride.getValue()) {
                  for(net.minecraft.entity.Entity entity : validTargets) {
                     double blockDist = pos.getSquaredDistance(entity.getPos());
                     if (!(blockDist > (double)144.0F)) {
                        double dist = mc.player.squaredDistanceTo(entity);
                        if (!(dist > (double)((Float)this.targetRangeConfig.getValue() * (Float)this.targetRangeConfig.getValue()))) {
                           boolean antiSurround = false;
                           if ((Boolean)this.antiSurroundConfig.getValue() && entity instanceof PlayerEntity) {
                              PlayerEntity player = (PlayerEntity)entity;
                              if (!BlastResistantBlocks.isUnbreakable(player.getBlockPos())) {
                                 Set<BlockPos> miningPositions = new HashSet<>();
                                 BlockPos miningBlock = AutoMineModule.getInstance().getMiningBlock();
                                 if (AutoMineModule.getInstance().isEnabled() && miningBlock != null) {
                                    miningPositions.add(miningBlock);
                                 }

                                 if (Managers.BLOCK.getMines(0.75F).contains(player.getBlockPos().up())) {
                                    miningPositions.add(player.getBlockPos().up());
                                 }

                                 for(BlockPos miningBlockPos : miningPositions) {
                                    if (SurroundModule.getInstance().getSurroundNoDown(player).contains(miningBlockPos)) {
                                       for(Direction direction : Direction.values()) {
                                          BlockPos pos1 = miningBlockPos.offset(direction);
                                          if (pos.equals(pos1.down())) {
                                             antiSurround = true;
                                          }
                                       }
                                    }
                                 }
                              }
                           }

                           double damage = ExplosionUtil.getDamageTo(entity, this.crystalDamageVec(pos), (Boolean)this.blockDestructionConfig.getValue(), (Integer)this.extrapolateTicksConfig.getValue(), (Boolean)this.assumeArmorConfig.getValue());
                           if (!this.checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                              DamageData<BlockPos> currentData = new DamageData<BlockPos>(pos, entity, damage, selfDamage, antiSurround);
                              validData.add(currentData);
                              if (data == null || damage > data.getDamage()) {
                                 data = currentData;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (data != null && !this.targetDamageCheck(data)) {
            return data;
         } else if ((Boolean)this.antiSurroundConfig.getValue()) {
            return (DamageData)validData.stream().filter(DamageData::isAntiSurround).min(Comparator.comparingDouble((d) -> mc.player.squaredDistanceTo(d.getBlockPos().toCenterPos()))).orElse(null);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private boolean placeRangeCheck(BlockPos pos) {
      double placeRange = (double)(Float)this.placeRangeConfig.getValue();
      double placeWallRange = (double)(Float)this.placeWallRangeConfig.getValue();
      Vec3d player = mc.player.getPos();
      double dist = player.squaredDistanceTo(pos.toCenterPos());
      if (dist > placeRange * placeRange) {
         return true;
      } else {
         Vec3d raytrace = Vec3d.of(pos).add((double)0.5F, 2.70000004768372, (double)0.5F);
         BlockHitResult result = mc.world.raycast(new net.minecraft.world.RaycastContext(mc.player.getEyePos(), raytrace, net.minecraft.world.RaycastContext.ShapeType.COLLIDER, net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
         float maxDist = (Float)this.breakRangeConfig.getValue() * (Float)this.breakRangeConfig.getValue();
         if (result != null && result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK && !result.getBlockPos().equals(pos)) {
            maxDist = (Float)this.breakWallRangeConfig.getValue() * (Float)this.breakWallRangeConfig.getValue();
            if (dist > placeWallRange * placeWallRange) {
               return true;
            }
         }

         return false;
      }
   }

   public void placeCrystalForTarget(PlayerEntity target, BlockPos blockPos) {
      if (target != null && !target.isDead() && !this.placeRangeCheck(blockPos) && this.canUseCrystalOnBlock(blockPos)) {
         double selfDamage = ExplosionUtil.getDamageTo(mc.player, this.crystalDamageVec(blockPos), (Boolean)this.blockDestructionConfig.getValue(), Set.of(blockPos), (Boolean)this.selfExtrapolateConfig.getValue() ? (Integer)this.extrapolateTicksConfig.getValue() : 0, false);
         if (!this.playerDamageCheck(selfDamage)) {
            double damage = ExplosionUtil.getDamageTo(target, this.crystalDamageVec(blockPos), (Boolean)this.blockDestructionConfig.getValue(), Set.of(blockPos), (Integer)this.extrapolateTicksConfig.getValue(), (Boolean)this.assumeArmorConfig.getValue());
            if ((!(damage < (double)(Float)this.minDamageConfig.getValue()) || this.isCrystalLethalTo(damage, target)) && (this.placeCrystal == null || !(this.placeCrystal.getDamage() >= damage))) {
               float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), blockPos.toCenterPos());
               this.setRotation(rotations[0], rotations[1]);
               this.placeCrystal(blockPos, Hand.MAIN_HAND, false);
               this.fadeList.put(blockPos, new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue()));
            }
         }
      }
   }

   private boolean checkOverrideSafety(boolean unsafeToPlayer, double damage, net.minecraft.entity.Entity entity) {
      return (Boolean)this.safetyOverride.getValue() && unsafeToPlayer && damage < (double)EntityUtil.getHealth(entity) + (double)0.5F;
   }

   private boolean targetDamageCheck(DamageData<?> crystal) {
      double minDmg = (double)(Float)this.minDamageConfig.getValue();
      net.minecraft.entity.Entity var5 = crystal.getAttackTarget();
      if (var5 instanceof LivingEntity entity) {
         if (this.isCrystalLethalTo(crystal, entity)) {
            minDmg = (double)2.0F;
         }
      }

      return crystal.getDamage() < minDmg;
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

   private boolean isFeetSurrounded(LivingEntity entity) {
      BlockPos pos1 = entity.getBlockPos();
      if (!mc.world.getBlockState(pos1).isReplaceable()) {
         return true;
      } else {
         for(Direction direction : Direction.values()) {
            if (direction.getAxis().isHorizontal()) {
               BlockPos pos2 = pos1.offset(direction);
               if (mc.world.getBlockState(pos2).isReplaceable()) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private boolean checkAntiTotem(double damage, LivingEntity entity) {
      if (entity instanceof PlayerEntity p) {
         float phealth = EntityUtil.getHealth(p);
         if (phealth <= 2.0F && (double)phealth - damage < (double)0.5F) {
            long time = Managers.TOTEM.getLastPopTime(p);
            if (time != -1L) {
               return System.currentTimeMillis() - time <= 500L;
            }
         }
      }

      return false;
   }

   private boolean isCrystalLethalTo(DamageData<?> crystal, LivingEntity entity) {
      return this.isCrystalLethalTo(crystal.getDamage(), entity);
   }

   private boolean isCrystalLethalTo(double damage, LivingEntity entity) {
      if ((Boolean)this.lethalDamageConfig.getValue() && this.lastAttackTimer.passed(500)) {
         return true;
      } else if ((Boolean)this.antiTotemConfig.getValue() && this.checkAntiTotem(damage, entity)) {
         return true;
      } else {
         float health = entity.getHealth() + entity.getAbsorptionAmount();
         if (damage * (double)(1.0F + (Float)this.lethalMultiplier.getValue()) >= (double)(health + 0.5F)) {
            return true;
         } else {
            if ((Boolean)this.armorBreakerConfig.getValue()) {
               for(ItemStack armorStack : entity.getArmorItems()) {
                  int n = armorStack.getDamage();
                  int n1 = armorStack.getMaxDamage();
                  float durability = (float)(n1 - n) / (float)n1 * 100.0F;
                  if (durability < (Float)this.armorScaleConfig.getValue()) {
                     return true;
                  }
               }
            }

            if ((Boolean)this.shulkersConfig.getValue() && entity instanceof PlayerEntity) {
               for(BlockPos pos : this.getSphere((double)3.0F, entity.getPos())) {
                  BlockState state = mc.world.getBlockState(pos);
                  if (state.getBlock() instanceof ShulkerBoxBlock) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private boolean attackCheckPre(Hand hand) {
      if (!this.lastSwapTimer.passed((Float)this.swapPenaltyConfig.getValue() * 25.0F)) {
         return true;
      } else {
         return hand == Hand.MAIN_HAND ? this.checkCanUseCrystal() : false;
      }
   }

   private boolean checkCanUseCrystal() {
      return !(Boolean)this.multitaskConfig.getValue() && this.checkMultitask() || !(Boolean)this.whileMiningConfig.getValue() && mc.interactionManager.isBreakingBlock();
   }

   private boolean isHoldingCrystal() {
      if (this.checkCanUseCrystal() || this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.SILENT && this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.SILENT_ALT) {
         return this.getCrystalHand() != null;
      } else {
         return true;
      }
   }

   private Vec3d crystalDamageVec(BlockPos pos) {
      return Vec3d.of(pos).add((double)0.5F, (double)1.0F, (double)0.5F);
   }

   private boolean isValidTarget(net.minecraft.entity.Entity e) {
      return e instanceof PlayerEntity && (Boolean)this.playersConfig.getValue() || EntityUtil.isMonster(e) && (Boolean)this.monstersConfig.getValue() || EntityUtil.isNeutral(e) && (Boolean)this.neutralsConfig.getValue() || EntityUtil.isPassive(e) && (Boolean)this.animalsConfig.getValue();
   }

   public boolean canUseCrystalOnBlock(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return !state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK) ? false : this.isCrystalHitboxClear(pos);
   }

   public boolean isCrystalHitboxClear(BlockPos pos) {
      BlockPos p2 = pos.up();
      BlockState state2 = mc.world.getBlockState(p2);
      if (this.placementsConfig.getValue() == AutoCrystalModule.Placements.PROTOCOL && !mc.world.isAir(p2.up())) {
         return false;
      } else if (!mc.world.isAir(p2) && !state2.isOf(Blocks.FIRE)) {
         return false;
      } else {
         Box bb = Managers.NETWORK.isCrystalPvpCC() ? HALF_CRYSTAL_BB : FULL_CRYSTAL_BB;
         double d = (double)p2.getX();
         double e = (double)p2.getY();
         double f = (double)p2.getZ();
         List<net.minecraft.entity.Entity> list = this.getEntitiesBlockingCrystal(new Box(d, e, f, d + bb.maxX, e + bb.maxY, f + bb.maxZ));
         return list.isEmpty();
      }
   }

   private List<net.minecraft.entity.Entity> getEntitiesBlockingCrystal(Box box) {
      List<net.minecraft.entity.Entity> entities = new CopyOnWriteArrayList(mc.world.getOtherEntities((net.minecraft.entity.Entity)null, box));

      for(net.minecraft.entity.Entity entity : entities) {
         if (entity != null && entity.isAlive() && !(entity instanceof ExperienceOrbEntity) && (!(Boolean)this.forcePlaceEnabledConfig.getValue() || !(entity instanceof ItemEntity) || entity.age > 10)) {
            if (entity instanceof EndCrystalEntity) {
               EndCrystalEntity entity1 = (EndCrystalEntity)entity;
               if (entity1.getBoundingBox().intersects(box)) {
                  Integer antiStuckAttacks = (Integer)this.antiStuckCrystals.get(entity1.getId());
                  if (this.attackRangeCheck(entity1) || antiStuckAttacks != null && !((float)antiStuckAttacks <= (Float)this.attackLimitConfig.getValue() * 10.0F)) {
                     double dist = mc.player.squaredDistanceTo(entity1);
                     this.stuckCrystals.add(new AntiStuckData(entity1.getId(), entity1.getBlockPos(), entity1.getPos(), dist));
                  } else {
                     entities.remove(entity);
                  }
               }
            }
         } else {
            entities.remove(entity);
         }
      }

      return entities;
   }

   private boolean intersectingAntiStuckCheck(BlockPos blockPos) {
      return this.stuckCrystals.isEmpty() ? false : this.stuckCrystals.stream().anyMatch((d) -> d.blockPos().equals(blockPos.up()));
   }

   private EndCrystalEntity intersectingCrystalCheck(BlockPos pos) {
      return (EndCrystalEntity)mc.world.getOtherEntities((net.minecraft.entity.Entity)null, new Box(pos)).stream().filter((e) -> e instanceof EndCrystalEntity).min(Comparator.comparingDouble((e) -> (double)mc.player.distanceTo(e))).orElse(null);
   }

   private int[][] getSphereOffsets(int rad) {
      return (int[][])this.sphereOffsetCache.computeIfAbsent(rad, (r) -> {
         List<int[]> offsets = new ArrayList<>();

         for(int x = -r; x <= r; ++x) {
            for(int y = -r; y <= r; ++y) {
               for(int z = -r; z <= r; ++z) {
                  if (x * x + y * y + z * z <= r * r) {
                     offsets.add(new int[]{x, y, z});
                  }
               }
            }
         }

         return (int[][])offsets.toArray(new int[0][]);
      });
   }

   private List<BlockPos> getSphere(Vec3d origin) {
      double rad = Math.ceil((double)(Float)this.placeRangeConfig.getValue());
      return this.getSphere(rad, origin);
   }

   private List<BlockPos> getSphere(double rad, Vec3d origin) {
      int r = (int)rad;
      int[][] offsets = this.getSphereOffsets(r);
      int ox = (int)Math.floor(origin.getX());
      int oy = (int)Math.floor(origin.getY());
      int oz = (int)Math.floor(origin.getZ());
      List<BlockPos> sphere = new ArrayList<>(offsets.length);

      for(int[] off : offsets) {
         sphere.add(new BlockPos(ox + off[0], oy + off[1], oz + off[2]));
      }

      return sphere;
   }

   private boolean canHoldCrystal() {
      return this.isHoldingCrystal() || this.autoSwapConfig.getValue() != AutoCrystalModule.Swap.OFF && this.getCrystalSlot() != -1;
   }

   private Hand getCrystalHand() {
      ItemStack offhand = mc.player.getOffHandStack();
      ItemStack mainhand = mc.player.getMainHandStack();
      if (offhand.getItem() instanceof EndCrystalItem) {
         return Hand.OFF_HAND;
      } else {
         return mainhand.getItem() instanceof EndCrystalItem ? Hand.MAIN_HAND : null;
      }
   }

   public float getBreakDelay() {
      return Math.max(0.0F, (Float)this.breakDelayFloatConfig.getValue());
   }

   public void setStage(String crystalStage) {
   }

   public int getBreakMs() {
      if (this.attackLatency.isEmpty()) {
         return 0;
      } else {
         float avg = 0.0F;
         ArrayList<Long> latencyCopy = Lists.newArrayList(this.attackLatency);
         if (!latencyCopy.isEmpty()) {
            float t;
            for(Iterator var3 = latencyCopy.iterator(); var3.hasNext(); avg += t) {
               t = (float)(Long)var3.next();
            }

            avg /= (float)latencyCopy.size();
         }

         return (int)avg;
      }
   }

   public boolean shouldPreForcePlace() {
      return (Boolean)this.forcePlaceEnabledConfig.getValue();
   }

   public float getPlaceRange() {
      return (Float)this.placeRangeConfig.getValue();
   }

   public static enum Page {
      PLACE,
      BREAK,
      ROTATE,
      MISC,
      CALC,
      RENDER,
      BASE;

      // $FF: synthetic method
      private static Page[] $values() {
         return new Page[]{PLACE, BREAK, ROTATE, MISC, CALC, RENDER, BASE};
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

   public static enum SwapAction {
      Hotbar,
      Screen;

      // $FF: synthetic method
      private static SwapAction[] $values() {
         return new SwapAction[]{Hotbar, Screen};
      }
   }

   public static enum SwingMode {
      Normal,
      Packet,
      None;

      // $FF: synthetic method
      private static SwingMode[] $values() {
         return new SwingMode[]{Normal, Packet, None};
      }
   }

   public static enum ShieldMode {
      Bypass,
      Disable,
      None;

      // $FF: synthetic method
      private static ShieldMode[] $values() {
         return new ShieldMode[]{Bypass, Disable, None};
      }
   }

   public static enum RenderMode {
      SLIDER,
      STATIC,
      NONE;

      // $FF: synthetic method
      private static RenderMode[] $values() {
         return new RenderMode[]{SLIDER, STATIC, NONE};
      }
   }

   public static enum BoxMode {
      Both,
      Outline,
      Fill;

      // $FF: synthetic method
      private static BoxMode[] $values() {
         return new BoxMode[]{Both, Outline, Fill};
      }
   }

   public static enum Sequential {
      NORMAL,
      STRICT,
      NONE;

      // $FF: synthetic method
      private static Sequential[] $values() {
         return new Sequential[]{NORMAL, STRICT, NONE};
      }
   }

   public static enum ForcePlace {
      PRE,
      POST,
      NONE;

      // $FF: synthetic method
      private static ForcePlace[] $values() {
         return new ForcePlace[]{PRE, POST, NONE};
      }
   }

   public static enum Placements {
      NATIVE,
      PROTOCOL;

      // $FF: synthetic method
      private static Placements[] $values() {
         return new Placements[]{NATIVE, PROTOCOL};
      }
   }

   public static enum Rotate {
      FULL,
      SEMI,
      OFF;

      // $FF: synthetic method
      private static Rotate[] $values() {
         return new Rotate[]{FULL, SEMI, OFF};
      }
   }

   private static record AntiStuckData(int id, BlockPos blockPos, Vec3d pos, double stuckDist) {
   }

   private static class DamageData<T> {
      private final List<String> tags = new ArrayList<>();
      private T damageData;
      private net.minecraft.entity.Entity attackTarget;
      private BlockPos blockPos;
      private double damage;
      private double selfDamage;
      private boolean antiSurround;

      public DamageData() {
      }

      public DamageData(BlockPos damageData, net.minecraft.entity.Entity attackTarget, double damage, double selfDamage, boolean antiSurround) {
         this.damageData = (T)(Object)damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
         this.blockPos = damageData;
         this.antiSurround = antiSurround;
      }

      public DamageData(T damageData, net.minecraft.entity.Entity attackTarget, double damage, double selfDamage, BlockPos blockPos, boolean antiSurround) {
         this.damageData = (T)(Object)damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
         this.blockPos = blockPos;
         this.antiSurround = antiSurround;
      }

      public void setDamageData(T damageData, net.minecraft.entity.Entity attackTarget, double damage, double selfDamage) {
         this.damageData = (T)(Object)damageData;
         this.attackTarget = attackTarget;
         this.damage = damage;
         this.selfDamage = selfDamage;
      }

      public T getDamageData() {
         return this.damageData;
      }

      public net.minecraft.entity.Entity getAttackTarget() {
         return this.attackTarget;
      }

      public double getDamage() {
         return this.damage;
      }

      public double getSelfDamage() {
         return this.selfDamage;
      }

      public BlockPos getBlockPos() {
         return this.blockPos;
      }

      public boolean isAntiSurround() {
         return this.antiSurround;
      }
   }

   private class AttackCrystalTask implements Callable<DamageData<EndCrystalEntity>> {
      private final List<net.minecraft.entity.Entity> threadSafeEntities;
      private final List<net.minecraft.entity.Entity> threadSafeValidTargets;

      public AttackCrystalTask(List<net.minecraft.entity.Entity> threadSafeEntities, List<net.minecraft.entity.Entity> threadSafeValidTargets) {
         this.threadSafeEntities = threadSafeEntities;
         this.threadSafeValidTargets = threadSafeValidTargets;
      }

      public DamageData<EndCrystalEntity> call() throws Exception {
         return AutoCrystalModule.this.calculateAttackCrystal(this.threadSafeEntities, this.threadSafeValidTargets);
      }
   }

   private class PlaceCrystalTask implements Callable<DamageData<BlockPos>> {
      private final List<BlockPos> threadSafeBlocks;
      private final List<net.minecraft.entity.Entity> threadSafeValidTargets;

      public PlaceCrystalTask(List<BlockPos> threadSafeBlocks, List<net.minecraft.entity.Entity> threadSafeValidTargets) {
         this.threadSafeBlocks = threadSafeBlocks;
         this.threadSafeValidTargets = threadSafeValidTargets;
      }

      public DamageData<BlockPos> call() throws Exception {
         return AutoCrystalModule.this.calculatePlaceCrystal(this.threadSafeBlocks, this.threadSafeValidTargets);
      }
   }
}
