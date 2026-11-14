package net.hachimi.client.impl.module.client;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.gui.chat.ChatMessageEvent;
import net.hachimi.client.impl.event.gui.hud.RenderOverlayEvent;
import net.hachimi.client.impl.event.keyboard.KeyboardInputEvent;
import net.hachimi.client.impl.event.network.GameJoinEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.chat.ChatUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.render.animation.Easing;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.gui.screen.ChatScreen;

public class ChatModule extends ToggleModule {
   private static ChatModule INSTANCE;
   private boolean ircChat;
   private boolean notified;
   private final Timer timer = new CacheTimer();
   public Config<Boolean> dmsOnly = this.register(new BooleanConfig("DMOnly", "Only receive private messages from IRC", false));
   private final Animation ircAnimation;

   public ChatModule() {
      super("Chat", "Manages the client chat", ModuleCategory.CLIENT);
      this.ircAnimation = new Animation(false, 200.0F, Easing.LINEAR);
      INSTANCE = this;
   }

   public static ChatModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      System.out.println("Baritone has been removed");
      if (mc.player != null && !this.notified && !(Boolean)this.dmsOnly.getValue()) {
         ChatUtil.clientSendMessageRaw("§s[Chat]§7 Press ALT in chat to enter IRC!", 107);
         this.notified = true;
      }
   }

   @EventListener
   public void onGameJoin(GameJoinEvent event) {
      if (!(Boolean)this.dmsOnly.getValue()) {
         if (!this.notified) {
            ChatUtil.clientSendMessageRaw("§s[Chat]§7 Press ALT in chat to enter IRC!", 107);
            this.notified = true;
         }
      }
   }

   @EventListener
   public void onKey(KeyboardInputEvent event) {
      if (!(Boolean)this.dmsOnly.getValue()) {
         if (this.timer.passed(250)) {
            if (event.getAction() != 2 && (event.getKeycode() == 342 || event.getKeycode() == 346) && mc.currentScreen instanceof ChatScreen) {
               this.ircChat = !this.ircChat;
               this.ircAnimation.setState(this.ircChat);
               this.timer.reset();
            }

         }
      }
   }

   @EventListener(
      priority = Integer.MIN_VALUE
   )
   public void onChatMessage(ChatMessageEvent.Client event) {
      if (!(Boolean)this.dmsOnly.getValue()) {
         if (this.ircChat) {
            String text = event.getMessage().trim();
            if (text.isEmpty() || text.isBlank() || text.startsWith(Managers.COMMAND.getPrefix()) || text.startsWith("/")) {
               return;
            }

            event.cancel();
            mc.inGameHud.getChatHud().addToMessageHistory(text);
         }

      }
   }

   @EventListener
   public void onRenderOverlay(RenderOverlayEvent.Hotbar event) {
      if (!(Boolean)this.dmsOnly.getValue()) {
         if (mc.currentScreen instanceof ChatScreen && this.ircAnimation.getFactor() > 0.01) {
            float height = (float)mc.getWindow().getScaledHeight();
            float width = (float)mc.getWindow().getScaledWidth();
            float anim = HUDModule.getInstance().isEnabled() ? HUDModule.getInstance().getChatAnimation() : 1.0F;
            RenderManager.borderedRect(event.getContext().getMatrices(), (double)2.0F, (double)((int)height), (double)(width - 4.0F), (double)(-16.0F * anim), ColorsModule.getInstance().getRGB((int)((double)255.0F * this.ircAnimation.getFactor())), (double)1.0F);
         }

      }
   }

   public boolean isDmsOnly() {
      return (Boolean)this.dmsOnly.getValue();
   }
}
