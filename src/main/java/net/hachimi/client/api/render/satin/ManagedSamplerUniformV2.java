package net.hachimi.client.api.render.satin;

import java.util.Objects;
import java.util.function.IntSupplier;
import net.hachimi.client.api.render.satin.uniform.SamplerUniformV2;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.JsonEffectShaderProgram;

public final class ManagedSamplerUniformV2 extends ManagedSamplerUniformBase implements SamplerUniformV2 {
   public ManagedSamplerUniformV2(String name) {
      super(name);
   }

   public void set(AbstractTexture texture) {
      Objects.requireNonNull(texture);
      this.set(texture::getGlId);
   }

   public void set(Framebuffer textureFbo) {
      Objects.requireNonNull(textureFbo);
      this.set(textureFbo::getColorAttachment);
   }

   public void set(int textureName) {
      this.set((IntSupplier)(() -> textureName));
   }

   protected void set(Object value) {
      this.set((IntSupplier)value);
   }

   public void set(IntSupplier value) {
      SamplerAccess[] targets = this.targets;
      if (targets.length > 0 && this.cachedValue != value) {
         for(SamplerAccess target : targets) {
            ((JsonEffectShaderProgram)target).bindSampler(this.name, value);
         }

         this.cachedValue = value;
      }

   }
}
