package net.hachimi.client.impl.module.misc;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.gui.chat.ChatHistoryEvent;
import net.hachimi.client.impl.event.gui.chat.ChatLengthEvent;
import net.hachimi.client.impl.event.gui.hud.ChatLineEvent;
import net.hachimi.client.impl.event.gui.hud.ChatMessageEvent;
import net.hachimi.client.impl.event.gui.hud.ChatTextRenderEvent;
import net.hachimi.client.impl.event.gui.hud.RenderChatHudEvent;
import net.hachimi.client.impl.event.gui.hud.SignatureIndicatorEvent;
import net.hachimi.client.util.FormattingUtil;
import net.hachimi.client.util.render.animation.Easing;
import net.hachimi.client.util.render.animation.TimeAnimation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.ChatHudLine;

public class BetterChatModule extends ToggleModule {
   Config<Boolean> chatFontConfig = this.register(new BooleanConfig("ChatFont", "Uses custom font for chat text", false));
   Config<Timestamp> timestampConfig;
   Config<AnimationMode> animationConfig;
   Config<Integer> timeConfig;
   Config<Boolean> noSignatureConfig;
   Config<Boolean> infiniteConfig;
   Config<Boolean> keepChatConfig;
   public final Map<net.minecraft.client.gui.hud.ChatHudLine.Visible, TimeAnimation> animationMap;

   public BetterChatModule() {
      super("BetterChat", "Modifications for the chat", ModuleCategory.MISCELLANEOUS);
      this.timestampConfig = this.register(new EnumConfig("Timestamp", "Shows chat timestamps", BetterChatModule.Timestamp.OFF, BetterChatModule.Timestamp.values()));
      this.animationConfig = this.register(new EnumConfig("Animation", "Animates the chat", BetterChatModule.AnimationMode.OFF, BetterChatModule.AnimationMode.values()));
      this.timeConfig = this.register(new NumberConfig("Anim-Time", "Time for the animation", 0, 200, 1000, () -> false));
      this.noSignatureConfig = this.register(new BooleanConfig("NoSignatureIndicator", "Removes the message signature indicator", false));
      this.infiniteConfig = this.register(new BooleanConfig("Infinite", "Makes chat length infinite", false));
      this.keepChatConfig = this.register(new BooleanConfig("KeepChat", "Maintains chat history", false));
      this.animationMap = new HashMap<>();
   }

   @EventListener
   public void onChatText(ChatMessageEvent event) {
      if (this.timestampConfig.getValue() != BetterChatModule.Timestamp.OFF) {
         String string = FormattingUtil.toString(event.getText());
         if (string.contains("§s[Hachimi] §r")) {
            return;
         }

         String time = (new SimpleDateFormat("k:mm")).format(new Date());
         String var10000;
         switch (((Timestamp)this.timestampConfig.getValue()).ordinal()) {
            case 0 -> var10000 = "<" + time + ">§r ";
            case 1 -> var10000 = "§8<§7" + time + "§8>§r ";
            case 2 -> var10000 = "§s<" + time + ">§r ";
            case 3 -> var10000 = "";
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         String text = var10000;
         event.cancel();
         event.setText(Text.of(text + string));
      }

   }

   @EventListener
   public void onChatLine(ChatLineEvent event) {
      this.animationMap.put(event.getChatHudLine(), new TimeAnimation(false, event.getWidth(), (double)0.0F, (float)(Integer)this.timeConfig.getValue(), Easing.LINEAR));
   }

   @EventListener
   public void onChatLineRender(RenderChatHudEvent event) {
      if (this.animationConfig.getValue() != BetterChatModule.AnimationMode.OFF) {
         TimeAnimation animation = null;
         if (event.getChatHudLine() != null && this.animationMap.containsKey(event.getChatHudLine())) {
            animation = (TimeAnimation)this.animationMap.get(event.getChatHudLine());
         }

         if (animation != null) {
            animation.setState(true);
            event.cancel();
            if (this.animationConfig.getValue() == BetterChatModule.AnimationMode.SLIDE) {
               event.setAnimation(animation.getCurrent());
            } else {
               event.setAnimation(animation.getFactor());
            }

            event.setSlide(this.animationConfig.getValue() == BetterChatModule.AnimationMode.SLIDE);
         }
      }

   }

   @EventListener
   public void onSignatureIndicator(SignatureIndicatorEvent event) {
      if ((Boolean)this.noSignatureConfig.getValue()) {
         event.cancel();
      }

   }

   @EventListener
   public void onChatHistory(ChatHistoryEvent event) {
      if ((Boolean)this.keepChatConfig.getValue()) {
         event.cancel();
      }

   }

   @EventListener
   public void onChatLength(ChatLengthEvent event) {
      if ((Boolean)this.infiniteConfig.getValue()) {
         event.cancel();
      }

   }

   @EventListener
   public void onChatTextRender(ChatTextRenderEvent event) {
      if ((Boolean)this.chatFontConfig.getValue()) {
         event.cancel();
      }

   }

   public static enum Timestamp {
      NORMAL,
      GRAY,
      COLOR,
      OFF;

      // $FF: synthetic method
      private static Timestamp[] $values() {
         return new Timestamp[]{NORMAL, GRAY, COLOR, OFF};
      }
   }

   public static enum AnimationMode {
      SLIDE,
      FADE,
      OFF;

      // $FF: synthetic method
      private static AnimationMode[] $values() {
         return new AnimationMode[]{SLIDE, FADE, OFF};
      }
   }
}
