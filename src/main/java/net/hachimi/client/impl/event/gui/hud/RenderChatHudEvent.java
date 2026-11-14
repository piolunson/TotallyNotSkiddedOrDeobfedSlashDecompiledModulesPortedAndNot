package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.client.gui.hud.ChatHudLine;

@Cancelable
public class RenderChatHudEvent extends Event {
   private final net.minecraft.client.gui.hud.ChatHudLine.Visible chatHudLine;
   private double animation;
   private boolean animationMode;

   public RenderChatHudEvent(net.minecraft.client.gui.hud.ChatHudLine.Visible chatHudLine) {
      this.chatHudLine = chatHudLine;
   }

   public double getAnimation() {
      return this.animation;
   }

   public void setAnimation(double animation) {
      this.animation = animation;
   }

   public void setSlide(boolean animationMode) {
      this.animationMode = animationMode;
   }

   public net.minecraft.client.gui.hud.ChatHudLine.Visible getChatHudLine() {
      return this.chatHudLine;
   }

   public boolean getAnimationMode() {
      return this.animationMode;
   }
}
