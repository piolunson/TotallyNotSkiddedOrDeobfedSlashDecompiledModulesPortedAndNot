package net.hachimi.client.impl.module.render;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.biome.BiomeColorEvent;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.render.AmbientColorEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;

public class AmbienceModule extends ToggleModule {
   private static AmbienceModule INSTANCE;
   Config<Boolean> lightConfig = this.register(new BooleanConfig("Light", "Colors the light", false));
   Config<Color> lightColorConfig;
   Config<Boolean> grassConfig;
   Config<Color> grassColorConfig;
   Config<Boolean> foliageConfig;
   Config<Color> foliageColorConfig;
   Config<Boolean> waterConfig;
   Config<Color> waterColorConfig;

   public AmbienceModule() {
      super("Ambience", "Changes rendering of world", ModuleCategory.RENDER);
      this.lightColorConfig = this.register(new ColorConfig("LightColor", "The color of the light", Color.RED, false, true, () -> this.lightConfig.getValue()));
      this.grassConfig = this.register(new BooleanConfig("Grass", "Colors the grass", false));
      this.grassColorConfig = this.register(new ColorConfig("GrassColor", "The color of the grass", Color.RED, false, true, () -> this.grassConfig.getValue()));
      this.foliageConfig = this.register(new BooleanConfig("Foliage", "Colors the foliage", false));
      this.foliageColorConfig = this.register(new ColorConfig("FoliageColor", "The color of the foliage", Color.RED, false, true, () -> this.foliageConfig.getValue()));
      this.waterConfig = this.register(new BooleanConfig("Water", "Colors the water", false));
      this.waterColorConfig = this.register(new ColorConfig("WaterColor", "The color of the water", Color.RED, false, true, () -> this.waterConfig.getValue()));
      INSTANCE = this;
   }

   public static AmbienceModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      if (mc.world != null) {
         mc.worldRenderer.reload();
      }
   }

   public void onDisable() {
      if (mc.world != null) {
         mc.worldRenderer.reload();
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (mc.world != null && event.getConfig().getContainer() == this && (event.getConfig() != this.lightConfig || event.getConfig() != this.lightColorConfig) && event.getStage() == StageEvent.EventStage.POST) {
         mc.worldRenderer.reload();
      }

   }

   @EventListener
   public void onAmbientColor(AmbientColorEvent event) {
      if ((Boolean)this.lightConfig.getValue()) {
         event.cancel();
         event.setColor(this.lightColorConfig.getValue());
      }

   }

   @EventListener
   public void onGrassColor(BiomeColorEvent.Grass event) {
      if ((Boolean)this.grassConfig.getValue()) {
         event.cancel();
         event.setColor(this.grassColorConfig.getValue());
      }

   }

   @EventListener
   public void onFoliageColor(BiomeColorEvent.Foliage event) {
      if ((Boolean)this.foliageConfig.getValue()) {
         event.cancel();
         event.setColor(this.foliageColorConfig.getValue());
      }

   }

   @EventListener
   public void onWaterColor(BiomeColorEvent.Water event) {
      if ((Boolean)this.waterConfig.getValue()) {
         event.cancel();
         event.setColor(this.waterColorConfig.getValue());
      }

   }
}
