package net.hachimi.client.mixin.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.lang.reflect.Field;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SplashOverlay.class})
public class MixinLoadingOverlay {
   @Unique
   private static final int FRAME_COUNT = 48;
   @Unique
   private static final int FRAME_W = 960;
   @Unique
   private static final int FRAME_H = 540;
   @Unique
   private static final int FRAME_TIME_MS = 85;
   @Unique
   private static final int HOLD_FRAMES = 24;
   @Unique
   private static final Identifier[] FRAME_IDS = new Identifier[48];
   @Unique
   private static final boolean[] REGISTERED = new boolean[48];
   @Unique
   private long startTimeMs = -1L;

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void hookRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      int sw = mc.getWindow().getScaledWidth();
      int sh = mc.getWindow().getScaledHeight();
      if (this.startTimeMs < 0L) {
         this.startTimeMs = System.currentTimeMillis();
      }

      float progress = this.getProgress();
      context.fill(0, 0, sw, sh, -16777216);
      int frameIdx = this.getFrameIndex();
      this.ensureRegistered(mc, frameIdx);
      float scale = Math.max((float)sw / 960.0F, (float)sh / 540.0F);
      int imgW = Math.round(960.0F * scale);
      int imgH = Math.round(540.0F * scale);
      int imgX = (sw - imgW) / 2;
      int imgY = (sh - imgH) / 2;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      context.drawTexture(FRAME_IDS[frameIdx], imgX, imgY, imgW, imgH, 0.0F, 0.0F, 960, 540, 960, 540);
      RenderSystem.disableBlend();
      this.drawProgressBar(context, sw, sh, progress);
   }

   @Unique
   private int getFrameIndex() {
      long elapsed = System.currentTimeMillis() - this.startTimeMs;
      long frame = Math.floorDiv(elapsed, 85);
      int sequenceFrame = Math.floorMod(frame, 72);
      return Math.min(sequenceFrame, 47);
   }

   @Unique
   private void ensureRegistered(MinecraftClient mc, int idx) {
      if (!REGISTERED[idx]) {
         try {
            String path = String.format("/assets/hachimiclient/textures/gui/loading/frame_%03d.png", idx);
            InputStream stream = MixinLoadingOverlay.class.getResourceAsStream(path);
            if (stream != null) {
               NativeImage img = NativeImage.read(stream);
               NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
               mc.getTextureManager().registerTexture(FRAME_IDS[idx], tex);
               stream.close();
            }
         } catch (Exception var7) {
         }

         REGISTERED[idx] = true;
      }
   }

   @Unique
   private float getProgress() {
      try {
         for(Field f : SplashOverlay.class.getDeclaredFields()) {
            if (f.getType() == Float.TYPE) {
               f.setAccessible(true);
               float val = (Float)f.get(this);
               if (val >= 0.0F && val <= 1.0F) {
                  return val;
               }
            }
         }
      } catch (Exception var6) {
      }

      return Math.min(1.0F, (float)(System.currentTimeMillis() - this.startTimeMs) / 5000.0F);
   }

   @Unique
   private void drawProgressBar(DrawContext context, int sw, int sh, float progress) {
      float eased = 1.0F - (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F)) * (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F)) * (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F));
      int maxW = Math.max(24, Math.min(620, sw - 42));
      int minW = Math.min(150, maxW);
      int barW = MathHelper.clamp(Math.round((float)sw * 0.56F), minW, maxW);
      int barX = (sw - barW) / 2;
      int barY = Math.round((float)sh * 0.76F);
      int barH = Math.max(1, Math.round((float)sh * 0.003F));
      int fillW = MathHelper.clamp(Math.round((float)barW * eased), 0, barW);
      context.fill(barX, barY, barX + barW, barY + barH, withAlpha(1443880, 1.0F));
      if (fillW > 0) {
         context.fillGradient(barX, barY, barX + fillW, barY + barH, -9814112, -5210113);
         context.fill(barX, barY - Math.max(1, barH + 2), barX + fillW, barY, withAlpha(9847008, 0.16F));
         context.fill(barX, barY + barH, barX + fillW, barY + barH + Math.max(1, barH + 2), withAlpha(9847008, 0.13F));
      }

   }

   @Unique
   private static int withAlpha(int rgb, float opacity) {
      int alpha = MathHelper.clamp(Math.round(opacity * 255.0F), 0, 255);
      return alpha << 24 | rgb & 16777215;
   }

   static {
      for(int i = 0; i < 48; ++i) {
         FRAME_IDS[i] = Identifier.of("hachimiclient", String.format("textures/gui/loading/frame_%03d", i));
      }

   }
}
