package net.hachimi.client.mixin.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.util.Objects;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.render.satin.ManagedShaderEffect;
import net.hachimi.client.impl.gui.account.AccountSelectorScreen;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.render.TextureUtil;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.realms.gui.screen.RealmsNotificationsScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.AccessibilityOnboardingButtons;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.client.texture.NativeImage.Format;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TitleScreen.class})
public abstract class MixinTitleScreen extends Screen {
   @Shadow
   private @Nullable SplashTextRenderer splashText;
   @Shadow
   @Final
   public static Text COPYRIGHT;
   @Shadow
   private @Nullable RealmsNotificationsScreen realmsNotificationGui;
   @Shadow
   private long backgroundFadeStart;
   @Shadow
   @Final
   private boolean doBackgroundFade;
   @Unique
   private static final Identifier AURORA_BG_ID = Identifier.of("hachimiclient", "textures/background.png");
   @Unique
   private static boolean bgRegistered = false;
   @Unique
   private static float auroraTime = 0.0F;
   @Unique
   private static boolean shadersEnsured = false;
   @Unique
   private static boolean auroraErrorLogged = false;
   @Unique
   private static boolean bgTextureErrorLogged = false;
   @Unique
   private static final Identifier TITLE_LOGO_ID = Identifier.of("hachimi", "title_logo");
   @Unique
   private static final Identifier BLANK_ID = Identifier.of("hachimi", "blank");
   @Unique
   private static boolean titleLogoRegistered = false;
   @Unique
   private static boolean blankRegistered = false;

   @Shadow
   protected abstract void initWidgetsDemo(int var1, int var2);

   @Shadow
   protected abstract void initWidgetsNormal(int var1, int var2);

   @Shadow
   protected abstract boolean isRealmsNotificationsGuiDisplayed();

   @Unique
   private void drawAuroraBackground(DrawContext context) {
      if (!shadersEnsured) {
         Managers.SHADER.reloadShaders();
         shadersEnsured = true;
      }

      ManagedShaderEffect menuEffect = Managers.SHADER.getMenuShaderEffect();
      if (menuEffect != null) {
         try {
            float w = (float)this.client.getWindow().getScaledWidth();
            float h = (float)this.client.getWindow().getScaledHeight();
            float mouseX = (float)(this.client.mouse.getX() / (double)this.client.getWindow().getWidth());
            float mouseY = (float)(this.client.mouse.getY() / (double)this.client.getWindow().getHeight());
            menuEffect.setUniformValue("resolution", w, h);
            menuEffect.setUniformValue("mouse", mouseX, mouseY);
            menuEffect.setUniformValue("time", auroraTime);
            menuEffect.render(0.0F);
            auroraTime += 0.016F;
            return;
         } catch (Exception e) {
            if (!auroraErrorLogged) {
               Hachimi.error("Failed to render aurora menu shader, falling back to static background: {}", e.toString());
               auroraErrorLogged = true;
            }
         }
      }

      try {
         int w = context.getScaledWindowWidth();
         int h = context.getScaledWindowHeight();
         context.drawTexture(AURORA_BG_ID, 0, 0, 0.0F, 0.0F, w, h, w, h);
      } catch (Exception e) {
         if (!bgTextureErrorLogged) {
            Hachimi.error("Failed to draw fallback background texture {}: {}", AURORA_BG_ID, e.toString());
            bgTextureErrorLogged = true;
         }
      }

   }

