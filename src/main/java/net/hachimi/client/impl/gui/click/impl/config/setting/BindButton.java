package net.hachimi.client.impl.gui.click.impl.config.setting;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.MacroConfig;
import net.hachimi.client.api.macro.Macro;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.gui.click.impl.config.CategoryFrame;
import net.hachimi.client.impl.gui.click.impl.config.ModuleButton;
import net.hachimi.client.impl.manager.world.sound.SoundManager;
import net.hachimi.client.impl.module.client.ClickGuiModule;
import net.hachimi.client.init.Managers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class BindButton extends ConfigButton<Macro> {
   private boolean listening;

   public BindButton(CategoryFrame frame, ModuleButton moduleButton, Config<Macro> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
   }

   public void render(DrawContext context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight((float)RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      Macro macro = this.config.getValue();
      String val = this.listening ? "..." : macro.getKeyName();
      boolean state = this.isWithin(mouseX, mouseY);
      if (state != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(state);
      }

      int hoverAlpha = (int)((double)80.0F * MathHelper.clamp(this.hoverAnimation.getFactor(), (double)0.0F, (double)1.0F));
      this.rect(context, (new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha)).getRGB());
      int whiteText = -1;
      this.drawStringScaled(context, this.config.getName(), ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, whiteText);
      float textLeng = (float)RenderManager.textWidth(this.config.getName()) * ClickGuiModule.CLICK_GUI_SCALE;
      int grayText = -5592406;
      this.drawStringScaled(context, " " + val, ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE + textLeng, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, grayText);
   }

   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isWithin(mouseX, mouseY)) {
         if (button == 0) {
            this.listening = !this.listening;
         } else if (button == 1 && !this.listening) {
            ((MacroConfig)this.config).setValue(-1);
         } else if (this.listening) {
            if (button != 1) {
               ((MacroConfig)this.config).setValue(1000 + button);
            }

            this.listening = false;
         }

         if (ClickGuiModule.getInstance().getSounds()) {
            Managers.SOUND.playSound(SoundManager.GUI_CLICK);
         }
      }

   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   public void keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.listening) {
         if (keyCode != 256 && keyCode != 259) {
            ((MacroConfig)this.config).setValue(keyCode);
         } else {
            ((MacroConfig)this.config).setValue(-1);
         }

         this.listening = false;
      }

   }

   public void charTyped(char character, int modifiers) {
   }

   public boolean isListening() {
      return this.listening;
   }

   public void setListening(boolean listening) {
      this.listening = listening;
   }
}
