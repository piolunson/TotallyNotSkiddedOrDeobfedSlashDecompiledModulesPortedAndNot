package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.ItemUseEvent;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorMinecraftClient;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public final class AirPlaceModule extends ToggleModule {
   private static AirPlaceModule INSTANCE;
   Config<Boolean> manualConfig = this.register(new BooleanConfig("Click", "Allow manual air place", true));
   Config<Boolean> grimConfig = this.register(new BooleanConfig("Grim", "Place on air on grim", false));
   Config<Float> rangeConfig;
   Config<Boolean> fluidsConfig;
   private int airPlaceTicks;

   public AirPlaceModule() {
      super("AirPlace", "Allows you to place blocks in the air", ModuleCategory.PLAYER);
      this.rangeConfig = this.register(new NumberConfig("Range", "The range to air place", 1.0F, 4.0F, 6.0F, NumberDisplay.DEFAULT));
      this.fluidsConfig = this.register(new BooleanConfig("Fluids", "Place against fluids", false));
      INSTANCE = this;
   }

   public static AirPlaceModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      this.airPlaceTicks = 0;
   }

   @EventListener
   public void onPlayerTick(TickEvent event) {
      if (this.airPlaceTicks > 0) {
         --this.airPlaceTicks;
      }

      if (mc.player != null && mc.interactionManager != null && (Boolean)this.manualConfig.getValue() && event.getStage() == StageEvent.EventStage.PRE) {
         HitResult result = mc.crosshairTarget;
         if (result instanceof BlockHitResult) {
            BlockHitResult blockHit = (BlockHitResult)result;
            if (!mc.world.isAir(blockHit.getBlockPos())) {
               return;
            }
         }

         ItemStack stack = mc.player.getMainHandStack();
         if (!stack.isEmpty() && stack.getItem() instanceof BlockItem && mc.options.useKey.isPressed()) {
            result = mc.player.raycast((double)(Float)this.rangeConfig.getValue(), 1.0F, (Boolean)this.fluidsConfig.getValue());
            if (((AccessorMinecraftClient)(Object)mc).hookGetItemUseCooldown() == 0 && this.airPlaceTicks == 0 && !mc.player.isUsingItem() && result instanceof BlockHitResult) {
               BlockHitResult blockHitResult = (BlockHitResult)result;
               BlockPos blockPos = BlockPos.ofFloored(blockHitResult.getPos());
               if (!mc.world.isAir(blockPos) || this.isEntityInBlockPos(blockPos)) {
                  return;
               }

               ((AccessorMinecraftClient)(Object)mc).hookSetItemUseCooldown(4);
               this.airPlaceTicks = 4;
               if ((Boolean)this.grimConfig.getValue()) {
                  Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                  mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, blockHitResult);
                  mc.player.swingHand(Hand.MAIN_HAND, false);
                  Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));
                  Managers.NETWORK.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
               } else {
                  mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
                  mc.player.swingHand(Hand.MAIN_HAND);
               }
            }

         }
      }
   }

   @EventListener
   public void onItemUse(ItemUseEvent event) {
      if (this.airPlaceTicks > 0 && (Boolean)this.manualConfig.getValue()) {
         event.cancel();
      }

   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && (Boolean)this.manualConfig.getValue()) {
         HitResult result = mc.crosshairTarget;
         if (result instanceof BlockHitResult) {
            BlockHitResult blockHit2 = (BlockHitResult)result;
            if (!mc.world.isAir(blockHit2.getBlockPos())) {
               return;
            }
         }

         ItemStack stack = mc.player.getMainHandStack();
         if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
            result = mc.player.raycast((double)(Float)this.rangeConfig.getValue(), 1.0F, (Boolean)this.fluidsConfig.getValue());
            if (result instanceof BlockHitResult) {
               BlockHitResult blockHitResult = (BlockHitResult)result;
               BlockPos blockPos = BlockPos.ofFloored(blockHitResult.getPos());
               if (mc.world.isAir(blockPos) && !this.isEntityInBlockPos(blockPos)) {
                  RenderBuffers.preRender();
                  RenderManager.renderBoundingBox(event.getMatrices(), blockPos, 1.5F, ColorsModule.getInstance().getRGB(145));
                  RenderBuffers.postRender();
               }
            }
         }
      }
   }

   private boolean isEntityInBlockPos(BlockPos blockPos) {
      for(Entity entity : mc.world.getEntities()) {
         if (entity.getBoundingBox().intersects(new Box(blockPos))) {
            return true;
         }
      }

      return false;
   }
}
