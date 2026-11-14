package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.biome.BiomeEffectsEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.world.biome.BiomeParticleConfig;

public class NoWeatherModule extends ToggleModule {
   Config<Weather> weatherConfig;
   private Weather weather;

   public NoWeatherModule() {
      super("NoWeather", "Prevents weather rendering", ModuleCategory.RENDER);
      this.weatherConfig = this.register(new EnumConfig("Weather", "The world weather", NoWeatherModule.Weather.CLEAR, NoWeatherModule.Weather.values()));
   }

   public String getModuleData() {
      return EnumFormatter.formatEnum(this.weatherConfig.getValue());
   }

   public void onEnable() {
      if (mc.world != null) {
         if (mc.world.isThundering()) {
            this.weather = NoWeatherModule.Weather.THUNDER;
         } else if (mc.world.isRaining()) {
            this.weather = NoWeatherModule.Weather.RAIN;
         } else {
            this.weather = NoWeatherModule.Weather.CLEAR;
         }

         this.setWeather(this.weatherConfig.getValue());
      }

   }

   public void onDisable() {
      if (mc.world != null && this.weather != null) {
         this.setWeather(this.weather);
      }

   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST) {
         this.setWeather(this.weatherConfig.getValue());
      }

   }

   @EventListener
   public void onBiomeEffects(BiomeEffectsEvent event) {
      if (this.weatherConfig.getValue() == NoWeatherModule.Weather.ASH) {
         event.cancel();
         event.setParticleConfig(new BiomeParticleConfig(ParticleTypes.WHITE_ASH, 0.118093334F));
      }

   }

   private void setWeather(Weather weather) {
      switch (weather.ordinal()) {
         case 0:
         case 3:
            mc.world.getLevelProperties().setRaining(false);
            mc.world.setRainGradient(0.0F);
            mc.world.setThunderGradient(0.0F);
            break;
         case 1:
            mc.world.getLevelProperties().setRaining(true);
            mc.world.setRainGradient(1.0F);
            mc.world.setThunderGradient(0.0F);
            break;
         case 2:
            mc.world.getLevelProperties().setRaining(true);
            mc.world.setRainGradient(2.0F);
            mc.world.setThunderGradient(1.0F);
      }

   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof GameStateChangeS2CPacket packet) {
         if (packet.getReason() == GameStateChangeS2CPacket.RAIN_STARTED || packet.getReason() == GameStateChangeS2CPacket.RAIN_STOPPED || packet.getReason() == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED || packet.getReason() == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED) {
            event.cancel();
         }
      }

   }

   public static enum Weather {
      CLEAR,
      RAIN,
      THUNDER,
      ASH;

      // $FF: synthetic method
      private static Weather[] $values() {
         return new Weather[]{CLEAR, RAIN, THUNDER, ASH};
      }
   }
}
