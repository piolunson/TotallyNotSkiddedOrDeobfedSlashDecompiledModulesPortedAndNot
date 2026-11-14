package net.hachimi.client.impl.imixin;

import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.MessageIndicator;

@IMixin
public interface IChatHud {
   void addMessage(Text var1, MessageIndicator var2, int var3);
}
