package net.hachimi.client.mixin.font;

import net.hachimi.client.api.render.layers.RenderLayersClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.client.font.TextRenderLayerSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({TextRenderLayerSet.class})
public class MixinTextRenderLayerSet {
   @Inject(
      method = {"of"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookOf(Identifier textureId, CallbackInfoReturnable<TextRenderLayerSet> cir) {
      cir.cancel();
      cir.setReturnValue(new TextRenderLayerSet((RenderLayer)RenderLayersClient.TEXT.apply(textureId), (RenderLayer)RenderLayersClient.TEXT_SEE_THROUGH.apply(textureId), (RenderLayer)RenderLayersClient.TEXT_POLYGON_OFFSET.apply(textureId)));
   }

   @Inject(
      method = {"ofIntensity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookOfIntensity(Identifier textureId, CallbackInfoReturnable<TextRenderLayerSet> cir) {
      cir.cancel();
      cir.setReturnValue(new TextRenderLayerSet((RenderLayer)RenderLayersClient.TEXT_INTENSITY.apply(textureId), (RenderLayer)RenderLayersClient.TEXT_INTENSITY_SEE_THROUGH.apply(textureId), (RenderLayer)RenderLayersClient.TEXT_INTENSITY_POLYGON_OFFSET.apply(textureId)));
   }
}
