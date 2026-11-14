package net.hachimi.client.impl.module.client;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.ConfigContainer;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.Module;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.ScreenOpenEvent;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.entity.StatusEffectEvent;
import net.hachimi.client.impl.event.gui.hud.RenderOverlayEvent;
import net.hachimi.client.impl.event.gui.screen.RenderOpenChatEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.exploit.FastLatencyModule;
import net.hachimi.client.impl.module.misc.TimerModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.StreamUtils;
import net.hachimi.client.util.math.PerSecondCounter;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.client.util.render.ColorUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.render.animation.Easing;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.client.util.world.BlockUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.util.Window;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.world.World;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.math.Direction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.StringHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.math.Direction.AxisDirection;

public class HUDModule extends ToggleModule {
   private static HUDModule INSTANCE;
   Config<Boolean> watermarkConfig = this.register(new BooleanConfig("Watermark", "Displays client name and version watermark", true));
   Config<Boolean> logoConfig = this.register(new BooleanConfig("Logo", "Displays the Hachimi logo in the top left corner", false));
   Config<Float> logoScaleConfig = this.register(new NumberConfig("Logo-Scale", "The scale of the logo image", 0.1F, 1.6F, 2.0F, () -> this.logoConfig.getValue()));
   Config<Integer> logoOpacityConfig = this.register(new NumberConfig("Logo-Opacity", "The opacity of the logo image", 0, 255, 255, () -> this.logoConfig.getValue()));
   Config<Boolean> serverStatusConfig = this.register(new BooleanConfig("ServerStatus", "Displays warning for server connection status", true));
   Config<Boolean> userInfo = this.register(new BooleanConfig("UserInfo", "Displays your user information", true));
   Config<Boolean> directionConfig = this.register(new BooleanConfig("Direction", "Displays facing direction", true));
   Config<Boolean> rotationConfig = this.register(new BooleanConfig("Rotation", "Displays player yaw and pitch", false, () -> this.directionConfig.getValue()));
   Config<Boolean> armorConfig = this.register(new BooleanConfig("Armor", "Displays player equipped armor and durability", true));
   Config<Boolean> armorDurabilityConfig = this.register(new BooleanConfig("ArmorDurability", "Displays player equipped armor durability", false, () -> this.armorConfig.getValue()));
   Config<Boolean> totemsConfig = this.register(new BooleanConfig("Totems", "Displays player totems", false));
   Config<VanillaHud> potionHudConfig;
   Config<VanillaHud> itemNameConfig;
   Config<Boolean> potionEffectsConfig;
   Config<PotionColors> potionColorsConfig;
   Config<Boolean> potionTimerConfig;
   Config<Boolean> potionTimerResLvlConfig;
   Config<Integer> potionTimerYOffsetConfig;
   Config<Boolean> durabilityConfig;
   Config<Boolean> coordsConfig;
   Config<Boolean> netherCoordsConfig;
   Config<Boolean> serverBrandConfig;
   Config<Boolean> chestsConfig;
   Config<SpeedHud> speedConfig;
   Config<Boolean> pingConfig;
   Config<Boolean> tpsConfig;
   Config<Boolean> packetsConfig;
   Config<Boolean> fpsConfig;
   Config<Boolean> arraylistConfig;
   Config<Integer> animTimeConfig;
   Config<Ordering> orderingConfig;
   Config<Rendering> renderingConfig;
   Config<RainbowMode> rainbowModeConfig;
   Config<Color> gradientColorConfig;
   Config<Float> rainbowSpeedConfig;
   Config<Integer> rainbowSaturationConfig;
   Config<Integer> rainbowBrightnessConfig;
   Config<Float> rainbowDifferenceConfig;
   private static final Identifier LOGO_TEXTURE = Identifier.of("hachimiclient", "textures/white.png");
   private static final int LOGO_WIDTH = 1057;
   private static final int LOGO_HEIGHT = 384;
   private final DecimalFormat decimal;
   private final DecimalFormat decimal2;
   private long rainbowOffset;
   private float topLeft;
   private float topRight;
   private float bottomLeft;
   private float bottomRight;
   private boolean renderingUp;
   private final Animation chatOpenAnimation;
   private final PerSecondCounter fpsCounter;
   private final Map<String, HudRenderModule> hudRenderModules;
   private final Map<PotionData, Animation> hudRenderPotions;
   private final Timer serverStatus;
   private final Animation statusAnimation;

