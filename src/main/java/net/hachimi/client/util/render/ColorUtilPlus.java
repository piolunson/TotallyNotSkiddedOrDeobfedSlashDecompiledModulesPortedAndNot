package net.hachimi.client.util.render;

import java.awt.Color;

public class ColorUtilPlus {
   public static Color fadeColor(Color start, Color end, double progress) {
      progress = Math.min(Math.max(progress, (double)0.0F), (double)1.0F);
      return new Color(Math.min((int)((double)start.getRed() + (double)(end.getRed() - start.getRed()) * progress), 255), Math.min((int)((double)start.getGreen() + (double)(end.getGreen() - start.getGreen()) * progress), 255), Math.min((int)((double)start.getBlue() + (double)(end.getBlue() - start.getBlue()) * progress), 255), Math.min((int)((double)start.getAlpha() + (double)(end.getAlpha() - start.getAlpha()) * progress), 255));
   }

   public static Color pulseColor(Color startColor, Color endColor, double index, int count, double speed) {
      double t = ((double)System.currentTimeMillis() * speed + index / (double)Math.max(count, 1) * (double)1000.0F) % (double)2000.0F;
      double quad = t < (double)1000.0F ? t / (double)1000.0F : ((double)2000.0F - t) / (double)1000.0F;
      return fadeColor(startColor, endColor, quad);
   }

   public static Color pulseColor(Color color, double index, int count, double speed) {
      float[] hsb = new float[3];
      Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
      double t = ((double)System.currentTimeMillis() * speed + index / (double)Math.max(count, 1) * (double)1000.0F) % (double)2000.0F;
      float bri = (float)(t < (double)1000.0F ? t / (double)1000.0F : ((double)2000.0F - t) / (double)1000.0F);
      bri = 0.4F + bri * 0.6F;
      int rgb = Color.HSBtoRGB(hsb[0], hsb[1], bri);
      return new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, color.getAlpha());
   }

   public static Color rainbowColor(double offset, double speed, float sat, float bri) {
      float hue = (float)((((double)System.currentTimeMillis() / speed + offset) % (double)1.0F + (double)1.0F) % (double)1.0F);
      int rgb = Color.HSBtoRGB(hue, sat, bri);
      return new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255);
   }

   public static Color rainbowColor(double offset, double speed, float sat, float bri, int alpha) {
      Color c = rainbowColor(offset, speed, sat, bri);
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
   }

   public static Color injectAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
   }

   public static int injectAlpha(int color, int alpha) {
      return color & 16777215 | Math.max(0, Math.min(255, alpha)) << 24;
   }

   public static Color hslToColor(float hue, float sat, float lit, float alpha) {
      if (!(sat < 0.0F) && !(sat > 100.0F)) {
         if (!(lit < 0.0F) && !(lit > 100.0F)) {
            if (!(alpha < 0.0F) && !(alpha > 1.0F)) {
               hue %= 360.0F;
               float var8;
               float f5 = (double)lit < (double)0.5F ? lit * (1.0F + sat) : (lit /= 100.0F) + (var8 = sat / 100.0F) - var8 * lit;
               sat = 2.0F * lit - f5;
               float var7;
               lit = Math.max(0.0F, colorCalc(sat, f5, (var7 = hue / 360.0F) + 0.33333334F));
               float f6 = Math.max(0.0F, colorCalc(sat, f5, var7));
               sat = Math.max(0.0F, colorCalc(sat, f5, var7 - 0.33333334F));
               return new Color(Math.min(lit, 1.0F), Math.min(f6, 1.0F), Math.min(sat, 1.0F), alpha);
            } else {
               throw new IllegalArgumentException("Alpha out of range");
            }
         } else {
            throw new IllegalArgumentException("Lightness out of range");
         }
      } else {
         throw new IllegalArgumentException("Saturation out of range");
      }
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

   public static Color interpolateColor(float value, Color start, Color end) {
      float sr = (float)start.getRed() / 255.0F;
      float er = (float)end.getRed() / 255.0F;
      float sg = (float)start.getGreen() / 255.0F;
      float eg = (float)end.getGreen() / 255.0F;
      float sb = (float)start.getBlue() / 255.0F;
      float eb = (float)end.getBlue() / 255.0F;
      float sa = (float)start.getAlpha() / 255.0F;
      float ea = (float)end.getAlpha() / 255.0F;
      return new Color(sr * value + er * (1.0F - value), sg * value + eg * (1.0F - value), sb * value + eb * (1.0F - value), sa * value + ea * (1.0F - value));
   }

   public static int withAlpha(int color, float opacity) {
      return withAlpha(color, (int)(255.0F * opacity));
   }

   public static int withAlpha(int color, int alpha) {
      return alpha << 24 | (color >> 16 & 255) << 16 | (color >> 8 & 255) << 8 | color & 255;
   }

   public static int fixTransparency(int color, float alpha) {
      if (alpha == 1.0F) {
         return color;
      } else {
         float colorAlpha = (float)(color >> 24 & 255);
         alpha = Math.max(0.0F, Math.min(1.0F, alpha));
         int a = Math.max(10, (int)(colorAlpha * alpha));
         return a << 24 | color & 16777215;
      }
   }
}
