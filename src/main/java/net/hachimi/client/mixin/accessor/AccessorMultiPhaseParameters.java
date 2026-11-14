package net.hachimi.client.mixin.accessor;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({net.minecraft.client.render.RenderLayer.MultiPhaseParameters.class})
public interface AccessorMultiPhaseParameters {
   @Accessor("outlineMode")
   net.minecraft.client.render.RenderLayer.OutlineMode hookGetOutlineMode();

   @Accessor("texture")
   net.minecraft.client.render.RenderPhase.TextureBase hookGetTexture();
}