   public HUDModule() {
      super("HUD", "Displays the HUD (heads up display) screen.", ModuleCategory.CLIENT);
      this.potionHudConfig = this.register(new EnumConfig("PotionHud", "Renders the Minecraft potion Hud", HUDModule.VanillaHud.HIDE, HUDModule.VanillaHud.values()));
      this.itemNameConfig = this.register(new EnumConfig("ItemName", "Renders the Minecraft item name display", HUDModule.VanillaHud.HIDE, HUDModule.VanillaHud.values()));
      this.potionEffectsConfig = this.register(new BooleanConfig("PotionEffects", "Displays active potion effects", true));
      this.potionColorsConfig = this.register(new EnumConfig("PotionColors", "Displays active potion colors", HUDModule.PotionColors.NORMAL, HUDModule.PotionColors.values(), () -> this.potionEffectsConfig.getValue()));
      this.potionTimerConfig = this.register(new BooleanConfig("PotionTimer", "Shows remaining seconds of effects + potion count (Alien-style)", true));
      this.potionTimerResLvlConfig = this.register(new BooleanConfig("ResistanceLevelCheck", "Only show Resistance timer if level II+", true, () -> this.potionTimerConfig.getValue()));
      this.potionTimerYOffsetConfig = this.register(new NumberConfig("PotionTimerY", "Y offset from center (positive = up)", -200, 40, 200, () -> this.potionTimerConfig.getValue()));
      this.durabilityConfig = this.register(new BooleanConfig("Durability", "Displays the current held items durability", false));
      this.coordsConfig = this.register(new BooleanConfig("Coords", "Displays world coordinates", true));
      this.netherCoordsConfig = this.register(new BooleanConfig("NetherCoords", "Displays nether coordinates", true, () -> this.coordsConfig.getValue()));
      this.serverBrandConfig = this.register(new BooleanConfig("ServerBrand", "Displays the current server brand", false));
      this.chestsConfig = this.register(new BooleanConfig("Chests", "Displays the amount of chests in your render distance", false));
      this.speedConfig = this.register(new EnumConfig("Speed", "Displays the current movement speed of the player", HUDModule.SpeedHud.K_M_H, HUDModule.SpeedHud.values()));
      this.pingConfig = this.register(new BooleanConfig("Ping", "Display server response time in ms", true));
      this.tpsConfig = this.register(new BooleanConfig("TPS", "Displays server ticks per second", true));
      this.packetsConfig = this.register(new BooleanConfig("Packets", "Displays outbound packets per second", false));
      this.fpsConfig = this.register(new BooleanConfig("FPS", "Displays game FPS", true));
      this.arraylistConfig = this.register(new BooleanConfig("Arraylist", "Displays a list of all active modules", true));
      this.animTimeConfig = this.register(new NumberConfig("Arraylist-Time", "Timer for the animation", 0, 1000, 2000, () -> false));
      this.orderingConfig = this.register(new EnumConfig("Ordering", "The ordering of the arraylist", HUDModule.Ordering.LENGTH, HUDModule.Ordering.values(), () -> this.arraylistConfig.getValue()));
      this.renderingConfig = this.register(new EnumConfig("Rendering", "The rendering mode of the HUD", HUDModule.Rendering.UP, HUDModule.Rendering.values()));
      this.rainbowModeConfig = this.register(new EnumConfig("Rainbow", "The rendering mode for rainbow", HUDModule.RainbowMode.OFF, HUDModule.RainbowMode.values()));
      this.gradientColorConfig = this.register(new ColorConfig("GradientColor", "The color of the rainbow gradient", Color.WHITE, false, false, () -> this.rainbowModeConfig.getValue() == HUDModule.RainbowMode.GRADIENT));
      this.rainbowSpeedConfig = this.register(new NumberConfig("Rainbow-Speed", "The speed for the rainbow color cycling", 0.1F, 50.0F, 100.0F, () -> this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.OFF));
      this.rainbowSaturationConfig = this.register(new NumberConfig("Rainbow-Saturation", "The saturation of rainbow colors", 0, 35, 100, () -> this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.OFF && this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.GRADIENT));
      this.rainbowBrightnessConfig = this.register(new NumberConfig("Rainbow-Brightness", "The brightness of rainbow colors", 0, 100, 100, () -> this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.OFF && this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.GRADIENT));
      this.rainbowDifferenceConfig = this.register(new NumberConfig("Rainbow-Difference", "The difference offset for rainbow colors", 0.1F, 40.0F, 100.0F, () -> this.rainbowModeConfig.getValue() != HUDModule.RainbowMode.OFF));
      this.decimal = new DecimalFormat("0.0");
      this.decimal2 = new DecimalFormat("0.0#");
      this.chatOpenAnimation = new Animation(false, 200.0F, Easing.LINEAR);
      this.fpsCounter = new PerSecondCounter();
      this.hudRenderModules = new HashMap<>();
      this.hudRenderPotions = new ConcurrentSkipListMap();
      this.serverStatus = new CacheTimer();
      this.statusAnimation = new Animation(false, 300.0F, Easing.LINEAR);
      INSTANCE = this;
   }

   public static HUDModule getInstance() {
      return INSTANCE;
   }

   private void arrayListRenderModule(RenderOverlayEvent.Post event, ToggleModule toggleModule, long drawnCount) {
      if (!(toggleModule.getAnimation().getFactor() <= (double)0.01F) && !toggleModule.isHidden()) {
         HudRenderModule hudRender = (HudRenderModule)this.hudRenderModules.get(toggleModule.getId());
         if (hudRender != null) {
            hudRender.draw(event.getContext(), drawnCount);
         }

      }
   }

   @EventListener
   public void onDeath(EntityDeathEvent event) {
      if (event.getEntity() == mc.player) {
         this.hudRenderPotions.clear();
      }

   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.hudRenderPotions.clear();
   }

   @EventListener
   public void onRenderOpenChat(RenderOpenChatEvent event) {
      event.cancel();
      event.setAnimation((float)this.chatOpenAnimation.getFactor());
   }

