package net.hachimi.client.mixin.accessor;

import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({net.minecraft.client.render.RenderLayer.MultiPhase.class})
public interface AccessorMultiPhase {
   @Invoker("getPhases")
   net.minecraft.client.render.RenderLayer.MultiPhaseParameters hookGetPhases();
}
