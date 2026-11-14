package net.hachimi.client.api.render.satin;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;

public final class ManagedSamplerUniformV1 extends ManagedSamplerUniformBase {
   public ManagedSamplerUniformV1(String name) {
      super(name);
   }

   public void set(AbstractTexture texture) {
      this.set((Object)texture);
   }

   public void set(Framebuffer textureFbo) {
      this.set((Object)textureFbo);
   }

   public void set(int textureName) {
      this.set((Object)textureName);
   }

   protected void set(Object value) {
      SamplerAccess[] targets = this.targets;
      if (targets.length > 0 && this.cachedValue != value) {
         for(SamplerAccess target : targets) {
            ((ShaderProgram)target).addSampler(this.name, value);
         }

         this.cachedValue = value;
      }

   }
}
