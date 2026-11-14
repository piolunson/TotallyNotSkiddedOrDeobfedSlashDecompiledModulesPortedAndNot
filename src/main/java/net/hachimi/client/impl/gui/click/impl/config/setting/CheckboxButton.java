package net.hachimi.client.impl.gui.click.impl.config.setting;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.gui.click.impl.config.CategoryFrame;
import net.hachimi.client.impl.gui.click.impl.config.ModuleButton;
import net.hachimi.client.impl.manager.world.sound.SoundManager;
import net.hachimi.client.impl.module.client.ClickGuiModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.render.animation.Animation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class CheckboxButton extends ConfigButton<Boolean> {
   public CheckboxButton(CategoryFrame frame, ModuleButton moduleButton, Config<Boolean> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
      config.getAnimation().setState((Boolean)config.getValue());
   }

   public void render(DrawContext context, float ix, float iy, float mouseX, float mouseY, float delta) {
      this.setHeight((float)RenderManager.textHeight() + 4.0F);
      this.x = ix;
      this.y = iy;
      Animation checkboxAnimation = this.config.getAnimation();
      boolean state = this.isWithin(mouseX, mouseY);
      if (state != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(state);
      }

      int hoverAlpha = (int)((double)80.0F * MathHelper.clamp(this.hoverAnimation.getFactor(), (double)0.0F, (double)1.0F));
      this.rect(context, checkboxAnimation.getFactor() > (double)0.01F ? ClickGuiModule.getInstance().getColor(100 + hoverAlpha, (float)checkboxAnimation.getFactor()) : (new Color(hoverAlpha, hoverAlpha, hoverAlpha, hoverAlpha)).getRGB());
      int whiteText = -1;
      this.drawStringScaled(context, this.config.getName(), ix + 2.0F * ClickGuiModule.CLICK_GUI_SCALE, iy + 4.0F * ClickGuiModule.CLICK_GUI_SCALE, whiteText);
   }

   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (this.isWithin(mouseX, mouseY) && button == 0) {
         boolean val = (Boolean)this.config.getValue();
         this.config.setValue(!val);
         if (ClickGuiModule.getInstance().getSounds()) {
            Managers.SOUND.playSound(SoundManager.GUI_CLICK);
         }
      }

   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   public void keyPressed(int keyCode, int scanCode, int modifiers) {
   }

   public void charTyped(char character, int modifiers) {
   }
}
