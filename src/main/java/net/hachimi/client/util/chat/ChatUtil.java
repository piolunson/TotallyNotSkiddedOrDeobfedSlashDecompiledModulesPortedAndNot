package net.hachimi.client.util.chat;

import net.hachimi.client.impl.imixin.IChatHud;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.util.Globals;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.MessageIndicator;

public class ChatUtil implements Globals {
   public static final String PREFIX = "§s[Hachimi] §r";

   public static void clientSendMessage(String message) {
      clientSendMessage(message, 0);
   }

   public static void clientSendMessage(String message, int id) {
      net.minecraft.client.gui.hud.MessageIndicator messageIndicator = new net.minecraft.client.gui.hud.MessageIndicator(ColorsModule.getInstance().getRGB(), (net.minecraft.client.gui.hud.MessageIndicator.Icon)null, Text.empty(), "Hachimi");
      ((IChatHud)mc.inGameHud.getChatHud()).addMessage(Text.of("§s[Hachimi] §r" + message), messageIndicator, id);
   }

   public static void clientSendMessage(String message, Object... params) {
      clientSendMessage(String.format(message, params));
   }

   public static void clientSendMessageRaw(String message) {
      clientSendMessageRaw(message, 0);
   }

   public static void clientSendMessageRaw(String message, int id) {
      net.minecraft.client.gui.hud.MessageIndicator messageIndicator = new net.minecraft.client.gui.hud.MessageIndicator(ColorsModule.getInstance().getRGB(), (net.minecraft.client.gui.hud.MessageIndicator.Icon)null, Text.empty(), "Hachimi");
      ((IChatHud)mc.inGameHud.getChatHud()).addMessage(Text.of(message), messageIndicator, id);
   }

   public static void clientSendMessageRaw(String message, Object... params) {
      clientSendMessageRaw(String.format(message, params));
   }

   public static void serverSendMessage(String message) {
      if (mc.player != null) {
         mc.player.networkHandler.sendChatMessage(message);
      }

   }

   public static void serverSendCommand(String message) {
      if (mc.player != null) {
         mc.player.networkHandler.sendChatCommand(message);
      }

   }

   public static void serverSendMessage(PlayerEntity player, String message) {
      if (mc.player != null) {
         String reply = "/msg " + player.getName().getString() + " ";
         mc.player.networkHandler.sendChatMessage(reply + message);
      }

   }

   public static void serverSendMessage(PlayerEntity player, String message, Object... params) {
      serverSendMessage(player, String.format(message, params));
   }

   public static void error(String message) {
      String var10000 = String.valueOf(Formatting.RED);
      clientSendMessage(var10000 + message);
   }

   public static void error(String message, Object... params) {
      clientSendMessage(String.valueOf(Formatting.RED) + message, params);
   }
}
