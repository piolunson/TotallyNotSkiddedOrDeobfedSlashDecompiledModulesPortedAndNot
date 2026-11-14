package net.hachimi.client.impl.font;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.hachimi.client.mixin.accessor.AccessorTextRenderer;
import net.hachimi.client.util.Globals;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.EmptyGlyphRenderer;
import net.minecraft.client.font.FontLoader;
import net.minecraft.client.font.Font;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.TextColor;
import net.minecraft.client.font.TrueTypeFontLoader;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.font.TrueTypeFontLoader.Shift;
import net.minecraft.client.font.FontFilterType.FilterMap;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/** @deprecated */
@Deprecated
public class VanillaTextRenderer implements Globals {
   public static net.minecraft.client.font.TextRenderer getTextRender(String fontName) throws IOException {
      List<net.minecraft.client.font.Font.FontFilterPair> list = Lists.newArrayList();
      net.minecraft.client.font.TrueTypeFontLoader loader = new net.minecraft.client.font.TrueTypeFontLoader(Identifier.of(String.format("hachimi:%s.ttf", fontName)), 11.0F, 20.0F, net.minecraft.client.font.TrueTypeFontLoader.Shift.NONE, "");
      net.minecraft.client.font.FontLoader.Loadable loadable = (net.minecraft.client.font.FontLoader.Loadable)loader.build().orThrow();
      net.minecraft.client.font.Font font = loadable.load(mc.getResourceManager());
      list.add(new net.minecraft.client.font.Font.FontFilterPair(font, net.minecraft.client.font.FontFilterType.FilterMap.NO_FILTER));
      FontStorage storage = new FontStorage(mc.getTextureManager(), Identifier.of("hachimi:tr"));
      storage.setFonts(list, Collections.emptySet());
      return new net.minecraft.client.font.TextRenderer((id) -> storage, true);
   }

   public void drawWithShadow(MatrixStack matrices, String text, float x, float y, int color) {
      this.draw(matrices, text, x + 1.0F, y + 1.0F, color, true);
      this.draw(matrices, text, x, y, color, false);
   }

   public void draw(MatrixStack matrices, String text, float x, float y, int color, boolean shadow) {
      this.draw(text, x, y, color, matrices.peek().getPositionMatrix(), shadow);
   }

   private void draw(String text, float x, float y, int color, Matrix4f matrix, boolean shadow) {
      if (text != null) {
         net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = net.minecraft.client.render.VertexConsumerProvider.immediate(new BufferAllocator(1536));
         this.draw(text, x, y, color, shadow, matrix, immediate, net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 15728880);
         immediate.draw();
      }
   }

   public void draw(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, net.minecraft.client.render.VertexConsumerProvider vertexConsumers, net.minecraft.client.font.TextRenderer.TextLayerType layerType, int backgroundColor, int light) {
      this.drawInternal(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light);
   }

   private void drawInternal(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, net.minecraft.client.render.VertexConsumerProvider vertexConsumers, net.minecraft.client.font.TextRenderer.TextLayerType layerType, int backgroundColor, int light) {
      Matrix4f matrix4f = new Matrix4f(matrix);
      this.drawLayer(text, x, y, color, shadow, matrix4f, vertexConsumers, layerType, backgroundColor, light);
   }

   private void drawLayer(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, net.minecraft.client.render.VertexConsumerProvider vertexConsumerProvider, net.minecraft.client.font.TextRenderer.TextLayerType layerType, int underlineColor, int light) {
      Drawer drawer = new Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, layerType, light);
      TextVisitFactory.visitFormatted(text, Style.EMPTY, drawer);
      drawer.drawLayer();
   }

   public static class Drawer implements CharacterVisitor {
      final net.minecraft.client.render.VertexConsumerProvider vertexConsumers;
      private final float brightnessMultiplier;
      private final float red;
      private final float green;
      private final float blue;
      private final float alpha;
      private final Matrix4f matrix;
      private final net.minecraft.client.font.TextRenderer.TextLayerType layerType;
      private final int light;
      float x;
      float y;
      private @Nullable List<net.minecraft.client.font.GlyphRenderer.Rectangle> rectangles;

      public Drawer(net.minecraft.client.render.VertexConsumerProvider vertexConsumers, float x, float y, int color, boolean shadow, Matrix4f matrix, net.minecraft.client.font.TextRenderer.TextLayerType layerType, int light) {
         this.vertexConsumers = vertexConsumers;
         this.x = x;
         this.y = y;
         this.brightnessMultiplier = shadow ? 0.25F : 1.0F;
         this.red = (float)(color >> 16 & 255) / 255.0F * this.brightnessMultiplier;
         this.green = (float)(color >> 8 & 255) / 255.0F * this.brightnessMultiplier;
         this.blue = (float)(color & 255) / 255.0F * this.brightnessMultiplier;
         this.alpha = (float)(color >> 24 & 255) / 255.0F;
         this.matrix = matrix;
         this.layerType = layerType;
         this.light = light;
      }

      public boolean accept(int i, Style style, int j) {
         FontStorage fontStorage = ((AccessorTextRenderer)(Object)Globals.mc.textRenderer).hookGetFontStorage(style.getFont());
         Glyph glyph = fontStorage.getGlyph(j, ((AccessorTextRenderer)(Object)Globals.mc.textRenderer).hookGetValidateAdvance());
         net.minecraft.client.font.GlyphRenderer glyphRenderer = style.isObfuscated() && j != 32 ? fontStorage.getObfuscatedGlyphRenderer(glyph) : fontStorage.getGlyphRenderer(j);
         boolean bl = style.isBold();
         float f = this.alpha;
         TextColor textColor = style.getColor();
         float l;
         float h;
         float g;
         if (textColor != null) {
            int k = textColor.getRgb();
            g = (float)(k >> 16 & 255) / 255.0F * this.brightnessMultiplier;
            h = (float)(k >> 8 & 255) / 255.0F * this.brightnessMultiplier;
            l = (float)(k & 255) / 255.0F * this.brightnessMultiplier;
         } else {
            g = this.red;
            h = this.green;
            l = this.blue;
         }

         if (!(glyphRenderer instanceof EmptyGlyphRenderer)) {
            float m = bl ? glyph.getBoldOffset() : 0.0F;
            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(glyphRenderer.getLayer(this.layerType));
            ((AccessorTextRenderer)(Object)Globals.mc.textRenderer).hookDrawGlyph(glyphRenderer, bl, style.isItalic(), m, this.x, this.y, this.matrix, vertexConsumer, g, h, l, f, this.light);
         }

         float m = glyph.getAdvance(bl);
         this.x += m;
         return true;
      }

      public void drawLayer() {
         if (this.rectangles != null) {
            net.minecraft.client.font.GlyphRenderer glyphRenderer = ((AccessorTextRenderer)(Object)Globals.mc.textRenderer).hookGetFontStorage(Style.DEFAULT_FONT_ID).getRectangleRenderer();
            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(glyphRenderer.getLayer(this.layerType));

            for(net.minecraft.client.font.GlyphRenderer.Rectangle rectangle : this.rectangles) {
               glyphRenderer.drawRectangle(rectangle, this.matrix, vertexConsumer, this.light);
            }
         }

      }
   }
}
