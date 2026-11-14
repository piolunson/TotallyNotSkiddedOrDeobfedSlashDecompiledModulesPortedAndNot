package net.hachimi.client.api.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.math.ColorHelper.Argb;
import org.joml.Matrix3f;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL11;

public class RenderBuffers {
   public static final Buffer QUADS;
   public static final Buffer TEXTURE_QUADS;
   public static final Buffer LINES;
   private static final List<Runnable> postRenderCallbacks;
   private static boolean isSetup;

   public static void preRender() {
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.disableDepthTest();
      isSetup = true;
   }

   public static void postRender() {
      isSetup = false;
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      GL11.glDisable(2848);

      for(Runnable callback : postRenderCallbacks) {
         callback.run();
      }

      postRenderCallbacks.clear();
   }

   public static void post(Runnable callback) {
      if (isSetup) {
         postRenderCallbacks.add(callback);
      } else {
         callback.run();
      }

   }

   private static Matrix4d toMatrix4d(Matrix4f matrix4f) {
      return new Matrix4d((double)matrix4f.m00(), (double)matrix4f.m01(), (double)matrix4f.m02(), (double)matrix4f.m03(), (double)matrix4f.m10(), (double)matrix4f.m11(), (double)matrix4f.m12(), (double)matrix4f.m13(), (double)matrix4f.m20(), (double)matrix4f.m21(), (double)matrix4f.m22(), (double)matrix4f.m23(), (double)matrix4f.m30(), (double)matrix4f.m31(), (double)matrix4f.m32(), (double)matrix4f.m33());
   }

   static {
      QUADS = new Buffer(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      TEXTURE_QUADS = new Buffer(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      LINES = new Buffer(net.minecraft.client.render.VertexFormat.DrawMode.LINES, VertexFormats.LINES);
      postRenderCallbacks = new ArrayList<>();
      isSetup = false;
   }

   public static class Buffer {
      public BufferBuilder buffer;
      private final net.minecraft.client.render.VertexFormat.DrawMode drawMode;
      private final net.minecraft.client.render.VertexFormat vertexFormat;
      private Matrix4d positionMatrix;
      private Matrix3f normalMatrix;
      private int color;

      public Buffer(net.minecraft.client.render.VertexFormat.DrawMode drawMode, net.minecraft.client.render.VertexFormat vertexFormat) {
         this.drawMode = drawMode;
         this.vertexFormat = vertexFormat;
         this.color = -1;
      }

      public void begin(MatrixStack stack) {
         this.updateMatrices(stack);
         this.buffer = Tessellator.getInstance().begin(this.drawMode, this.vertexFormat);
      }

      public void updateMatrices(MatrixStack stack) {
         this.positionMatrix = RenderBuffers.toMatrix4d(stack.peek().getPositionMatrix());
         this.normalMatrix = stack.peek().getNormalMatrix();
         Vec3d pos = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().camera.getPos();
         this.positionMatrix.translate(-pos.x, -pos.y, -pos.z);
      }

      public void end() {
         this.draw();
      }

      public Buffer vertex(double x, double y, double z) {
         if (this.buffer == null) {
            return this;
         } else {
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x, y, z, (double)1.0F));
            this.buffer.vertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z()).color(net.minecraft.util.math.ColorHelper.Argb.getRed(this.color), net.minecraft.util.math.ColorHelper.Argb.getGreen(this.color), net.minecraft.util.math.ColorHelper.Argb.getBlue(this.color), net.minecraft.util.math.ColorHelper.Argb.getAlpha(this.color));
            return this;
         }
      }

      public Buffer vertexTex(double x, double y, double z, float u, float v) {
         if (this.buffer == null) {
            return this;
         } else {
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x, y, z, (double)1.0F));
            this.buffer.vertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z()).texture(u, v).color(net.minecraft.util.math.ColorHelper.Argb.getRed(this.color), net.minecraft.util.math.ColorHelper.Argb.getGreen(this.color), net.minecraft.util.math.ColorHelper.Argb.getBlue(this.color), net.minecraft.util.math.ColorHelper.Argb.getAlpha(this.color));
            return this;
         }
      }

      public Buffer vertexLine(double x1, double y1, double z1, double x2, double y2, double z2) {
         if (this.buffer == null) {
            return this;
         } else {
            float k = (float)(x2 - x1);
            float l = (float)(y2 - y1);
            float m = (float)(z2 - z1);
            float n = MathHelper.sqrt(k * k + l * l + m * m);
            k /= n;
            l /= n;
            m /= n;
            Vector3f vector3f = this.normalMatrix.transform(k, l, m, new Vector3f()).normalize();
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x1, y1, z1, (double)1.0F));
            this.buffer.vertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z()).normal(vector3f.x, vector3f.y, vector3f.z).color(net.minecraft.util.math.ColorHelper.Argb.getRed(this.color), net.minecraft.util.math.ColorHelper.Argb.getGreen(this.color), net.minecraft.util.math.ColorHelper.Argb.getBlue(this.color), net.minecraft.util.math.ColorHelper.Argb.getAlpha(this.color));
            Vector4d vector4d2 = this.positionMatrix.transform(new Vector4d(x2, y2, z2, (double)1.0F));
            this.buffer.vertex((float)vector4d2.x(), (float)vector4d2.y(), (float)vector4d2.z()).normal(vector3f.x, vector3f.y, vector3f.z).color(net.minecraft.util.math.ColorHelper.Argb.getRed(this.color), net.minecraft.util.math.ColorHelper.Argb.getGreen(this.color), net.minecraft.util.math.ColorHelper.Argb.getBlue(this.color), net.minecraft.util.math.ColorHelper.Argb.getAlpha(this.color));
            return this;
         }
      }

      public void color(int color) {
         this.color = color;
      }

      public void draw() {
         if (this.buffer != null) {
            if (this.vertexFormat == VertexFormats.LINES) {
               RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
            } else if (this.vertexFormat == VertexFormats.POSITION_COLOR) {
               RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            } else if (this.vertexFormat == VertexFormats.POSITION_TEXTURE) {
               RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            }

            BuiltBuffer builtBuffer = this.buffer.endNullable();
            if (builtBuffer != null) {
               BufferRenderer.drawWithGlobalProgram(builtBuffer);
               this.buffer = null;
            }

         }
      }
   }
}
