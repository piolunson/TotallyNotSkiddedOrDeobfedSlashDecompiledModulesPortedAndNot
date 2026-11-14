package net.hachimi.client.mixin.gui.hud;

import net.hachimi.client.impl.event.gui.hud.ChatLineEvent;
import net.hachimi.client.impl.imixin.IChatHudLineVisible;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.client.gui.hud.MessageIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({net.minecraft.client.gui.hud.ChatHudLine.Visible.class})
public class MixinChatHudLineVisible implements IChatHudLineVisible, Globals {
   @Unique
   private int id;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void hookInit(int i, OrderedText orderedText, MessageIndicator messageIndicator, boolean bl, CallbackInfo ci) {
      ChatLineEvent chatLineEvent = new ChatLineEvent((net.minecraft.client.gui.hud.ChatHudLine.Visible)(Object)this, (double)(-mc.textRenderer.getWidth(orderedText)));
      EventBus.INSTANCE.dispatch(chatLineEvent);
   }

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }
}
