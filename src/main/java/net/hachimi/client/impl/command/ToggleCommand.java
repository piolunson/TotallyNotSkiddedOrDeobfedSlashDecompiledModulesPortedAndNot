package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.command.ModuleArgumentType;
import net.hachimi.client.api.module.Module;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class ToggleCommand extends Command {
   public ToggleCommand() {
      super("Toggle", "Enables/Disables a module", literal("toggle"));
   }

   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      ((LiteralArgumentBuilder)builder.then(argument("module", ModuleArgumentType.module()).executes((c) -> {
         Module module = ModuleArgumentType.getModule(c, "module");
         if (module instanceof ToggleModule t) {
            t.toggle();
            Object[] var10001 = new Object[2];
            String var10004 = t.getName();
            var10001[0] = "§7" + var10004 + "§f";
            var10001[1] = t.isEnabled() ? "§senabled§f" : "§cdisabled§f";
            ChatUtil.clientSendMessage("%s is now %s", var10001);
         }

         return 1;
      }))).executes((c) -> {
         ChatUtil.error("Must provide module to toggle!");
         return 1;
      });
   }
}
