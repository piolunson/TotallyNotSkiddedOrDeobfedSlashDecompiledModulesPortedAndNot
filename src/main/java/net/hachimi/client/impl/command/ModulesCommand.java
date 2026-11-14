package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.List;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.module.Module;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.command.CommandSource;

public class ModulesCommand extends Command {
   public ModulesCommand() {
      super("Modules", "Displays all client modules", literal("modules"));
   }

   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((c) -> {
         List<String> modulesList = new ArrayList<>();

         for(Module module : Managers.MODULE.getModules()) {
            String var10000;
            label17: {
               if (module instanceof ToggleModule t) {
                  if (t.isEnabled()) {
                     var10000 = "§s";
                     break label17;
                  }
               }

               var10000 = "§f";
            }

            String formatting = var10000;
            modulesList.add(formatting + module.getName());
         }

         ChatUtil.clientSendMessageRaw(" §7Modules:§f " + String.join(", ", modulesList));
         return 1;
      });
   }
}
