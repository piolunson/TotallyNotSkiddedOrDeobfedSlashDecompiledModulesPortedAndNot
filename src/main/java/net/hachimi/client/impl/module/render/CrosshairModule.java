package net.hachimi.client.impl.module.render;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.gui.hud.RenderCrosshairEvent;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.render.ColorUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.render.animation.Easing;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.world.GameMode;
import net.minecraft.client.gui.DrawContext;

public class CrosshairModule extends ToggleModule {
   Config<Float> lengthConfig = this.register(new NumberConfig("Length", "The crosshair length", 0.0F, 1.0F, 2.5F));
   Config<Float> thicknessConfig = this.register(new NumberConfig("Thickness", "The crosshair thickness", 0.1F, 0.5F, 2.0F));
   Config<Integer> gapConfig = this.register(new NumberConfig("Gap", "The gap between the lines", 1, 2, 5));
   Config<Boolean> dynamicConfig = this.register(new BooleanConfig("Dynamic", "Indicates when the player is moving", false));
   Config<Boolean> outlineConfig = this.register(new BooleanConfig("Outline", "Outlines the crosshair", true));
   Config<Float> outlineThicknessConfig = this.register(new NumberConfig("OutlineThickness", "The width of the outline", 0.1F, 0.3F, 0.5F, () -> this.outlineConfig.getValue()));
   Config<Float> opacityConfig = this.register(new NumberConfig("Opacity", "The crosshair opacity", 0.1F, 1.0F, 1.0F));
   Config<Color> colorConfig;
   private final Animation gapAnimation;

   public CrosshairModule() {
      super("Crosshair", "Custom crosshairs", ModuleCategory.RENDER);
      this.colorConfig = this.register(new ColorConfig("Color", "The crosshair color", Color.WHITE));
      this.gapAnimation = new Animation(false, 100.0F, Easing.LINEAR);
   }

   @EventListener
   public void onRenderCrosshair(RenderCrosshairEvent event) {
      event.cancel();
      if (mc.options.getPerspective().isFirstPerson() && mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         DrawContext context = event.getContext();
         float x = (float)context.getScaledWindowWidth() / 2.0F - 0.5F;
         float y = (float)context.getScaledWindowHeight() / 2.0F - 0.5F;
         float halfLength = (Float)this.lengthConfig.getValue() * 10.0F / 2.0F;
         float o1 = (Float)this.outlineThicknessConfig.getValue();
         float o2 = o1 * 2.0F;
         float o3 = o1 * 3.0F;
         boolean moving = MovementUtil.isMovingInput() || mc.player.isSneaking() || mc.player.isClimbing() || !mc.player.isOnGround();
         double gap = (double)(Integer)this.gapConfig.getValue();
         if ((Boolean)this.dynamicConfig.getValue()) {
            this.gapAnimation.setState(moving);
            gap += (double)2.0F * this.gapAnimation.getFactor();
         }

         if ((Boolean)this.outlineConfig.getValue()) {
            RenderManager.rect(context.getMatrices(), (double)(x - halfLength) - gap - (double)o2, (double)(y - (Float)this.thicknessConfig.getValue() - o1), (double)(halfLength + o3), (double)((Float)this.thicknessConfig.getValue() * 2.0F + o2), ColorUtil.withAlpha(Color.BLACK.getRGB(), (Float)this.opacityConfig.getValue()));
            RenderManager.rect(context.getMatrices(), (double)x + gap - (double)o2, (double)(y - (Float)this.thicknessConfig.getValue() - o1), (double)(halfLength + o3), (double)((Float)this.thicknessConfig.getValue() * 2.0F + o2), ColorUtil.withAlpha(Color.BLACK.getRGB(), (Float)this.opacityConfig.getValue()));
         }

         RenderManager.rect(context.getMatrices(), (double)(x - halfLength) - gap, (double)(y - (Float)this.thicknessConfig.getValue()), (double)halfLength, (double)((Float)this.thicknessConfig.getValue() * 2.0F), ColorUtil.withAlpha(((Color)this.colorConfig.getValue()).getRGB(), (Float)this.opacityConfig.getValue()));
         RenderManager.rect(context.getMatrices(), (double)x + gap, (double)(y - (Float)this.thicknessConfig.getValue()), (double)halfLength, (double)((Float)this.thicknessConfig.getValue() * 2.0F), ColorUtil.withAlpha(((Color)this.colorConfig.getValue()).getRGB(), (Float)this.opacityConfig.getValue()));
         if ((Boolean)this.outlineConfig.getValue()) {
            RenderManager.rect(context.getMatrices(), (double)(x - (Float)this.thicknessConfig.getValue() - o1), (double)(y - halfLength) - gap - (double)o1, (double)((Float)this.thicknessConfig.getValue() * 2.0F + o2), (double)(halfLength + o3), ColorUtil.withAlpha(Color.BLACK.getRGB(), (Float)this.opacityConfig.getValue()));
            RenderManager.rect(context.getMatrices(), (double)(x - (Float)this.thicknessConfig.getValue() - o1), (double)y + gap - (double)o1, (double)((Float)this.thicknessConfig.getValue() * 2.0F + o2), (double)(halfLength + o3), ColorUtil.withAlpha(Color.BLACK.getRGB(), (Float)this.opacityConfig.getValue()));
         }

         RenderManager.rect(context.getMatrices(), (double)(x - (Float)this.thicknessConfig.getValue()), (double)(y - halfLength) - gap, (double)((Float)this.thicknessConfig.getValue() * 2.0F), (double)halfLength, ColorUtil.withAlpha(((Color)this.colorConfig.getValue()).getRGB(), (Float)this.opacityConfig.getValue()));
         RenderManager.rect(context.getMatrices(), (double)(x - (Float)this.thicknessConfig.getValue()), (double)y + gap, (double)((Float)this.thicknessConfig.getValue() * 2.0F), (double)halfLength, ColorUtil.withAlpha(((Color)this.colorConfig.getValue()).getRGB(), (Float)this.opacityConfig.getValue()));
      }
   }
}
