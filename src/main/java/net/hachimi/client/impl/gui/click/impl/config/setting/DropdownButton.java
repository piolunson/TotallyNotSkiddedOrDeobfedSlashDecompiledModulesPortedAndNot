package net.hachimi.client.impl.gui.click.impl.config.setting;

import java.util.Arrays;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.gui.click.impl.config.CategoryFrame;
import net.hachimi.client.impl.gui.click.impl.config.ModuleButton;
import net.hachimi.client.impl.manager.world.sound.SoundManager;
import net.hachimi.client.impl.module.client.ClickGuiModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.string.EnumFormatter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class DropdownButton extends ConfigButton<Enum<?>> {
   private int index;
   private boolean open;
   private String[] values;

   public DropdownButton(CategoryFrame frame, ModuleButton moduleButton, Config<Enum<?>> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
      this.cacheValues();
   }

   private void cacheValues() {
      Enum<?>[] enumValues = ((EnumConfig)this.config).getValues();
      this.values = (String[])Arrays.stream(enumValues).map(Enum::name).toArray((x$0) -> new String[x$0]);
      Enum<?> val = this.config.getValue();
      if (val != null) {
         for(int i = 0; i < this.values.length; ++i) {
            if (this.values[i].equals(val.name())) {
               this.index = i;
               break;
            }
         }
      }

   }

   private float rowH() {
      return (float)RenderManager.textHeight() + 4.0F;
   }

   public void render(DrawContext context, float ix, float iy, float mouseX, float mouseY, float delta) {
      float s = ClickGuiModule.CLICK_GUI_SCALE;
      float base = this.rowH();
      float totalH = this.open ? base + (float)this.values.length * base : base;
      this.setHeight(totalH);
      this.x = ix;
      this.y = iy;
      String val = EnumFormatter.formatEnum(this.config.getValue());
      boolean over = this.isWithin(mouseX, mouseY);
      if (over != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(over);
      }

      int hoverAlpha = (int)((double)80.0F * MathHelper.clamp(this.hoverAnimation.getFactor(), (double)0.0F, (double)1.0F));
      this.fill(context, (double)ix, (double)iy, (double)(this.width * s), (double)(base * s), ClickGuiModule.getInstance().getColor(100 + hoverAlpha));
      this.drawStringScaled(context, this.config.getName(), ix + 2.0F * s, iy + 4.0F * s, -1);
      float nameW = (float)RenderManager.textWidth(this.config.getName()) * s;
      int valColor = ClickGuiModule.getInstance().getColor(255);
      this.drawStringScaled(context, " " + val, ix + 2.0F * s + nameW, iy + 4.0F * s, valColor);
      String arrow = this.open ? " ▲" : " ▼";
      String var10000 = this.config.getName();
      float arrowW = (float)RenderManager.textWidth(var10000 + " " + val) * s;
      this.drawStringScaled(context, arrow, ix + 2.0F * s + arrowW, iy + 4.0F * s, -5592406);
      if (this.open) {
         for(int i = 0; i < this.values.length; ++i) {
            float rowY = iy + base * s + (float)i * base * s;
            boolean rowOver = this.isMouseOver((double)mouseX, (double)mouseY, ix, rowY, this.width * s, base * s);
            int bgColor;
            if (i == this.index) {
               bgColor = ClickGuiModule.getInstance().getColor(160);
            } else if (rowOver) {
               bgColor = ClickGuiModule.getInstance().getColor(120);
            } else {
               bgColor = ClickGuiModule.getInstance().getColor(70);
            }

            this.fill(context, (double)ix, (double)rowY, (double)(this.width * s), (double)(base * s), bgColor);
            String label = EnumFormatter.formatEnum(Enum.valueOf(((Enum)this.config.getValue()).getClass(), this.values[i]));
            int textColor = i == this.index ? -1 : -3355444;
            this.drawStringScaled(context, " " + label, ix + 2.0F * s, rowY + 4.0F * s, textColor);
         }
      }

   }

   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (!this.isWithin(mouseX, mouseY) && !this.openAreaContains(mouseX, mouseY)) {
         if (this.open) {
            this.open = false;
         }

      } else {
         float s = ClickGuiModule.CLICK_GUI_SCALE;
         float base = this.rowH();
         if (button == 1) {
            if (this.isWithin(mouseX, mouseY)) {
               this.open = !this.open;
               this.playClick();
            }

         } else {
            if (button == 0) {
               if (this.open) {
                  for(int i = 0; i < this.values.length; ++i) {
                     float rowY = this.y + base * s + (float)i * base * s;
                     if (this.isMouseOver(mouseX, mouseY, this.x, rowY, this.width * s, base * s)) {
                        this.index = i;
                        this.config.setValue(Enum.valueOf(((Enum)this.config.getValue()).getClass(), this.values[i]));
                        this.open = false;
                        this.playClick();
                        return;
                     }
                  }

                  if (this.isWithin(mouseX, mouseY)) {
                     this.open = false;
                     this.playClick();
                  }
               } else {
                  this.index = this.index + 1 > this.values.length - 1 ? 0 : this.index + 1;
                  this.config.setValue(Enum.valueOf(((Enum)this.config.getValue()).getClass(), this.values[this.index]));
                  this.playClick();
               }
            }

         }
      }
   }

   private boolean openAreaContains(double mouseX, double mouseY) {
      if (!this.open) {
         return false;
      } else {
         float s = ClickGuiModule.CLICK_GUI_SCALE;
         float base = this.rowH();
         return this.isMouseOver(mouseX, mouseY, this.x, this.y + base * s, this.width * s, (float)this.values.length * base * s);
      }
   }

   private boolean isMouseOver(double mx, double my, float rx, float ry, float rw, float rh) {
      return mx >= (double)rx && mx <= (double)(rx + rw) && my >= (double)ry && my <= (double)(ry + rh);
   }

   private void playClick() {
      if (ClickGuiModule.getInstance().getSounds()) {
         Managers.SOUND.playSound(SoundManager.GUI_CLICK);
      }

   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   public void keyPressed(int keyCode, int scanCode, int modifiers) {
   }

   public void charTyped(char character, int modifiers) {
   }
}
