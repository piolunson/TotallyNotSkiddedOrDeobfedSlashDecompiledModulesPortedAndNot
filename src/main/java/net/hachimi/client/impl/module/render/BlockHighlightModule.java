package net.hachimi.client.impl.module.render;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.BoxRender;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.render.RenderBlockOutlineEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult.Type;

public class BlockHighlightModule extends ToggleModule {
   Config<BoxRender> boxModeConfig;
   Config<Boolean> entitiesConfig;
   Config<Float> widthConfig;
   Config<Boolean> fadeConfig;
   Config<Integer> fadeTimeConfig;
   private double distance;
   private final Map<Box, Animation> fadeList;

   public BlockHighlightModule() {
      super("BlockHighlight", "Highlights the block the player is facing", ModuleCategory.RENDER);
      this.boxModeConfig = this.register(new EnumConfig("BoxMode", "Box rendering mode", BoxRender.OUTLINE, BoxRender.values()));
      this.entitiesConfig = this.register(new BooleanConfig("Debug-Entities", "Highlights entity bounding boxes for debug purposes", false));
      this.widthConfig = this.register(new NumberConfig("Width", "The line width of the highlight", 1.0F, 1.0F, 5.0F));
      this.fadeConfig = this.register(new BooleanConfig("Fade", "Fades the block highlight", false));
      this.fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Timer for the fade", 0, 200, 1000, () -> false));
      this.fadeList = new HashMap<>();
   }

   public String getModuleData() {
      if (mc.world == null) {
         return super.getModuleData();
      } else {
         net.minecraft.util.hit.HitResult var2 = mc.crosshairTarget;
         if (var2 instanceof BlockHitResult) {
            BlockHitResult result = (BlockHitResult)var2;
            if (mc.world.getBlockState(result.getBlockPos()).isAir()) {
               return super.getModuleData();
            }
         }

         DecimalFormat decimal = new DecimalFormat("0.0");
         return decimal.format(this.distance);
      }
   }

   public void onDisable() {
      this.fadeList.clear();
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.world != null) {
         if (!(Boolean)this.fadeConfig.getValue() && !this.fadeList.isEmpty()) {
            this.fadeList.clear();
         }

         Box render = null;
         net.minecraft.util.hit.HitResult result = mc.crosshairTarget;
         if (result != null) {
            Vec3d pos = Managers.POSITION.getEyePos();
            if ((Boolean)this.entitiesConfig.getValue() && result.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
               Entity entity = ((EntityHitResult)result).getEntity();
               render = entity.getBoundingBox();
               this.distance = pos.distanceTo(entity.getPos());
               if ((Boolean)this.fadeConfig.getValue()) {
                  this.fadeList.put(render, new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue()));
               }
            } else if (result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
               BlockPos hpos = ((BlockHitResult)result).getBlockPos();
               BlockState state = mc.world.getBlockState(hpos);
               VoxelShape outlineShape = state.getOutlineShape(mc.world, hpos);
               if (outlineShape.isEmpty()) {
                  return;
               }

               Box render1 = outlineShape.getBoundingBox();
               render = new Box((double)hpos.getX() + render1.minX, (double)hpos.getY() + render1.minY, (double)hpos.getZ() + render1.minZ, (double)hpos.getX() + render1.maxX, (double)hpos.getY() + render1.maxY, (double)hpos.getZ() + render1.maxZ);
               this.distance = pos.distanceTo(hpos.toCenterPos());
               if ((Boolean)this.fadeConfig.getValue()) {
                  this.fadeList.put(render, new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue()));
               }
            }
         }

         RenderBuffers.preRender();
         if ((Boolean)this.fadeConfig.getValue()) {
            for(Map.Entry<Box, Animation> set : this.fadeList.entrySet()) {
               Box box = (Box)set.getKey();
               ((Animation)set.getValue()).setState(false);
               if (!(((Animation)set.getValue()).getFactor() < (double)0.01F)) {
                  int boxAlpha = (int)((double)40.0F * ((Animation)set.getValue()).getFactor());
                  int lineAlpha = (int)((double)145.0F * ((Animation)set.getValue()).getFactor());
                  this.renderBb(event.getMatrices(), box, ColorsModule.getInstance().getRGB(boxAlpha), ColorsModule.getInstance().getRGB(lineAlpha));
               }
            }
         } else if (render != null) {
            this.renderBb(event.getMatrices(), render, ColorsModule.getInstance().getRGB(40), ColorsModule.getInstance().getRGB(145));
         }

         RenderBuffers.postRender();
      }
   }

   private void renderBb(MatrixStack matrixStack, Box render, int color, int lineColor) {
      switch ((BoxRender)this.boxModeConfig.getValue()) {
         case FILL:
            RenderManager.renderBox(matrixStack, render, color);
            RenderManager.renderBoundingBox(matrixStack, render, (Float)this.widthConfig.getValue(), lineColor);
            break;
         case OUTLINE:
            RenderManager.renderBoundingBox(matrixStack, render, (Float)this.widthConfig.getValue(), lineColor);
      }

   }

   @EventListener
   public void onRenderBlockOutline(RenderBlockOutlineEvent event) {
      event.cancel();
   }
}
