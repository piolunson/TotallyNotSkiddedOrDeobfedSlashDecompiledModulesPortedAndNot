package net.hachimi.client.util.render;

import java.awt.Color;

public class ColorUtil {
   public static Color hslToColor(float f, float f2, float f3, float f4) {
      if (!(f2 < 0.0F) && !(f2 > 100.0F)) {
         if (!(f3 < 0.0F) && !(f3 > 100.0F)) {
            if (!(f4 < 0.0F) && !(f4 > 1.0F)) {
               f %= 360.0F;
               float f5 = 0.0F;
               float var8;
               f5 = (double)f3 < (double)0.5F ? f3 * (1.0F + f2) : (f3 /= 100.0F) + (var8 = f2 / 100.0F) - var8 * f3;
               f2 = 2.0F * f3 - f5;
               float var7;
               f3 = Math.max(0.0F, colorCalc(f2, f5, (var7 = f / 360.0F) + 0.33333334F));
               float f6 = Math.max(0.0F, colorCalc(f2, f5, var7));
               f2 = Math.max(0.0F, colorCalc(f2, f5, var7 - 0.33333334F));
               f3 = Math.min(f3, 1.0F);
               f6 = Math.min(f6, 1.0F);
               f2 = Math.min(f2, 1.0F);
               return new Color(f3, f6, f2, f4);
            } else {
               throw new IllegalArgumentException("Color parameter outside of expected range - Alpha");
            }
         } else {
            throw new IllegalArgumentException("Color parameter outside of expected range - Lightness");
         }
      } else {
         throw new IllegalArgumentException("Color parameter outside of expected range - Saturation");
      }
   }

   public static Color interpolateColor(float value, Color start, Color end) {
      float sr = (float)start.getRed() / 255.0F;
      float sg = (float)start.getGreen() / 255.0F;
      float sb = (float)start.getBlue() / 255.0F;
      float sa = (float)start.getAlpha() / 255.0F;
      float er = (float)end.getRed() / 255.0F;
      float eg = (float)end.getGreen() / 255.0F;
      float eb = (float)end.getBlue() / 255.0F;
      float ea = (float)end.getAlpha() / 255.0F;
      return new Color(sr * value + er * (1.0F - value), sg * value + eg * (1.0F - value), sb * value + eb * (1.0F - value), sa * value + ea * (1.0F - value));
   }

   private static float colorCalc(float f, float f2, float f3) {
      if (f3 < 0.0F) {
         ++f3;
      }

      if (f3 > 1.0F) {
         --f3;
      }

      if (6.0F * f3 < 1.0F) {
         return f + (f2 - f) * 6.0F * f3;
      } else if (2.0F * f3 < 1.0F) {
         return f2;
      } else {
         return 3.0F * f3 < 2.0F ? f + (f2 - f) * 6.0F * (0.6666667F - f3) : f;
      }
   }

   public static int withAlpha(int color, float opacity) {
      return withAlpha(color, (int)(255.0F * opacity));
   }

   public static int withAlpha(int color, int alpha) {
      int red = 255 & color >> 16;
      int blue = 255 & color;
      int green = 255 & color >> 8;
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   public static int fixTransparency(int color, float alpha) {
      if (alpha == 1.0F) {
         return color;
      } else {
         float colorAlpha = (float)(color >> 24 & 255);
         alpha = Math.max(0.0F, Math.min(1.0F, alpha));
         int colorAlphaInt = Math.max(10, (int)(colorAlpha * alpha));
         return colorAlphaInt << 24 | color & 16777215;
      }
   }
}
