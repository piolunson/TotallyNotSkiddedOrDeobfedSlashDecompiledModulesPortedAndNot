package net.hachimi.client.impl.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.hachimi.client.api.command.Command;
import net.minecraft.item.ItemStack;
import net.minecraft.command.CommandSource;

public class PeekCommand extends Command {
   public PeekCommand() {
      super("Peek", "Shows the contents of held shulkers", literal("peek"));
   }

   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
   }

   public void displayShulkerInventory(ItemStack stack) {
   }
}