   @EventListener
   public void onRenderOverlayPost(RenderOverlayEvent.Post event) {
      if (this.hudRenderModules.isEmpty()) {
         for(Module module : Managers.MODULE.getModules()) {
            if (module instanceof ToggleModule) {
               ToggleModule toggleModule = (ToggleModule)module;
               this.hudRenderModules.put(module.getId(), new HudRenderModule(toggleModule));
            }
         }
      }

      int updates = MathHelper.clamp(Math.round(240.0F / (float)mc.getCurrentFps()), 1, 10);

      for(int i = 0; i < updates; ++i) {
         for(HudRenderModule hudRenderModule : this.hudRenderModules.values()) {
            hudRenderModule.updateAnimation();
         }
      }

      this.fpsCounter.updateCounter();
      if (mc.player != null && mc.world != null) {
         for(Map.Entry<PotionData, Animation> entry : this.hudRenderPotions.entrySet()) {
            PotionData effectInstance = (PotionData)entry.getKey();
            Animation animation = (Animation)entry.getValue();
            effectInstance.update();
            if (mc.player.getStatusEffect(effectInstance.getType()) == null && animation.getFactor() < (double)0.01F) {
               this.hudRenderPotions.remove(effectInstance);
            }
         }

         if (mc.options.hudHidden) {
            return;
         }

         Window res = mc.getWindow();
         if ((Boolean)this.serverStatusConfig.getValue()) {
            this.statusAnimation.setState(this.serverStatus.passed(1000));
            String warning = String.format("§fServer not responding §7(§r%s.s§7)", this.decimal.format((double)this.serverStatus.getElapsedTime() / (double)1000.0F));
            int width = RenderManager.textWidth(warning);
            Color color = ColorUtil.interpolateColor(MathHelper.clamp(((float)this.serverStatus.getElapsedTime() - 1000.0F) / 5000.0F, 0.0F, 1.0F), Color.RED, Color.GREEN);
            int alpha = (int)((double)255.0F * this.statusAnimation.getFactor());
            RenderManager.renderText(event.getContext(), warning, (float)res.getScaledWidth() / 2.0F - (float)width / 2.0F, 4.0F, ColorUtil.fixTransparency(color.getRGB(), (float)alpha));
         }

         this.rainbowOffset = 0L;
         this.topLeft = 2.0F;
         this.topRight = this.topLeft;
         this.bottomLeft = (float)res.getScaledHeight() - 11.0F;
         this.bottomRight = this.bottomLeft;
         this.renderingUp = this.renderingConfig.getValue() == HUDModule.Rendering.UP;
         this.bottomLeft -= (float)((double)14.0F * this.chatOpenAnimation.getFactor());
         this.bottomRight -= (float)((double)14.0F * this.chatOpenAnimation.getFactor());
         if (this.potionHudConfig.getValue() == HUDModule.VanillaHud.MOVE && !mc.player.getStatusEffects().isEmpty()) {
            this.topRight += 27.0F;
         }

         List<Module> modules = Managers.MODULE.getModules();
         Stream var10000 = modules.stream();
         Objects.requireNonNull(ToggleModule.class);
         var10000 = var10000.filter(ToggleModule.class::isInstance);
         Objects.requireNonNull(ToggleModule.class);
         long drawnCount = var10000.map(ToggleModule.class::cast).filter((m) -> !((ToggleModule)m).isHidden()).count();
         if ((Boolean)this.logoConfig.getValue()) {
            float scale = (Float)this.logoScaleConfig.getValue();
            float width = 1057.0F * scale;
            float height = 384.0F * scale;
            float alpha = (float)(Integer)this.logoOpacityConfig.getValue() / 255.0F;
            int logoColor = this.getHudColor(drawnCount - this.rainbowOffset);
            float logoRed = (float)(logoColor >> 16 & 255) / 255.0F;
            float logoGreen = (float)(logoColor >> 8 & 255) / 255.0F;
            float logoBlue = (float)(logoColor & 255) / 255.0F;
            RenderManager.rectTextured(event.getContext().getMatrices(), LOGO_TEXTURE, 2.0F, 2.0F + width, this.topLeft, this.topLeft + height, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, logoRed, logoGreen, logoBlue, alpha);
            this.topLeft += height + 2.0F;
         }

         if ((Boolean)this.watermarkConfig.getValue()) {
            RenderManager.renderText(event.getContext(), String.format("%s %s", "Hachimi", "2.0"), 2.0F, this.topLeft, this.getHudColor(drawnCount - this.rainbowOffset));
            this.topLeft += (float)RenderManager.textHeight();
         }

         if ((Boolean)this.userInfo.getValue()) {
            RenderManager.renderText(event.getContext(), String.format("UID %s", "1"), 2.0F, this.topLeft, this.getHudColor(drawnCount - this.rainbowOffset));
            this.topLeft += (float)RenderManager.textHeight();
         }

         if ((Boolean)this.arraylistConfig.getValue()) {
            var10000 = modules.stream();
            Objects.requireNonNull(ToggleModule.class);
            var10000 = var10000.filter(ToggleModule.class::isInstance);
            Objects.requireNonNull(ToggleModule.class);
            Stream<ToggleModule> moduleStream = var10000.map(ToggleModule.class::cast);
            switch (((Ordering)this.orderingConfig.getValue()).ordinal()) {
               case 0 -> var10000 = StreamUtils.sortCached(moduleStream, (m) -> -RenderManager.textWidth(this.getFormattedModule(m)));
               case 1 -> var10000 = StreamUtils.sortCached(moduleStream, ConfigContainer::getName);
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            moduleStream = var10000;
            moduleStream.forEach((t) -> this.arrayListRenderModule(event, t, drawnCount));
         }

         if ((Boolean)this.coordsConfig.getValue()) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            boolean nether = mc.world.getRegistryKey() == World.NETHER;
            RenderManager.renderText(event.getContext(), String.format("XYZ §f%s, %s, %s " + ((Boolean)this.netherCoordsConfig.getValue() ? "§7[§f%s, %s§7]" : ""), this.decimal.format(x), this.decimal.format(y), this.decimal.format(z), nether ? this.decimal.format(x * (double)8.0F) : this.decimal.format(x / (double)8.0F), nether ? this.decimal.format(z * (double)8.0F) : this.decimal.format(z / (double)8.0F)), 2.0F, this.bottomLeft, this.getHudColor(this.rainbowOffset));
            this.bottomLeft -= (float)RenderManager.textHeight();
         }

         if ((Boolean)this.directionConfig.getValue()) {
            net.minecraft.util.math.Direction direction = mc.player.getHorizontalFacing();
            String dir = EnumFormatter.formatDirection(direction);
            String axis = EnumFormatter.formatAxis(direction.getAxis());
            boolean pos = direction.getDirection() == net.minecraft.util.math.Direction.AxisDirection.POSITIVE;
            String rotationText = String.format(", %s", (int)MathHelper.wrapDegrees(Managers.ROTATION.getServerYaw()));
            RenderManager.renderText(event.getContext(), String.format("%s §7[§f%s%s%s§7]", dir, axis, pos ? "+" : "-", (Boolean)this.rotationConfig.getValue() ? rotationText : ""), 2.0F, this.bottomLeft, this.getHudColor(this.rainbowOffset));
         }

         if ((Boolean)this.potionEffectsConfig.getValue()) {
            for(Map.Entry<PotionData, Animation> e1 : this.hudRenderPotions.entrySet()) {
               PotionData e = (PotionData)e1.getKey();
               Animation animation = (Animation)e1.getValue();
               StatusEffect effect = (StatusEffect)e.getType().value();
               if (effect != StatusEffects.NIGHT_VISION) {
                  boolean infinite = e.getDuration() == -1;
                  boolean amplifier = e.getAmplifier() + 1 > 1 && !infinite;
                  String duration;
                  if (infinite) {
                     duration = "Inf";
                  } else {
                     int i = MathHelper.floor((float)e.getDuration());
                     duration = StringHelper.formatTicks(i, mc.world.getTickManager().getTickRate());
                  }

                  String text = String.format("%s %s§f%s", effect.getName().getString(), amplifier ? e.getAmplifier() + 1 + " " : "", duration);
                  int width = RenderManager.textWidth(text);
                  float x = ((float)width + 1.0F) * (float)animation.getFactor();
                  int var123;
                  switch (((PotionColors)this.potionColorsConfig.getValue()).ordinal()) {
                     case 0 -> var123 = ColorUtil.withAlpha(effect.getColor(), (int)((double)255.0F * animation.getFactor()));
                     case 1 -> var123 = ColorUtil.withAlpha(this.getLiquidColor(e.getType().getIdAsString(), effect), (int)((double)255.0F * animation.getFactor()));
                     case 2 -> var123 = ColorUtil.withAlpha(this.getHudColor(this.rainbowOffset), (int)((double)255.0F * animation.getFactor()));
                     default -> throw new MatchException((String)null, (Throwable)null);
                  }

                  int potionColor = var123;
                  RenderManager.renderText(event.getContext(), text, (float)res.getScaledWidth() - x, this.renderingUp ? this.bottomRight : this.topRight, ColorUtil.fixTransparency(potionColor, (float)animation.getFactor()));
                  if (this.renderingUp) {
                     this.bottomRight -= (float)RenderManager.textHeight();
                  } else {
                     this.topRight += (float)RenderManager.textHeight();
                  }

                  ++this.rainbowOffset;
               }
            }
         }

         if ((Boolean)this.serverBrandConfig.getValue()) {
            String brand = mc.player.networkHandler.getBrand();
            int width = RenderManager.textWidth(brand);
            RenderManager.renderText(event.getContext(), brand, (float)(res.getScaledWidth() - width) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, this.getHudColor(this.rainbowOffset));
            if (this.renderingUp) {
               this.bottomRight -= (float)RenderManager.textHeight();
            } else {
               this.topRight += (float)RenderManager.textHeight();
            }

            ++this.rainbowOffset;
         }

         if ((Boolean)this.potionTimerConfig.getValue()) {
            StringBuilder sb = new StringBuilder();
            int resCount = 0;

            for(int i = 0; i < 36; ++i) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (stack.getItem() == Items.SPLASH_POTION) {
                  PotionContentsComponent contents = (PotionContentsComponent)stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

                  for(StatusEffectInstance fx : contents.getEffects()) {
                     if (fx.getEffectType().equals(StatusEffects.RESISTANCE)) {
                        ++resCount;
                        break;
                     }
                  }
               }
            }

            if (resCount > 0) {
               sb.append("§e").append(resCount);
            }

            if (mc.player.hasStatusEffect(StatusEffects.RESISTANCE)) {
               StatusEffectInstance fx = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
               boolean levelOk = !(Boolean)this.potionTimerResLvlConfig.getValue() || fx.getAmplifier() > 0;
               if (levelOk) {
                  if (!sb.isEmpty()) {
                     sb.append(" ");
                  }

                  sb.append("§9").append(fx.getDuration() / 20 + 1);
               }
            }

            if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
               if (!sb.isEmpty()) {
                  sb.append(" ");
               }

               sb.append("§4").append(mc.player.getStatusEffect(StatusEffects.STRENGTH).getDuration() / 20 + 1);
            }

            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
               if (!sb.isEmpty()) {
                  sb.append(" ");
               }

               sb.append("§b").append(mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() / 20 + 1);
            }

