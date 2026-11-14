package net.hachimi.client.impl.font;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.awt.Color;
import java.awt.Font;
import java.io.Closeable;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.hachimi.client.api.font.Glyph;
import net.hachimi.client.api.font.GlyphCache;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.client.FontModule;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.math.HexRandom;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;

public final class AWTFontRenderer implements Closeable, Globals {
   private Font font;
   private final float size;
   private int scale;
   private int lastScale;
   private static final Pattern PATTERN_CONTROL_CODE = Pattern.compile("(?i)\\u00A7[0-9A-FK-OG]");
   private final ObjectList<GlyphCache> caches = new ObjectArrayList();
   private final Char2ObjectArrayMap<Glyph> glyphs = new Char2ObjectArrayMap();
   private final Map<Identifier, ObjectList<CharLocation>> cache = new Object2ObjectOpenHashMap();

   public AWTFontRenderer(InputStream inputStream, float size) {
      try {
         this.font = Font.createFont(0, inputStream);
      } catch (Throwable t) {
         t.printStackTrace();
         this.font = new Font("Verdana", 0, Math.round(size));
      }

      this.size = size;
      this.createFont(this.font, size);
   }

   public static String stripControlCodes(String text) {
      return PATTERN_CONTROL_CODE.matcher(text).replaceAll("");
   }

   private void createFont(Font font, float size) {
      this.lastScale = (int)mc.getWindow().getScaleFactor();
      this.scale = this.lastScale;
      this.font = font.deriveFont(size * (float)this.scale);
   }

   public void drawStringWithShadow(MatrixStack stack, String text, double x, double y, int color) {
      this.drawString(stack, text, x + (double)0.5F, y + (double)0.5F, color, true);
      this.drawString(stack, text, x, y, color, false);
   }

   public void drawString(MatrixStack stack, String text, double x, double y, int color) {
      this.drawString(stack, text, x, y, color, false);
   }

   private void drawString(MatrixStack stack, String text, double x, double y, int color, boolean shadow) {
      float brightnessMultiplier = shadow ? 0.25F : 1.0F;
      float r = (float)(color >> 16 & 255) / 255.0F * brightnessMultiplier;
      float g = (float)(color >> 8 & 255) / 255.0F * brightnessMultiplier;
      float b = (float)(color & 255) / 255.0F * brightnessMultiplier;
      float a = (float)(color >> 24 & 255) / 255.0F;
      this.drawString(stack, text, (float)x, (float)y, r, g, b, a, brightnessMultiplier);
   }

   public void drawString(MatrixStack stack, String text, double x, double y, Color color) {
      this.drawString(stack, text, x, y, color, false);
   }

   public void drawString(MatrixStack stack, String text, double x, double y, Color color, boolean shadow) {
      float brightnessMultiplier = shadow ? 0.25F : 1.0F;
      this.drawString(stack, text, (float)x, (float)y, (float)color.getRed() / 255.0F * brightnessMultiplier, (float)color.getGreen() / 255.0F * brightnessMultiplier, (float)color.getBlue() / 255.0F * brightnessMultiplier, (float)color.getAlpha(), brightnessMultiplier);
   }