   public MixinTitleScreen(Text title) {
      super(title);
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   public void hookRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
      float f = this.doBackgroundFade ? (float)(Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0F : 1.0F;
      float g = this.doBackgroundFade ? MathHelper.clamp(f - 1.0F, 0.0F, 1.0F) : 1.0F;
      int i = MathHelper.ceil(g * 255.0F) << 24;
      if ((i & -67108864) != 0) {
         TextRenderer var10001 = this.client.textRenderer;
         String var10002 = String.format("%s %s", "Hachimi", "2.0");
         int var10004 = this.height;
         Objects.requireNonNull(this.client.textRenderer);
         context.drawTextWithShadow(var10001, var10002, 2, var10004 - 9 * 2 - 2, 16777215 | i);
         this.renderHachimiLogo(context);
      }
   }

   @Unique
   private void renderHachimiLogo(DrawContext context) {
      if (!titleLogoRegistered) {
         try {
            InputStream stream = MixinTitleScreen.class.getResourceAsStream("/assets/hachimiclient/textures/purple.png");
            if (stream != null) {
               net.minecraft.client.texture.NativeImage img = net.minecraft.client.texture.NativeImage.read(stream);
               NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
               this.client.getTextureManager().registerTexture(TITLE_LOGO_ID, tex);
               stream.close();
            }
         } catch (Exception var8) {
         }

         titleLogoRegistered = true;
      }

      float aspect = 2.7526042F;
      int logoW = (int)MathHelper.clamp((float)this.width * 0.3F, 140.0F, 260.0F);
      int logoH = (int)((float)logoW / aspect);
      int logoX = (this.width - logoW) / 2;
      int buttonTopY = this.height / 4 + 48;
      int logoY = buttonTopY - logoH - 8;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      TextureUtil.drawScaled(context, TITLE_LOGO_ID, logoX, logoY, logoW, logoH);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   public void hookRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
      this.drawAuroraBackground(context);
   }

   @ModifyArg(
      method = {"render"},
      require = 0,
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFII)V"
),
      index = 0
   )
   private Identifier hideVanillaLogoNew(Identifier id) {
      return this.shouldHide(id) ? this.getBlankTexture() : id;
   }

   @ModifyArg(
      method = {"render"},
      require = 0,
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIIIII)V"
),
      index = 0
   )
   private Identifier hideVanillaLogo(Identifier id) {
      return this.shouldHide(id) ? this.getBlankTexture() : id;
   }

   @ModifyArg(
      method = {"render"},
      require = 0,
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIFFIIII)V"
),
      index = 0
   )
   private Identifier hideVanillaLogoAlt(Identifier id) {
      return this.shouldHide(id) ? this.getBlankTexture() : id;
   }

   @Unique
   private boolean shouldHide(Identifier id) {
      String path = id.getPath();
      return path.contains("title/minecraft") || path.contains("title/edition") || path.contains("title/minceraft");
   }

   @Unique
   private Identifier getBlankTexture() {
      if (!blankRegistered) {
         try {
            net.minecraft.client.texture.NativeImage img = new net.minecraft.client.texture.NativeImage(net.minecraft.client.texture.NativeImage.Format.RGBA, 1, 1, true);
            img.setColor(0, 0, 0);
            this.client.getTextureManager().registerTexture(BLANK_ID, new NativeImageBackedTexture(img));
         } catch (Exception var2) {
         }

         blankRegistered = true;
      }

      return BLANK_ID;
   }

   @Inject(
      method = {"init"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookInit(CallbackInfo ci) {
      ci.cancel();
      if (this.splashText == null) {
         this.splashText = this.client.getSplashTextLoader().get();
      }

      int i = this.textRenderer.getWidth(COPYRIGHT);
      int j = this.width - i - 2;
      int k = 24;
      int l = this.height / 4 + 48;
      if (this.client.isDemo()) {
         this.initWidgetsDemo(l, 24);
      } else {
         this.initWidgetsNormal(l, 24);
      }

      TextIconButtonWidget textIconButtonWidget = (TextIconButtonWidget)(Object)this.addDrawableChild(AccessibilityOnboardingButtons.createLanguageButton(20, (button) -> this.client.setScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager())), true));
      textIconButtonWidget.setPosition(this.width / 2 - 124, l + 72 + 24);
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.options"), (button) -> this.client.setScreen(new OptionsScreen(this, this.client.options))).dimensions(this.width / 2 - 100, l + 72 + 24, 98, 20).build());
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.quit"), (button) -> this.client.scheduleStop()).dimensions(this.width / 2 + 2, l + 72 + 24, 98, 20).build());
      TextIconButtonWidget textIconButtonWidget2 = (TextIconButtonWidget)(Object)this.addDrawableChild(AccessibilityOnboardingButtons.createAccessibilityButton(20, (button) -> this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options)), true));
      textIconButtonWidget2.setPosition(this.width / 2 + 104, l + 72 + 24);
      this.addDrawableChild(new PressableTextWidget(j, this.height - 10, i, 10, COPYRIGHT, (button) -> this.client.setScreen(new CreditsAndAttributionScreen(this)), this.textRenderer));
      if (this.realmsNotificationGui == null) {
         this.realmsNotificationGui = new RealmsNotificationsScreen();
      }

      if (this.isRealmsNotificationsGuiDisplayed()) {
         this.realmsNotificationGui.init(this.client, this.width, this.height);
      }

   }

   @Inject(
      method = {"initWidgetsNormal"},
      at = {@At(
   target = "Lnet/minecraft/client/gui/screen/TitleScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;",
   value = "INVOKE",
   shift = Shift.AFTER,
   ordinal = 2
)}
   )
   public void hookInit(int y, int spacingY, CallbackInfo ci) {
      ButtonWidget widget = ButtonWidget.builder(Text.of("Account Manager"), (action) -> this.client.setScreen(new AccountSelectorScreen(this))).dimensions(this.width / 2 - 100, y + spacingY * 3, 200, 20).tooltip(Tooltip.of(Text.of("Allows you to switch your in-game account"))).build();
      widget.active = true;
      this.addDrawableChild(widget);
   }
}