            if (!sb.isEmpty()) {
               String txt = sb.toString();
               int sw = res.getScaledWidth();
               int sh = res.getScaledHeight();
               int tx = sw / 2 - mc.textRenderer.getWidth(txt) / 2;
               int var124 = sh / 2;
               Objects.requireNonNull(mc.textRenderer);
               int ty = var124 + 9 - (Integer)this.potionTimerYOffsetConfig.getValue();
               event.getContext().drawTextWithShadow(mc.textRenderer, txt, tx, ty, -1);
            }
         }

         if (this.speedConfig.getValue() != HUDModule.SpeedHud.OFF) {
            double x = mc.player.getX() - mc.player.prevX;
            double z = mc.player.getZ() - mc.player.prevZ;
            float timer = TimerModule.getInstance().isEnabled() ? TimerModule.getInstance().getTimer() : 1.0F;
            double speed;
            if (this.speedConfig.getValue() == HUDModule.SpeedHud.K_M_H) {
               double dist = Math.sqrt(x * x + z * z) / (double)1000.0F;
               double div = 1.388888888888889E-5;
               speed = dist / div * (double)timer;
            } else {
               x *= (double)20.0F;
               z *= (double)20.0F;
               double dist = Math.sqrt(x * x + z * z);
               speed = Math.abs(dist) * (double)timer;
            }

            String text = String.format("Speed §f%s%s", this.decimal2.format(speed), this.speedConfig.getValue() == HUDModule.SpeedHud.K_M_H ? "km/h" : "b/s");
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(event.getContext(), text, (float)(res.getScaledWidth() - width) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, this.getHudColor(this.rainbowOffset));
            if (this.renderingUp) {
               this.bottomRight -= (float)RenderManager.textHeight();
            } else {
               this.topRight += (float)RenderManager.textHeight();
            }

            ++this.rainbowOffset;
         }

         if ((Boolean)this.durabilityConfig.getValue() && mc.player.getMainHandStack().isDamageable()) {
            int n = mc.player.getMainHandStack().getMaxDamage();
            int n2 = mc.player.getMainHandStack().getDamage();
            String text1 = "Durability ";
            String text2 = String.valueOf(n - n2);
            int width = RenderManager.textWidth(text1);
            int width2 = RenderManager.textWidth(text2);
            Color color = ColorUtil.hslToColor((float)(n - n2) / (float)n * 120.0F, 100.0F, 50.0F, 1.0F);
            RenderManager.renderText(event.getContext(), text1, (float)(res.getScaledWidth() - width - width2) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, this.getHudColor(this.rainbowOffset));
            RenderManager.renderText(event.getContext(), text2, (float)(res.getScaledWidth() - width2) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, color.getRGB());
            if (this.renderingUp) {
               this.bottomRight -= (float)RenderManager.textHeight();
            } else {
               this.topRight += (float)RenderManager.textHeight();
            }

            ++this.rainbowOffset;
         }

         if ((Boolean)this.pingConfig.getValue() && !mc.isInSingleplayer()) {
            int latency = FastLatencyModule.getInstance().isEnabled() ? (int)FastLatencyModule.getInstance().getLatency() : Managers.NETWORK.getClientLatency();
            String text = String.format("Ping §f%dms", latency);
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(event.getContext(), text, (float)(res.getScaledWidth() - width) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, this.getHudColor(this.rainbowOffset));
            if (this.renderingUp) {
               this.bottomRight -= (float)RenderManager.textHeight();
            } else {
               this.topRight += (float)RenderManager.textHeight();
            }

            ++this.rainbowOffset;
         }

         if ((Boolean)this.packetsConfig.getValue()) {
            String text = String.format("Packets §f%s<-%s", Managers.NETWORK.getOutgoingPPS(), Managers.NETWORK.getIncomingPPS());
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(event.getContext(), text, (float)(res.getScaledWidth() - width) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, this.getHudColor(this.rainbowOffset));
            if (this.renderingUp) {
               this.bottomRight -= (float)RenderManager.textHeight();
            } else {
               this.topRight += (float)RenderManager.textHeight();
            }

            ++this.rainbowOffset;
         }

         if ((Boolean)this.tpsConfig.getValue()) {
            float curr = Managers.TICK.getTpsCurrent();
            float avg = Managers.TICK.getTpsAverage();
            Object[] var10001 = new Object[2];
            String var10004 = this.decimal2.format((double)curr);
            var10001[0] = var10004 + (Managers.TICK.isTicksFilled() ? "" : "*");
            var10001[1] = this.decimal2.format((double)avg);
            String text = String.format("TPS §f%s §7[§f%s§7]", var10001);
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(event.getContext(), text, (float)(res.getScaledWidth() - width) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, this.getHudColor(this.rainbowOffset));
            if (this.renderingUp) {
               this.bottomRight -= (float)RenderManager.textHeight();
            } else {
               this.topRight += (float)RenderManager.textHeight();
            }

            ++this.rainbowOffset;
         }

         if ((Boolean)this.chestsConfig.getValue()) {
            int singleChests = 0;
            int doubleChests = 0;

            for(BlockEntity blockEntity : BlockUtil.blockEntities()) {
               if (blockEntity instanceof ChestBlockEntity) {
                  BlockState state = blockEntity.getCachedState();
                  if (state.contains(ChestBlock.CHEST_TYPE)) {
                     ChestType chestType = (ChestType)state.get(ChestBlock.CHEST_TYPE);
                     if (chestType == ChestType.SINGLE) {
                        ++singleChests;
                     } else {
                        ++doubleChests;
                     }
                  }
               }
            }

            String text = String.format("Chests §f%d", singleChests + doubleChests / 2);
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(event.getContext(), text, (float)(res.getScaledWidth() - width) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, this.getHudColor(this.rainbowOffset));
            if (this.renderingUp) {
               this.bottomRight -= (float)RenderManager.textHeight();
            } else {
               this.topRight += (float)RenderManager.textHeight();
            }

            ++this.rainbowOffset;
         }

         if ((Boolean)this.fpsConfig.getValue()) {
            String text = String.format("FPS §f%d", this.fpsCounter.getPerSecond());
            int width = RenderManager.textWidth(text);
            RenderManager.renderText(event.getContext(), text, (float)(res.getScaledWidth() - width) - 1.0F, this.renderingUp ? this.bottomRight : this.topRight, this.getHudColor(this.rainbowOffset));
            ++this.rainbowOffset;
         }

         if ((Boolean)this.armorConfig.getValue()) {
            Entity riding = mc.player.getVehicle();
            int x = res.getScaledWidth() / 2 + 15;
            int y = res.getScaledHeight();
            int n1 = mc.player.getMaxAir();
            int n2 = Math.min(mc.player.getAir(), n1);
            if (!mc.player.isSubmergedIn(FluidTags.WATER) && n2 >= n1) {
               if (riding instanceof LivingEntity) {
                  LivingEntity entity = (LivingEntity)riding;
                  y -= 45 + (int)Math.ceil((double)((entity.getMaxHealth() - 1.0F) / 20.0F)) * 10;
               } else if (mc.player.isCreative()) {
                  y -= 45;
               } else {
                  y -= 55;
               }
            } else {
               y -= 65;
            }

            for(int i = 3; i >= 0; --i) {
               ItemStack armor = (ItemStack)mc.player.getInventory().armor.get(i);
               int f = armor.getMaxDamage();
               int f2 = armor.getDamage();
               event.getContext().drawItem(armor, x, y);
               event.getContext().drawItemInSlot(mc.textRenderer, armor, x, y);
               if ((Boolean)this.armorDurabilityConfig.getValue() && !armor.isEmpty()) {
                  event.getContext().getMatrices().scale(0.65F, 0.65F, 1.0F);
                  RenderManager.renderText(event.getContext(), Math.round((float)(f - f2) / (float)f * 100.0F) + "%", ((float)x + 2.0F) * 1.5384616F, ((float)y - 5.0F) * 1.5384616F, ColorUtil.hslToColor((float)(f - f2) / (float)f * 120.0F, 100.0F, 50.0F, 1.0F).getRGB());
                  event.getContext().getMatrices().scale(1.5384616F, 1.5384616F, 1.0F);
               }

               x += 18;
            }
         }

         if ((Boolean)this.totemsConfig.getValue() && !mc.player.isCreative() && !mc.player.isSpectator()) {
            int x = res.getScaledWidth() / 2 - 8;
            int y = res.getScaledHeight() - 55;
            event.getContext().drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), x, y - 1);
            String totems = String.valueOf(InventoryUtil.count(Items.TOTEM_OF_UNDYING));
            event.getContext().getMatrices().scale(0.75F, 0.75F, 1.0F);
            event.getContext().getMatrices().translate(0.0F, 0.0F, 200.0F);
            RenderManager.renderText(event.getContext(), totems, (float)(x + 19 - RenderManager.textWidth(totems)) * 1.333333F, (float)(y + 9) * 1.333333F, -1);
            event.getContext().getMatrices().scale(1.333333F, 1.333333F, 1.0F);
         }
      }

   }

   @EventListener
   public void onStatusEffectAdd(StatusEffectEvent.Add event) {
      StatusEffectInstance instance = event.getStatusEffect();
      PotionData data = new PotionData(instance.getEffectType(), instance.getAmplifier(), instance.getDuration());
      if (this.hudRenderPotions.keySet().removeIf((d) -> data.getType() == d.getType())) {
         this.hudRenderPotions.put(data, new Animation(true, 250.0F, Easing.SINE_IN_OUT));
      } else {
         Animation anim = new Animation(false, 250.0F, Easing.SINE_IN_OUT);
         anim.setState(true);
         this.hudRenderPotions.put(data, anim);
      }

   }

   @EventListener
   public void onStatusEffectRemove(StatusEffectEvent.Remove event) {
      for(PotionData data : this.hudRenderPotions.keySet()) {
         if (data.getType().equals(event.getType())) {
            ((Animation)this.hudRenderPotions.get(data)).setState(false);
            break;
         }
      }

   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      this.serverStatus.reset();
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.world == null || mc.isPaused()) {
         this.serverStatus.reset();
      }

   }

   @EventListener
   public void onChatOpen(ScreenOpenEvent event) {
      if (event.getScreen() == null && this.chatOpenAnimation.getState()) {
         this.chatOpenAnimation.setState(false);
      } else if (event.getScreen() instanceof ChatScreen) {
         this.chatOpenAnimation.setState(true);
      }

   }

   @EventListener
   public void onRenderOverlayStatusEffect(RenderOverlayEvent.StatusEffect event) {
      if (this.potionHudConfig.getValue() == HUDModule.VanillaHud.HIDE) {
         event.cancel();
      }

   }

   @EventListener
   public void onRenderOverlayItemName(RenderOverlayEvent.ItemName event) {
      if (this.itemNameConfig.getValue() != HUDModule.VanillaHud.KEEP) {
         event.cancel();
      }

      if (this.itemNameConfig.getValue() == HUDModule.VanillaHud.MOVE) {
         Window window = mc.getWindow();
         int x = window.getScaledWidth() / 2 - 90;
         int y = window.getScaledHeight() - 49;
         boolean armor = !mc.player.getInventory().armor.isEmpty();
         if (mc.player.getAbsorptionAmount() > 0.0F) {
            y -= 9;
         }

         if (armor) {
            y -= 9;
         }

         event.setX(x);
         event.setY(y);
      }

   }

   private int getHudColor(long rainbowOffset) {
      int var10000;
      switch (((RainbowMode)this.rainbowModeConfig.getValue()).ordinal()) {
         case 0:
            var10000 = ColorsModule.getInstance().getRGB();
            break;
         case 1:
            float speed = Math.max(100.0F - (Float)this.rainbowSpeedConfig.getValue(), 0.1F);
            float difference = Math.max(100.0F - (Float)this.rainbowDifferenceConfig.getValue(), 0.1F);
            double roundY = Math.sin(Math.toRadians((double)((float)rainbowOffset * difference) + (double)System.currentTimeMillis() / (double)speed));
            roundY = Math.abs(roundY);
            var10000 = ColorUtil.interpolateColor((float)MathHelper.clamp(roundY, (double)0.0F, (double)1.0F), ColorsModule.getInstance().getColor(), this.gradientColorConfig.getValue()).getRGB();
            break;
         case 2:
            var10000 = this.rainbow(rainbowOffset);
            break;
         case 3:
            var10000 = this.rainbow(1L);
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private String getFormattedModule(Module module) {
      String var10000 = module.getName();
      return var10000 + this.getFormattedModuleData(module.getModuleData());
   }

   private String getFormattedModuleData(String metadata) {
      return metadata != null && !metadata.equals("ARRAYLIST_INFO") ? " §7[§f" + metadata + "§7]" : "";
   }

   public int getLiquidColor(String name, StatusEffect effect) {
      int var10000;
      switch (name.replace("minecraft:", "")) {
         case "speed" -> var10000 = 8171462;
         case "slowness" -> var10000 = 5926017;
         case "haste" -> var10000 = 14270531;
         case "mining_fatigue" -> var10000 = 4866583;
         case "strength" -> var10000 = 9643043;
         case "instant_health" -> var10000 = 16262179;
         case "instant_damage" -> var10000 = 4393481;
         case "jump_boost" -> var10000 = 2293580;
         case "nausea" -> var10000 = 5578058;
         case "regeneration" -> var10000 = 13458603;
         case "resistance" -> var10000 = 10044730;
         case "fire_resistance" -> var10000 = 14981690;
         case "water_breathing" -> var10000 = 3035801;
         case "invisibility" -> var10000 = 8356754;
         case "blindness" -> var10000 = 2039587;
         case "night_vision" -> var10000 = 2039713;
         case "hunger" -> var10000 = 5797459;
         case "weakness" -> var10000 = 4738376;
         case "poison" -> var10000 = 5149489;
         case "wither" -> var10000 = 3484199;
         case "health_boost" -> var10000 = 16284963;
         case "absorption" -> var10000 = 2445989;
         case "saturation" -> var10000 = 16262179;
         case "glowing" -> var10000 = 9740385;
         case "levitation" -> var10000 = 13565951;
         case "luck" -> var10000 = 3381504;
         case "unluck" -> var10000 = 12624973;
         default -> var10000 = effect.getColor();
      }

      return var10000;
   }

   private int rainbow(long offset) {
      float hue = (float)(((double)System.currentTimeMillis() * (double)((Float)this.rainbowSpeedConfig.getValue() / 10.0F) + (double)(offset * 500L)) % (double)(30000.0F / ((Float)this.rainbowDifferenceConfig.getValue() / 100.0F)) / (double)(30000.0F / ((Float)this.rainbowDifferenceConfig.getValue() / 20.0F)));
      return Color.HSBtoRGB(hue, (float)(Integer)this.rainbowSaturationConfig.getValue() / 100.0F, (float)(Integer)this.rainbowBrightnessConfig.getValue() / 100.0F);
   }

   public int alpha(long offset) {
      float[] hsb = new float[3];
      Color color = ColorsModule.getInstance().getColor();
      Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
      float b = (float)(((double)System.currentTimeMillis() * (double)((Float)this.rainbowSpeedConfig.getValue() / 10.0F) + (double)(offset * 500L)) % (double)(30000.0F / ((Float)this.rainbowDifferenceConfig.getValue() / 100.0F)) / (double)(30000.0F / ((Float)this.rainbowDifferenceConfig.getValue() / 20.0F)));
      float brightness = hsb[2] * Math.abs(b % 1.0F - 0.55F) + 0.4F;
      return Color.HSBtoRGB(hsb[0], hsb[1], brightness);
   }

   public float getChatAnimation() {
      return (float)this.chatOpenAnimation.getFactor();
   }

   public static enum VanillaHud {
      MOVE,
      HIDE,
      KEEP;

      // $FF: synthetic method
      private static VanillaHud[] $values() {
         return new VanillaHud[]{MOVE, HIDE, KEEP};
      }
   }

   public static enum Ordering {
      LENGTH,
      ALPHABETICAL;

      // $FF: synthetic method
      private static Ordering[] $values() {
         return new Ordering[]{LENGTH, ALPHABETICAL};
      }
   }

   public static enum Rendering {
      UP,
      DOWN;

      // $FF: synthetic method
      private static Rendering[] $values() {
         return new Rendering[]{UP, DOWN};
      }
   }

   public static enum RainbowMode {
      OFF,
      GRADIENT,
      GRADIENT_HUE,
      STATIC_HUE;

      // $FF: synthetic method
      private static RainbowMode[] $values() {
         return new RainbowMode[]{OFF, GRADIENT, GRADIENT_HUE, STATIC_HUE};
      }
   }

   public static enum SpeedHud {
      K_M_H,
      B_P_S,
      OFF;

      // $FF: synthetic method
      private static SpeedHud[] $values() {
         return new SpeedHud[]{K_M_H, B_P_S, OFF};
      }
   }

   public static enum PotionColors {
      NORMAL,
      OLD,
      OFF;

      // $FF: synthetic method
      private static PotionColors[] $values() {
         return new PotionColors[]{NORMAL, OLD, OFF};
      }
   }

   public class HudRenderModule {
      private final ToggleModule module;
      private double x;
      private long startTime;
      private double endpoint;
      private int prevTextWidth;
      private boolean wasDrawing;

      public HudRenderModule(ToggleModule module) {
         this.module = module;
      }

      public void updateAnimation() {
         String text = HUDModule.this.getFormattedModule(this.module);
         int textWidth = RenderManager.textWidth(text);
         boolean drawing = this.module.isEnabled() && !this.module.isHidden();
         if (drawing != this.wasDrawing || this.prevTextWidth != textWidth) {
            this.startTime = System.currentTimeMillis();
            if (drawing) {
               this.endpoint = (double)((float)(-textWidth) - 2.0F);
            } else {
               this.endpoint = (double)2.0F;
            }

            this.wasDrawing = drawing;
            this.prevTextWidth = textWidth;
         }

         double animationProgress = Math.min((double)((float)(System.currentTimeMillis() - this.startTime) / (float)(Integer)HUDModule.this.animTimeConfig.getValue()), (double)1.0F);
         double factor = Easing.BOUNCE_IN_OUT.ease(animationProgress);
         this.x = this.x * ((double)1.0F - factor) + this.endpoint * factor;
      }

      public void draw(DrawContext context, long drawnCount) {
         int color = ColorUtil.fixTransparency(HUDModule.this.getHudColor(drawnCount - HUDModule.this.rainbowOffset), (float)this.module.getAnimation().getFactor());
         RenderManager.renderText(context, HUDModule.this.getFormattedModule(this.module), (float)Globals.mc.getWindow().getScaledWidth() + (float)this.x, HUDModule.this.renderingUp ? HUDModule.this.topRight : HUDModule.this.bottomRight, color);
         if (HUDModule.this.renderingUp) {
            HUDModule var10000 = HUDModule.this;
            var10000.topRight += (float)RenderManager.textHeight();
         } else {
            HUDModule var5 = HUDModule.this;
            var5.bottomRight -= (float)RenderManager.textHeight();
         }

         ++HUDModule.this.rainbowOffset;
      }
   }

   public static class PotionData implements Comparable<PotionData> {
      private final RegistryEntry<StatusEffect> type;
      private final int amplifier;
      private int duration;

      public PotionData(RegistryEntry<StatusEffect> type, int amplifier, int duration) {
         this.type = type;
         this.amplifier = amplifier;
         this.duration = duration;
      }

      public void update() {
         StatusEffectInstance instance = Globals.mc.player.getStatusEffect(this.type);
         if (instance != null) {
            this.duration = instance.getDuration();
         }

      }

      public String getPotionName() {
         return ((StatusEffect)this.getType().value()).getName().getString();
      }

      public RegistryEntry<StatusEffect> getType() {
         return this.type;
      }

      public int getAmplifier() {
         return this.amplifier;
      }

      public int getDuration() {
         return this.duration;
      }

      public int compareTo(PotionData other) {
         return other.getPotionName().compareTo(this.getPotionName());
      }
   }
}
