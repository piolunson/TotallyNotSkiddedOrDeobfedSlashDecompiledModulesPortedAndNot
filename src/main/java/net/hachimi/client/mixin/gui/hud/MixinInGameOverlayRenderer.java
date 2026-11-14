package net.hachimi.client.mixin.gui.hud;

import net.hachimi.client.impl.event.gui.hud.RenderOverlayEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameOverlayRenderer.class})
public class MixinInGameOverlayRenderer {
   @Inject(
      method = {"renderFireOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookRenderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
      RenderOverlayEvent.Fire renderOverlayEvent = new RenderOverlayEvent.Fire((DrawContext)null);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"renderUnderwaterOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookRenderUnderwaterOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
      RenderOverlayEvent.Water renderOverlayEvent = new RenderOverlayEvent.Water((DrawContext)null);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"renderInWallOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookRenderFireOverlay(Sprite sprite, MatrixStack matrices, CallbackInfo ci) {
      RenderOverlayEvent.Block renderOverlayEvent = new RenderOverlayEvent.Block((DrawContext)null);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }

   }
}
