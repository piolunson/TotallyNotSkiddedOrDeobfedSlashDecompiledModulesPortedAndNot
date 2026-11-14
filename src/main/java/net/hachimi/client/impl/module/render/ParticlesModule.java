package net.hachimi.client.impl.module.render;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.particle.BlockBreakParticleEvent;
import net.hachimi.client.impl.event.particle.EmitParticleEvent;
import net.hachimi.client.impl.event.particle.ParticleEvent;
import net.hachimi.client.impl.event.particle.TotemParticleEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;

public class ParticlesModule extends ToggleModule {
   Config<TotemParticle> totemConfig;
   Config<Color> totemColorConfig;
   Config<Integer> totemParticlesConfig;
   Config<Integer> totemParticleTicksConfig;
   Config<Boolean> explosionsConfig;
   Config<Boolean> fireworkConfig;
   Config<Boolean> potionConfig;
   Config<Boolean> itemsConfig;
   Config<Boolean> bottleConfig;
   Config<Boolean> portalConfig;
   Config<Boolean> blockConfig;
   Config<Boolean> blockBreakConfig;
   Config<Boolean> campfiresConfig;
   Config<Boolean> obsidianTearConfig;

   public ParticlesModule() {
      super("Particles", "Change the rendering of particles", ModuleCategory.RENDER);
      this.totemConfig = this.register(new EnumConfig("Totem", "Modify totem particle rendering", ParticlesModule.TotemParticle.OFF, ParticlesModule.TotemParticle.values()));
      this.totemColorConfig = this.register(new ColorConfig("TotemColor", "Color of the totem particles", new Color(25, 120, 0), false, false, () -> this.totemConfig.getValue() == ParticlesModule.TotemParticle.COLOR));
      this.totemParticlesConfig = this.register(new NumberConfig("TotemParticles", "Max totem particles", 3, 16, 16, () -> this.totemConfig.getValue() != ParticlesModule.TotemParticle.REMOVE));
      this.totemParticleTicksConfig = this.register(new NumberConfig("TotemParticleTicks", "Max totem particle ticks", 5, 30, 30, () -> this.totemConfig.getValue() != ParticlesModule.TotemParticle.REMOVE));
      this.explosionsConfig = this.register(new BooleanConfig("Explosions", "Prevents explosion particles from rendering", true));
      this.fireworkConfig = this.register(new BooleanConfig("Firework", "Prevents rendering of firework particles", false));
      this.potionConfig = this.register(new BooleanConfig("Effects", "Prevents rendering of potion effect particles", true));
      this.itemsConfig = this.register(new BooleanConfig("Items", "Prevents rendering of eating particles", false));
      this.bottleConfig = this.register(new BooleanConfig("BottleSplash", "Prevents rendering of bottle splash particles", true));
      this.portalConfig = this.register(new BooleanConfig("Portal", "Prevents rendering of portal particles", true));
      this.blockConfig = this.register(new BooleanConfig("Block", "Prevents block particles from rendering", false));
      this.blockBreakConfig = this.register(new BooleanConfig("BlockBreak", "Prevents block break particles from rendering", false));
      this.campfiresConfig = this.register(new BooleanConfig("Campfires", "Prevents campfire particles from rendering", false));
      this.obsidianTearConfig = this.register(new BooleanConfig("CryingObsidian", "Prevents obsidian tear particles from rendering", false));
   }

   @EventListener
   public void onParticle(ParticleEvent event) {
      if ((Boolean)this.potionConfig.getValue() && event.getParticleType() == ParticleTypes.ENTITY_EFFECT || (Boolean)this.explosionsConfig.getValue() && (event.getParticleType() == ParticleTypes.EXPLOSION || event.getParticleType() == ParticleTypes.EXPLOSION_EMITTER) || (Boolean)this.fireworkConfig.getValue() && event.getParticleType() == ParticleTypes.FIREWORK || (Boolean)this.itemsConfig.getValue() && event.getParticleType() == ParticleTypes.ITEM || (Boolean)this.bottleConfig.getValue() && (event.getParticleType() == ParticleTypes.EFFECT || event.getParticleType() == ParticleTypes.INSTANT_EFFECT) || (Boolean)this.portalConfig.getValue() && event.getParticleType() == ParticleTypes.PORTAL || (Boolean)this.blockConfig.getValue() && event.getParticleType() == ParticleTypes.BLOCK || (Boolean)this.campfiresConfig.getValue() && event.getParticleType() == ParticleTypes.CAMPFIRE_COSY_SMOKE || (Boolean)this.obsidianTearConfig.getValue() && (event.getParticleType() == ParticleTypes.FALLING_OBSIDIAN_TEAR || event.getParticleType() == ParticleTypes.DRIPPING_OBSIDIAN_TEAR || event.getParticleType() == ParticleTypes.LANDING_OBSIDIAN_TEAR)) {
         event.cancel();
      }

   }

   @EventListener
   public void onParticleEmitter(ParticleEvent.Emitter event) {
      if (this.totemConfig.getValue() == ParticlesModule.TotemParticle.REMOVE && event.getParticleType() == ParticleTypes.TOTEM_OF_UNDYING) {
         event.cancel();
      }

   }

   @EventListener
   public void onEmitParticle(EmitParticleEvent event) {
      if (this.totemConfig.getValue() != ParticlesModule.TotemParticle.REMOVE && event.getParticleType() == ParticleTypes.TOTEM_OF_UNDYING) {
         event.cancel();
         event.setParticleCount((Integer)this.totemParticlesConfig.getValue());
         event.setParticleTime((Integer)this.totemParticleTicksConfig.getValue());
      }

   }

   @EventListener
   public void onTotemParticle(TotemParticleEvent event) {
      if (this.totemConfig.getValue() == ParticlesModule.TotemParticle.COLOR) {
         event.cancel();
         Color color = this.totemColorConfig.getValue();
         float r = (float)color.getRed() / 255.0F;
         float g = (float)color.getGreen() / 255.0F;
         float b = (float)color.getBlue() / 255.0F;
         if (RANDOM.nextInt(4) == 0) {
            float r2 = (float)color.getRed() * 0.7F / 255.0F;
            float g2 = (float)color.getGreen() * 0.7F / 255.0F;
            float b2 = (float)color.getBlue() * 0.7F / 255.0F;
            event.setColor(new Color(MathHelper.clamp(r2, 0.0F, 1.0F), MathHelper.clamp(g2, 0.0F, 1.0F), MathHelper.clamp(b2, 0.0F, 1.0F)));
         } else {
            event.setColor(new Color(MathHelper.clamp(r + RANDOM.nextFloat() * 0.1F, 0.0F, 1.0F), MathHelper.clamp(g + RANDOM.nextFloat() * 0.1F, 0.0F, 1.0F), MathHelper.clamp(b + RANDOM.nextFloat() * 0.1F, 0.0F, 1.0F)));
         }
      }

   }

   @EventListener
   public void onBlockBreakParticle(BlockBreakParticleEvent event) {
      if ((Boolean)this.blockBreakConfig.getValue()) {
         event.cancel();
      }

   }

   private static enum TotemParticle {
      OFF,
      REMOVE,
      COLOR;

      // $FF: synthetic method
      private static TotemParticle[] $values() {
         return new TotemParticle[]{OFF, REMOVE, COLOR};
      }
   }
}
