package net.hachimi.client.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public final class TextureUtil {
   private TextureUtil() {
   }

   public static void drawScaled(DrawContext context, Identifier texture, int x, int y, int width, int height) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      RenderSystem.setShaderTexture(0, texture);
      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
      BufferBuilder buffer = Tessellator.getInstance().begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      buffer.vertex(matrix, (float)x, (float)y, 0.0F).texture(0.0F, 0.0F);
      buffer.vertex(matrix, (float)x, (float)(y + height), 0.0F).texture(0.0F, 1.0F);
      buffer.vertex(matrix, (float)(x + width), (float)(y + height), 0.0F).texture(1.0F, 1.0F);
      buffer.vertex(matrix, (float)(x + width), (float)y, 0.0F).texture(1.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }
}
