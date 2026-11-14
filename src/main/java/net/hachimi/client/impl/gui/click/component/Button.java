package net.hachimi.client.impl.gui.click.component;

import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.render.animation.Easing;

public abstract class Button extends Component implements Interactable {
   protected final Frame frame;
   protected final Animation hoverAnimation;

   public Button(Frame frame, float x, float y, float width, float height) {
      this.hoverAnimation = new Animation(false, 150.0F, Easing.LINEAR);
      this.frame = frame;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public Button(Frame frame) {
      this.hoverAnimation = new Animation(false, 150.0F, Easing.LINEAR);
      this.frame = frame;
   }

   public abstract void mouseClicked(double var1, double var3, int var5);

   public abstract void mouseReleased(double var1, double var3, int var5);

   public abstract void keyPressed(int var1, int var2, int var3);

   public Frame getFrame() {
      return this.frame;
   }
}
