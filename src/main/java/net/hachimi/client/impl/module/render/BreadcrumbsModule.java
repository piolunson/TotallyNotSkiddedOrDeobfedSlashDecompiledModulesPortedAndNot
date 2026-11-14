package net.hachimi.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.Interpolation;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.event.world.RemoveEntityEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class BreadcrumbsModule extends ToggleModule {
   private final Map<Integer, List<TimedPosition>> positions = new ConcurrentHashMap<>();
   Config<Boolean> infiniteConfig = this.register(new BooleanConfig("Infinite", "Renders breadcrumbs for all positions since toggle", true));
   Config<Float> maxTimeConfig = this.register(new NumberConfig("MaxPosition", "The maximum time for a given position", 1.0F, 2.0F, 20.0F));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Timer for the fade", 0, 1000, 5000, () -> false));
   Config<Float> widthConfig = this.register(new NumberConfig("Width", "The line width of the path", 1.0F, 1.0F, 5.0F));
   Config<Boolean> selfConfig = this.register(new BooleanConfig("Self", "Renders breadcrumbs on player", true));
   Config<Boolean> playersConfig = this.register(new BooleanConfig("Players", "Renders breadcrumbs on other players", false));
   Config<Boolean> pearlsConfig = this.register(new BooleanConfig("Pearls", "Renders breadcrumbs on thrown pearls", false));
   Config<Boolean> arrowsConfig = this.register(new BooleanConfig("Arrows", "Renders breadcrumbs on arrows", false));
   Config<Boolean> xpBottlesConfig = this.register(new BooleanConfig("XPBottles", "Renders breadcrumbs on thrown experience bottles", false));

   public BreadcrumbsModule() {
      super("Breadcrumbs", "Renders a line connecting all previous positions", ModuleCategory.RENDER);
   }

   public void onDisable() {
      this.positions.clear();
   }

   @EventListener
   public void onPlayerUpdate(PlayerTickEvent event) {
      for(Entity entity : mc.world.getEntities()) {
         if (this.checkEntity(entity)) {
            Vec3d pos = Interpolation.getInterpolatedPosition(entity, mc.getRenderTickCounter().getTickDelta(true));
            if (this.positions.containsKey(entity.getId())) {
               ((List)this.positions.get(entity.getId())).add(new TimedPosition(pos, System.currentTimeMillis()));
            } else {
               List<TimedPosition> timedPositions = new CopyOnWriteArrayList();
               timedPositions.add(new TimedPosition(pos, System.currentTimeMillis()));
               this.positions.put(entity.getId(), timedPositions);
            }
         }
      }

      if (!(Boolean)this.infiniteConfig.getValue()) {
         for(Map.Entry<Integer, List<TimedPosition>> entry : this.positions.entrySet()) {
            for(TimedPosition timedPosition : (List<TimedPosition>)entry.getValue()) {
               if ((float)(System.currentTimeMillis() - timedPosition.time()) > (Float)this.maxTimeConfig.getValue() * 1000.0F) {
                  ((List)this.positions.get(entry.getKey())).remove(timedPosition);
               }
            }
         }
      }

   }

   @EventListener
   public void onEntityDeath(RemoveEntityEvent event) {
      if ((Boolean)this.infiniteConfig.getValue()) {
         this.positions.remove(event.getEntity().getId());
      }

   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      event.getMatrices().push();
      RenderBuffers.preRender();
      RenderSystem.lineWidth((Float)this.widthConfig.getValue());
      RenderBuffers.LINES.begin(event.getMatrices());

      for(Map.Entry<Integer, List<TimedPosition>> entry : this.positions.entrySet()) {
         List<TimedPosition> timedPositions = (List)entry.getValue();

         for(int i = 0; i < timedPositions.size(); ++i) {
            TimedPosition timedPosition = (TimedPosition)timedPositions.get(i);
            if (!(Boolean)this.infiniteConfig.getValue()) {
               float fade = 1.0F - MathHelper.clamp((float)(System.currentTimeMillis() - timedPosition.time()) / (float)(Integer)this.fadeTimeConfig.getValue(), 0.0F, 1.0F);
               RenderBuffers.LINES.color(ColorsModule.getInstance().getRGB((int)(fade * 255.0F)));
            } else {
               RenderBuffers.LINES.color(ColorsModule.getInstance().getRGB());
            }

            if (i > 1) {
               Vec3d vec3d = ((TimedPosition)timedPositions.get(i - 1)).pos();
               Vec3d vec3d2 = timedPosition.pos();
               RenderBuffers.LINES.vertexLine(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z);
            }
         }
      }

      RenderBuffers.LINES.end();
      RenderBuffers.postRender();
      event.getMatrices().pop();
   }

   public boolean checkEntity(Entity entity) {
      if (entity instanceof PlayerEntity) {
         return (Boolean)this.playersConfig.getValue() || entity == mc.player && (Boolean)this.selfConfig.getValue();
      } else {
         return entity instanceof EnderPearlEntity && (Boolean)this.pearlsConfig.getValue() || entity instanceof ArrowEntity && (Boolean)this.arrowsConfig.getValue() || entity instanceof ExperienceBottleEntity && (Boolean)this.xpBottlesConfig.getValue();
      }
   }

   private static record TimedPosition(Vec3d pos, long time) {
   }
}
