package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.text.DecimalFormat;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.command.PlayerArgumentType;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class CoordsCommand extends Command {
   public CoordsCommand() {
      super("Coords", "Sends player coordinates", literal("coords"));
   }

   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      ((LiteralArgumentBuilder)builder.then(argument("player", PlayerArgumentType.player()).executes((c) -> {
         String playerName = PlayerArgumentType.getPlayer(c, "player");
         ChatUtil.serverSendCommand("w " + playerName + " " + this.getCoords());
         return 1;
      }))).executes((c) -> {
         Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
         Transferable transferable = new StringSelection(this.getCoords());
         clipboard.setContents(transferable, (ClipboardOwner)null);
         ChatUtil.clientSendMessage("Copied player coordinates to clipboard!");
         return 1;
      });
   }

   public String getCoords() {
      DecimalFormat decimal = new DecimalFormat("0.0");
      double x = mc.player.getX();
      double y = mc.player.getY();
      double z = mc.player.getZ();
      String var10000 = decimal.format(x);
      return var10000 + ", " + decimal.format(y) + ", " + decimal.format(z);
   }
}
