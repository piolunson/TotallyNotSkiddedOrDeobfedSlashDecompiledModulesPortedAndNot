package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.event.Event;
import net.minecraft.client.gui.hud.ChatHudLine;

public class ChatLineEvent extends Event {
   private final net.minecraft.client.gui.hud.ChatHudLine.Visible chatHudLine;
   private final double width;

   public ChatLineEvent(net.minecraft.client.gui.hud.ChatHudLine.Visible chatHudLine, double width) {
      this.chatHudLine = chatHudLine;
      this.width = width;
   }

   public net.minecraft.client.gui.hud.ChatHudLine.Visible getChatHudLine() {
      return this.chatHudLine;
   }

   public double getWidth() {
      return this.width;
   }
}
