package net.hachimi.client.mixin.accessor;

import java.util.Optional;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({net.minecraft.client.render.RenderPhase.TextureBase.class})
public interface AccessorTextureBase {
   @Invoker("getId")
   Optional<Identifier> hookGetId();
}
