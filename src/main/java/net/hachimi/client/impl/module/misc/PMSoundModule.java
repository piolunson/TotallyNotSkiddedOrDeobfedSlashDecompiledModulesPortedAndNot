package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.gui.hud.ChatMessageEvent;
import net.hachimi.client.impl.manager.world.sound.SoundManager;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.sound.SoundEvent;

public class PMSoundModule extends ToggleModule {
   private static PMSoundModule INSTANCE;
   Config<PMSounds> soundsConfig;

   public PMSoundModule() {
      super("PMSound", "Plays a sound for private messages", ModuleCategory.MISCELLANEOUS);
      this.soundsConfig = this.register(new EnumConfig("Sounds", "The sound to play for PM", PMSoundModule.PMSounds.TWITTER, PMSoundModule.PMSounds.values()));
      INSTANCE = this;
   }

   public static PMSoundModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onChatMessage(ChatMessageEvent event) {
      String chatText = event.getText().getString();
      if (!chatText.startsWith("<") && (chatText.contains("whispers:") || chatText.contains("says:"))) {
         SoundManager var10000 = Managers.SOUND;
         SoundEvent var10001;
         switch (((PMSounds)this.soundsConfig.getValue()).ordinal()) {
            case 0 -> var10001 = SoundManager.TWITTER;
            case 1 -> var10001 = SoundManager.IOS;
            case 2 -> var10001 = SoundManager.DISCORD;
            case 3 -> var10001 = SoundManager.STEAM;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         var10000.playSound(var10001);
      }

   }

   public PMSounds getPMSound() {
      return this.soundsConfig.getValue();
   }

   public static enum PMSounds {
      TWITTER,
      IOS,
      DISCORD,
      STEAM;

      // $FF: synthetic method
      private static PMSounds[] $values() {
         return new PMSounds[]{TWITTER, IOS, DISCORD, STEAM};
      }
   }
}
