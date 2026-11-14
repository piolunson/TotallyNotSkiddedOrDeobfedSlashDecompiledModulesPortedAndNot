package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.BlockPlacerModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class AutoWebModule extends BlockPlacerModule {
   private static AutoWebModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig("PlaceRange", "The range to fill nearby holes", 0.1F, 4.0F, 6.0F));
   Config<Float> enemyRangeConfig = this.register(new NumberConfig("EnemyRange", "The maximum range of targets", 0.1F, 10.0F, 15.0F));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to block before placing", false));
   Config<Boolean> coverHeadConfig = this.register(new BooleanConfig("CoverHead", "Places webs on the targets head", false));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig("ShiftTicks", "The number of blocks to place per tick", 1, 2, 5));
   Config<Integer> shiftDelayConfig = this.register(new NumberConfig("ShiftDelay", "The delay between each block placement interval", 0, 1, 5));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders web placements", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private int shiftDelay;
   private List<BlockPos> webs = new ArrayList<>();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();

   public AutoWebModule() {
      super("AutoWeb", "Automatically traps nearby entities in webs", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AutoWebModule getInstance() {
      return INSTANCE;
   }

   public void onDisable() {
      this.fadeList.clear();
      this.webs.clear();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!(Boolean)this.multitaskConfig.getValue() && this.checkMultitask()) {
         this.webs.clear();
      } else {
         int blocksPlaced = 0;
         int slot = this.getBlockItemSlot(Blocks.COBWEB);
         if (slot == -1) {
            this.webs.clear();
         } else if (this.shiftDelay < (Integer)this.shiftDelayConfig.getValue()) {
            ++this.shiftDelay;
         } else {
            List<BlockPos> webPlacements = new ArrayList<>();

            for(PlayerEntity entity : mc.world.getPlayers()) {
               if (entity != mc.player && !Managers.SOCIAL.isFriend(entity.getName())) {
                  double d = (double)mc.player.distanceTo(entity);
                  if (!(d > (double)(Float)this.enemyRangeConfig.getValue())) {
                     BlockPos feetPos = entity.getBlockPos();
                     double dist = mc.player.getEyePos().squaredDistanceTo(feetPos.toCenterPos());
                     if (mc.world.getBlockState(feetPos).isAir() && dist <= ((NumberConfig)this.rangeConfig).getValueSq()) {
                        webPlacements.add(feetPos);
                     }

                     if ((Boolean)this.coverHeadConfig.getValue()) {
                        BlockPos headPos = feetPos.up();
                        double dist2 = mc.player.getEyePos().squaredDistanceTo(headPos.toCenterPos());
                        if (mc.world.getBlockState(headPos).isAir() && dist2 <= ((NumberConfig)this.rangeConfig).getValueSq()) {
                           webPlacements.add(headPos);
                        }
                     }
                  }
               }
            }

            this.webs = webPlacements;
            if (!this.webs.isEmpty()) {
               while(blocksPlaced < (Integer)this.shiftTicksConfig.getValue() && blocksPlaced < this.webs.size()) {
                  BlockPos targetPos = (BlockPos)this.webs.get(blocksPlaced);
                  ++blocksPlaced;
                  this.shiftDelay = 0;
                  this.placeWeb(targetPos, slot);
               }

               if ((Boolean)this.rotateConfig.getValue()) {
                  Managers.ROTATION.setRotationSilentSync();
               }

            }
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if ((Boolean)this.renderConfig.getValue()) {
         RenderBuffers.preRender();

         for(Map.Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            ((Animation)set.getValue()).setState(false);
            int lineAlpha = (int)((double)120.0F * ((Animation)set.getValue()).getFactor());
            Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
            BlockPos blockPos = (BlockPos)set.getKey();
            double x1 = (double)blockPos.getX();
            double y1 = (double)blockPos.getY();
            double z1 = (double)blockPos.getZ();
            double x2 = (double)blockPos.getX() + (double)1.0F;
            double y2 = (double)blockPos.getY() + (double)1.0F;
            double z2 = (double)blockPos.getZ() + (double)1.0F;
            RenderManager.renderPlane(event.getMatrices(), x1, y1, z1, x2, y2, z2, lineColor.getRGB());
            RenderManager.renderPlane(event.getMatrices(), x2, y1, z1, x1, y2, z2, lineColor.getRGB());
         }

         RenderBuffers.postRender();
         if (this.webs.isEmpty()) {
            return;
         }

         for(BlockPos pos : this.webs) {
            Animation animation = new Animation(true, (float)(Integer)this.fadeTimeConfig.getValue());
            this.fadeList.put(pos, animation);
         }
      }

      this.fadeList.entrySet().removeIf((e) -> ((Animation)e.getValue()).getFactor() == (double)0.0F);
   }

   private void placeWeb(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock((BlockPos)pos, slot, (Boolean)this.strictDirectionConfig.getValue(), false, (state, angles) -> {
         if ((Boolean)this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }

      });
   }

   public boolean isPlacing() {
      return !this.webs.isEmpty();
   }
}
