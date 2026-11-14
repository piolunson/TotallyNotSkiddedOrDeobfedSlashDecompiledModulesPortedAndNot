package net.hachimi.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;

public class ConnectCommand extends Command {
   public ConnectCommand() {
      super("Connect", "Connects you to a server", literal("connect"));
   }

   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      ((LiteralArgumentBuilder)builder.then(argument("server", StringArgumentType.string()))).executes((c) -> {
         String serverAddress = StringArgumentType.getString(c, "server");
         if (mc.currentScreen instanceof ConnectScreen) {
            ChatUtil.error("Attempt to connect while already connecting");
            return 0;
         } else {
            mc.disconnect();
            mc.loadBlockList();
            Managers.NETWORK.connect(ServerAddress.parse(serverAddress), new net.minecraft.client.network.ServerInfo(I18n.translate("selectServer.defaultName", new Object[0]), serverAddress, net.minecraft.client.network.ServerInfo.ServerType.OTHER));
            return 1;
         }
      });
   }
}
