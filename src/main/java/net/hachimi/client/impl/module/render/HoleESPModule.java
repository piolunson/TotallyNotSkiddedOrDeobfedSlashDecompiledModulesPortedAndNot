package net.hachimi.client.impl.module.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.manager.combat.hole.Hole;
import net.hachimi.client.impl.manager.combat.hole.HoleType;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.math.Box;
import net.minecraft.client.util.math.MatrixStack;

public class HoleESPModule extends ToggleModule {
   private static HoleESPModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig("Range", "Range to display holes", 3.0F, 5.0F, 25.0F));
   Config<Boolean> outlineConfig = this.register(new BooleanConfig("Outline", "Renders an outline around the hole", true));
   Config<Boolean> crossConfig = this.register(new BooleanConfig("Cross", "Renders a cross on the floor of the hole", false));
   Config<Float> heightConfig = this.register(new NumberConfig("Size", "Render height of holes", -1.0F, 1.0F, 1.0F));
   Config<Boolean> ignoreSelfConfig = this.register(new BooleanConfig("IgnoreSelf", "Ignores the hole the player is standing in", false));
   Config<Boolean> obsidianCheckConfig = this.register(new BooleanConfig("Obsidian", "Displays obsidian holes", true));
   Config<Boolean> obsidianBedrockConfig = this.register(new BooleanConfig("Obsidian-Bedrock", "Displays mixed obsidian and bedrock holes", true));
   Config<Boolean> doubleConfig = this.register(new BooleanConfig("Double", "Displays double holes where the player can stand in the middle of two blocks to block explosion damage", false));
   Config<Boolean> quadConfig = this.register(new BooleanConfig("Quad", "Displays quad holes where the player can stand in the middle of four blocks to block explosion damage", false));
   Config<Boolean> voidConfig = this.register(new BooleanConfig("Void", "Displays void holes in the world", false));
   Config<Color> obsidianConfig = this.register(new ColorConfig("ObsidianColor", "The color for rendering obsidian holes", new Color(255, 0, 0, 80), () -> this.obsidianCheckConfig.getValue()));
   Config<Color> mixedConfig = this.register(new ColorConfig("Obsidian-BedrockColor", "The color for rendering mixed holes", new Color(255, 255, 0, 80), () -> this.obsidianBedrockConfig.getValue()));
   Config<Color> bedrockConfig = this.register(new ColorConfig("BedrockColor", "The color for rendering bedrock holes", new Color(0, 255, 0, 80)));
   Config<Color> voidColorConfig = this.register(new ColorConfig("VoidColor", "The color for rendering bedrock holes", new Color(255, 0, 0, 140), () -> this.voidConfig.getValue()));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Timer for the fade", 0, 300, 1000, () -> false));
   private final Map<Hole, Animation> fadeList = new HashMap<>();

   public HoleESPModule() {
      super("HoleESP", "Displays nearby blast resistant holes", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public void onDisable() {
      this.fadeList.clear();
   }

   public static HoleESPModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null) {
         for(Hole hole : Managers.HOLE.getHoles()) {
            if (!this.containsPos(this.fadeList.keySet(), hole)) {
               Animation anim = new Animation(false, (float)(Integer)this.fadeTimeConfig.getValue());
               this.fadeList.put(hole, anim);
            }
         }

         RenderBuffers.preRender();

         for(Map.Entry<Hole, Animation> set : this.fadeList.entrySet()) {
            Hole hole = (Hole)set.getKey();
            double dist = hole.squaredDistanceTo(mc.player);
            if (dist > ((NumberConfig)this.rangeConfig).getValueSq()) {
               ((Animation)set.getValue()).setState(false);
            } else if ((Boolean)this.ignoreSelfConfig.getValue() && mc.player.getBoundingBox().intersects(hole.getBoundingBox((double)0.5F))) {
               ((Animation)set.getValue()).setState(false);
            } else if ((!hole.isDoubleX() && !hole.isDoubleZ() || (Boolean)this.doubleConfig.getValue()) && (!hole.isQuad() || (Boolean)this.quadConfig.getValue()) && (hole.getSafety() != HoleType.VOID || (Boolean)this.voidConfig.getValue()) && (hole.getSafety() != HoleType.OBSIDIAN || (Boolean)this.obsidianCheckConfig.getValue()) && (hole.getSafety() != HoleType.OBSIDIAN_BEDROCK || (Boolean)this.obsidianBedrockConfig.getValue())) {
               ((Animation)set.getValue()).setState(this.containsPos(Managers.HOLE.getHoles(), hole));
            } else {
               ((Animation)set.getValue()).setState(false);
            }

            if (!(((Animation)set.getValue()).getFactor() < (double)0.01F)) {
               Color color = this.getHoleColor(hole.getSafety());
               int boxAlpha = (int)((double)color.getAlpha() * ((Animation)set.getValue()).getFactor());
               int lineAlpha = (int)((double)100.0F * ((Animation)set.getValue()).getFactor());
               this.renderHole(event.getMatrices(), hole, this.getHoleColor(hole.getSafety(), boxAlpha), this.getHoleColor(hole.getSafety(), lineAlpha));
            }
         }

         RenderBuffers.postRender();
      }
   }

   private void renderHole(MatrixStack matrixStack, Hole hole, int color1, int color2) {
      Box render = hole.getBoundingBox((double)(Float)this.heightConfig.getValue());
      RenderManager.renderBox(matrixStack, render, color1);
      if ((Boolean)this.outlineConfig.getValue()) {
         RenderManager.renderBoundingBox(matrixStack, render, 1.5F, color2);
      }

      if ((Boolean)this.crossConfig.getValue()) {
         RenderManager.renderBoundingCross(matrixStack, render, 2.0F, color2);
      }

   }

   private boolean containsPos(Set<Hole> set, Hole hole) {
      return set.stream().anyMatch((hole1) -> {
         if (hole1.isDoubleX() == hole.isDoubleX() && hole1.isDoubleZ() == hole.isDoubleZ() && hole1.isQuad() == hole.isQuad() && hole1.isStandard() == hole.isStandard()) {
            return hole.getSafety() != hole1.getSafety() ? false : hole.equals(hole1);
         } else {
            return false;
         }
      });
   }

   private int getHoleColor(HoleType holeType, int alpha) {
      int var10000;
      switch (holeType) {
         case OBSIDIAN -> var10000 = ((ColorConfig)this.obsidianConfig).getRgb(alpha);
         case OBSIDIAN_BEDROCK -> var10000 = ((ColorConfig)this.mixedConfig).getRgb(alpha);
         case BEDROCK -> var10000 = ((ColorConfig)this.bedrockConfig).getRgb(alpha);
         case VOID -> var10000 = ((ColorConfig)this.voidColorConfig).getRgb(alpha);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private Color getHoleColor(HoleType holeType) {
      Color var10000;
      switch (holeType) {
         case OBSIDIAN -> var10000 = this.obsidianConfig.getValue();
         case OBSIDIAN_BEDROCK -> var10000 = this.mixedConfig.getValue();
         case BEDROCK -> var10000 = this.bedrockConfig.getValue();
         case VOID -> var10000 = this.voidColorConfig.getValue();
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public double getRange() {
      return (double)(Float)this.rangeConfig.getValue();
   }
}
