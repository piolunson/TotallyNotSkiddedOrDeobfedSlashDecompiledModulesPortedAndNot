package net.hachimi.client.api.render.satin;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.hachimi.client.api.render.satin.uniform.Uniform1f;
import net.hachimi.client.api.render.satin.uniform.Uniform1i;
import net.hachimi.client.api.render.satin.uniform.Uniform2f;
import net.hachimi.client.api.render.satin.uniform.Uniform2i;
import net.hachimi.client.api.render.satin.uniform.Uniform3f;
import net.hachimi.client.api.render.satin.uniform.Uniform3i;
import net.hachimi.client.api.render.satin.uniform.Uniform4f;
import net.hachimi.client.api.render.satin.uniform.Uniform4i;
import net.hachimi.client.api.render.satin.uniform.UniformFinder;
import net.hachimi.client.api.render.satin.uniform.UniformMat4;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceFactory;

public abstract class ResettableManagedShaderBase<S extends AutoCloseable> implements UniformFinder {
   private final Identifier location;
   private final Map<String, ManagedUniform> managedUniforms = new HashMap<>();
   private final List<ManagedUniformBase> allUniforms = new ArrayList<>();
   private boolean errored;
   protected S shader;

   public ResettableManagedShaderBase(Identifier location) {
      this.location = location;
   }

   public void initializeOrLog(ResourceFactory mgr) {
      try {
         this.initialize(mgr);
      } catch (IOException e) {
         this.errored = true;
         this.logInitError(e);
      }

   }

   protected abstract void logInitError(IOException var1);

   protected void initialize(ResourceFactory resourceManager) throws IOException {
      this.release();
      MinecraftClient mc = MinecraftClient.getInstance();
      this.shader = this.parseShader(resourceManager, mc, this.location);
      this.setup(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
   }

   protected abstract S parseShader(ResourceFactory var1, MinecraftClient var2, Identifier var3) throws IOException;

   public void release() {
      if (this.isInitialized()) {
         try {
            assert this.shader != null;

            this.shader.close();
            this.shader = null;
         } catch (Exception e) {
            throw new RuntimeException("Failed to release shader " + String.valueOf(this.location), e);
         }
      }

      this.errored = false;
   }

   protected Collection<ManagedUniformBase> getManagedUniforms() {
      return this.allUniforms;
   }

   protected abstract boolean setupUniform(ManagedUniformBase var1, S var2);

   public boolean isInitialized() {
      return this.shader != null;
   }

   public boolean isErrored() {
      return this.errored;
   }

   public Identifier getLocation() {
      return this.location;
   }

   protected <U extends ManagedUniformBase> U manageUniform(Map<String, U> uniformMap, Function<String, U> factory, String uniformName, String uniformKind) {
      U existing = (U)(uniformMap.get(uniformName));
      if (existing != null) {
         return existing;
      } else {
         U ret = (U)(factory.apply(uniformName));
         if (this.shader != null) {
            boolean found = this.setupUniform(ret, this.shader);
            if (!found) {
               LogUtils.getLogger().warn("No {} found with name {} in shader {}", new Object[]{uniformKind, uniformName, this.location});
            }
         }

         uniformMap.put(uniformName, ret);
         this.allUniforms.add(ret);
         return ret;
      }
   }

   public Uniform1i findUniform1i(String uniformName) {
      return (Uniform1i)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 1), uniformName, "uniform");
   }

   public Uniform2i findUniform2i(String uniformName) {
      return (Uniform2i)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 2), uniformName, "uniform");
   }

   public Uniform3i findUniform3i(String uniformName) {
      return (Uniform3i)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 3), uniformName, "uniform");
   }

   public Uniform4i findUniform4i(String uniformName) {
      return (Uniform4i)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 4), uniformName, "uniform");
   }

   public Uniform1f findUniform1f(String uniformName) {
      return (Uniform1f)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 1), uniformName, "uniform");
   }

   public Uniform2f findUniform2f(String uniformName) {
      return (Uniform2f)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 2), uniformName, "uniform");
   }

   public Uniform3f findUniform3f(String uniformName) {
      return (Uniform3f)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 3), uniformName, "uniform");
   }

   public Uniform4f findUniform4f(String uniformName) {
      return (Uniform4f)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 4), uniformName, "uniform");
   }

   public UniformMat4 findUniformMat4(String uniformName) {
      return (UniformMat4)this.manageUniform(this.managedUniforms, (name) -> new ManagedUniform(name, 16), uniformName, "uniform");
   }

   public abstract void setup(int var1, int var2);

   public String toString() {
      return "%s[%s]".formatted(this.getClass().getSimpleName(), this.location);
   }
}