   public void drawString(MatrixStack stack, String text, float x, float y, float r, float g, float b, float a, float brightnessMultiplier) {
      int currentScale = (int)mc.getWindow().getScaleFactor();
      if (currentScale != this.lastScale) {
         this.close();
         this.createFont(this.font, this.size);
      }

      float r2 = r;
      float g2 = g;
      float b2 = b;
      stack.push();
      y -= 3.0F;
      stack.translate(x, y, 0.0F);
      stack.scale(1.0F / (float)this.scale, 1.0F / (float)this.scale, 0.0F);
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      Tessellator tessellator = Tessellator.getInstance();
      Matrix4f matrix4f = stack.peek().getPositionMatrix();
      char[] chars = text.toCharArray();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      boolean formatting = false;
      int lineStart = 0;
      this.glyphs.clear();
      synchronized(this.cache) {
         for(int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if (formatting) {
               formatting = false;
               if (c == 'r') {
                  r2 = r;
                  g2 = g;
                  b2 = b;
               } else {
                  int colorCode = this.getColorFromCode(c);
                  int[] col = this.toRgbComponents(colorCode);
                  r2 = (float)col[0] / 255.0F * brightnessMultiplier;
                  g2 = (float)col[1] / 255.0F * brightnessMultiplier;
                  b2 = (float)col[2] / 255.0F * brightnessMultiplier;
               }
            } else if (c == 167) {
               formatting = true;
            } else if (c == '\n') {
               yOffset += this.getStringHeight(text.substring(lineStart, i)) * (float)this.scale;
               xOffset = 0.0F;
               lineStart = i + 1;
            } else {
               Glyph glyph = (Glyph)this.glyphs.computeIfAbsent(c, (g1) -> this.getGlyphFromChar(g1));
               if (glyph != null) {
                  if (glyph.value() != ' ') {
                     Identifier i1 = glyph.owner().getId();
                     CharLocation entry = new CharLocation(xOffset, yOffset, r2, g2, b2, glyph);
                     ((ObjectList)this.cache.computeIfAbsent(i1, (integer) -> new ObjectArrayList())).add(entry);
                  }

                  xOffset += (float)glyph.width();
               }
            }
         }

         Iterator var45 = this.cache.keySet().iterator();

         label68:
         while(true) {
            Identifier identifier;
            while(true) {
               if (!var45.hasNext()) {
                  this.cache.clear();
                  break label68;
               }

               identifier = (Identifier)var45.next();
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableCull();
               RenderSystem.texParameter(3553, 10241, 9729);
               RenderSystem.texParameter(3553, 10240, 9729);

               try {
                  RenderSystem.setShaderTexture(0, identifier);
                  break;
               } catch (Exception var42) {
               }
            }

            List<CharLocation> objects = (List)this.cache.get(identifier);
            BufferBuilder bufferBuilder = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for(CharLocation object : objects) {
               float xo = object.x;
               float yo = object.y;
               float cr = object.r;
               float cg = object.g;
               float cb = object.b;
               Glyph glyph = object.glyph;
               GlyphCache owner = glyph.owner();
               float w = (float)glyph.width();
               float h = (float)glyph.height();
               float u1 = (float)glyph.textureWidth() / (float)owner.getWidth();
               float v1 = (float)glyph.textureHeight() / (float)owner.getHeight();
               float u2 = (float)(glyph.textureWidth() + glyph.width()) / (float)owner.getWidth();
               float v2 = (float)(glyph.textureHeight() + glyph.height()) / (float)owner.getHeight();
               bufferBuilder.vertex(matrix4f, xo + 0.0F, yo + h, 0.0F).color(cr, cg, cb, a).texture(u1, v2);
               bufferBuilder.vertex(matrix4f, xo + w, yo + h, 0.0F).color(cr, cg, cb, a).texture(u2, v2);
               bufferBuilder.vertex(matrix4f, xo + w, yo + 0.0F, 0.0F).color(cr, cg, cb, a).texture(u2, v1);
               bufferBuilder.vertex(matrix4f, xo + 0.0F, yo + 0.0F, 0.0F).color(cr, cg, cb, a).texture(u1, v1);
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.disableBlend();
         }
      }

      stack.pop();
   }

   public void drawCenteredString(MatrixStack stack, String text, double x, double y, int color) {
      this.drawString(stack, text, x, y, color, false);
   }

   public void drawCenteredString(MatrixStack stack, String text, double x, double y, int color, boolean shadow) {
      float brightnessMultiplier = shadow ? 0.25F : 1.0F;
      float r = (float)(color >> 16 & 255) / 255.0F * brightnessMultiplier;
      float g = (float)(color >> 8 & 255) / 255.0F * brightnessMultiplier;
      float b = (float)(color & 255) / 255.0F * brightnessMultiplier;
      float a = (color & -16777216) != -16777216 ? 1.0F : (float)(color >> 24 & 255) / 255.0F * brightnessMultiplier;
      this.drawString(stack, text, (float)(x - (double)(this.getStringWidth(text) / 2.0F)), (float)y, r, g, b, a, brightnessMultiplier);
   }

   public void drawCenteredString(MatrixStack stack, String text, double x, double y, Color color) {
      this.drawString(stack, text, x, y, color, false);
   }

   public void drawCenteredString(MatrixStack stack, String text, double x, double y, Color color, boolean shadow) {
      float brightnessMultiplier = shadow ? 0.25F : 1.0F;
      this.drawString(stack, text, (float)(x - (double)(this.getStringWidth(text) / 2.0F)), (float)y, (float)color.getRed() / 255.0F * brightnessMultiplier, (float)color.getGreen() / 255.0F * brightnessMultiplier, (float)color.getBlue() / 255.0F * brightnessMultiplier, (float)color.getAlpha() / 255.0F, brightnessMultiplier);
   }

   public float getStringWidth(String text) {
      char[] c = stripControlCodes(text).toCharArray();
      float currentLine = 0.0F;
      float maxPreviousLines = 0.0F;

      for(char c1 : c) {
         if (c1 == '\n') {
            maxPreviousLines = Math.max(currentLine, maxPreviousLines);
            currentLine = 0.0F;
         } else {
            Glyph glyph = (Glyph)this.glyphs.computeIfAbsent(c1, (g1) -> this.getGlyphFromChar(g1));
            float w = glyph == null ? 0.0F : (float)glyph.width();
            currentLine += w / (float)this.scale;
         }
      }

      return Math.max(currentLine, maxPreviousLines);
   }

   public float getStringHeight(String text) {
      char[] c = stripControlCodes(text).toCharArray();
      if (c.length == 0) {
         c = new char[]{' '};
      }

      float currentLine = 0.0F;
      float previous = 0.0F;

      for(char c1 : c) {
         if (c1 == '\n') {
            if (currentLine == 0.0F) {
               Glyph glyph = (Glyph)this.glyphs.computeIfAbsent(' ', (g1) -> this.getGlyphFromChar(g1));
               currentLine = (float)glyph.height() / (float)this.scale;
            }

            previous += currentLine;
            currentLine = 0.0F;
         } else {
            Glyph glyph = (Glyph)this.glyphs.computeIfAbsent(c1, (g1) -> this.getGlyphFromChar(g1));
            float h = glyph == null ? 0.0F : (float)glyph.height();
            currentLine = Math.max(h / (float)this.scale, currentLine);
         }
      }

      return currentLine + previous;
   }

   public float getFontHeight() {
      return this.size;
   }

   private Glyph getGlyphFromChar(char c) {
      ObjectListIterator var2 = this.caches.iterator();

      while(var2.hasNext()) {
         GlyphCache map = (GlyphCache)var2.next();
         if (map.contains(c)) {
            return map.getGlyph(c);
         }
      }

      int base = 256 * (int)Math.floor((double)c / (double)256.0F);
      GlyphCache glyphCache = new GlyphCache((char)base, (char)(base + 256), this.font, this.getGlyphIdentifier(), 5, FontModule.getInstance().getAntiAlias(), FontModule.getInstance().getFractionalMetrics());
      this.caches.add(glyphCache);
      return glyphCache.getGlyph(c);
   }

   public void close() {
      try {
         ObjectListIterator var1 = this.caches.iterator();

         while(var1.hasNext()) {
            GlyphCache cache1 = (GlyphCache)var1.next();
            cache1.clear();
         }

         this.caches.clear();
         this.glyphs.clear();
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public Identifier getGlyphIdentifier() {
      return Identifier.of("hachimi", "font/storage/" + HexRandom.generateRandomHex(32));
   }

   public int[] toRgbComponents(int color) {
      float r = (float)(color >> 16 & 255);
      float g = (float)(color >> 8 & 255);
      float b = (float)(color & 255);
      float a = (color & -16777216) != -16777216 ? 255.0F : (float)(color >> 24 & 255);
      return new int[]{(int)r, (int)g, (int)b, (int)a};
   }

   private int getColorFromCode(char code) {
      int var10000;
      switch (code) {
         case '0':
            var10000 = Color.BLACK.getRGB();
            break;
         case '1':
            var10000 = -16777046;
            break;
         case '2':
            var10000 = -16733696;
            break;
         case '3':
            var10000 = -16733526;
            break;
         case '4':
            var10000 = -5636096;
            break;
         case '5':
            var10000 = -5635926;
            break;
         case '6':
            var10000 = -22016;
            break;
         case '7':
            var10000 = -5592406;
            break;
         case '8':
            var10000 = -11184811;
            break;
         case '9':
            var10000 = -11184641;
            break;
         case ':':
         case ';':
         case '<':
         case '=':
         case '>':
         case '?':
         case '@':
         case 'A':
         case 'B':
         case 'C':
         case 'D':
         case 'E':
         case 'F':
         case 'G':
         case 'H':
         case 'I':
         case 'J':
         case 'K':
         case 'L':
         case 'M':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'R':
         case 'S':
         case 'T':
         case 'U':
         case 'V':
         case 'W':
         case 'X':
         case 'Y':
         case 'Z':
         case '[':
         case '\\':
         case ']':
         case '^':
         case '_':
         case '`':
         case 'h':
         case 'i':
         case 'j':
         case 'k':
         case 'l':
         case 'm':
         case 'n':
         case 'o':
         case 'p':
         case 'q':
         case 'r':
         default:
            var10000 = -1;
            break;
         case 'a':
            var10000 = -11141291;
            break;
         case 'b':
            var10000 = -11141121;
            break;
         case 'c':
            var10000 = -43691;
            break;
         case 'd':
            var10000 = -43521;
            break;
         case 'e':
            var10000 = -171;
            break;
         case 'f':
            var10000 = -1;
            break;
         case 'g':
            var10000 = SocialsModule.getInstance().getFriendRGB();
            break;
         case 's':
            var10000 = ColorsModule.getInstance().getRGB();
      }

      return var10000;
   }

   public static record CharLocation(float x, float y, float r, float g, float b, Glyph glyph) {
   }
}
