package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.SwingEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;

public class SwingModule extends ToggleModule {
   private static SwingModule INSTANCE;
   Config<SwingHand> swingHandConfig;
   private Hand prevPreferredHand;
   private Hand hand;

   public SwingModule() {
      super("Swing", "Modifies the swinging hand", ModuleCategory.MISCELLANEOUS);
      this.swingHandConfig = this.register(new EnumConfig("Hand", "The player swinging hand", SwingModule.SwingHand.MAINHAND, SwingModule.SwingHand.values()));
      INSTANCE = this;
   }

   public static SwingModule getInstance() {
      return INSTANCE;
   }

   public void onEnable() {
      if (mc.player != null) {
         this.prevPreferredHand = mc.player.preferredHand;
         if (this.swingHandConfig.getValue() != SwingModule.SwingHand.PACKET) {
            this.updateSwingHand(this.swingHandConfig.getValue());
         }
      }

   }

   public void onDisable() {
      if (mc.player != null) {
         mc.player.preferredHand = this.prevPreferredHand;
      }

   }

   @EventListener
   public void onSwing(SwingEvent event) {
      this.updateSwingHand(this.swingHandConfig.getValue());
   }

   private void updateSwingHand(SwingHand swingHand) {
      Hand var10001;
      switch (swingHand.ordinal()) {
         case 0:
            var10001 = Hand.MAIN_HAND;
            break;
         case 1:
            var10001 = Hand.OFF_HAND;
            break;
         case 2:
            var10001 = mc.player.handSwinging && mc.player.handSwingTicks < this.getHandSwingDuration() / 2 && mc.player.handSwingTicks >= 0 ? this.hand : (this.hand != Hand.MAIN_HAND ? Hand.MAIN_HAND : Hand.OFF_HAND);
            break;
         case 3:
            mc.player.handSwingTicks = 0;
            mc.player.handSwinging = false;
            var10001 = null;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      this.hand = var10001;
      if (this.hand != null) {
         mc.player.preferredHand = this.hand;
      }

   }

   public int getHandSwingDuration() {
      if (StatusEffectUtil.hasHaste(mc.player)) {
         return 6 - (1 + StatusEffectUtil.getHasteAmplifier(mc.player));
      } else {
         return mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE) ? 6 + (1 + mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2 : 6;
      }
   }

   public Hand getSwingHand() {
      return this.hand;
   }

   public Hand getPrevPreferredHand() {
      return this.prevPreferredHand;
   }

   private static enum SwingHand {
      MAINHAND,
      OFFHAND,
      SWAP,
      PACKET;

      // $FF: synthetic method
      private static SwingHand[] $values() {
         return new SwingHand[]{MAINHAND, OFFHAND, SWAP, PACKET};
      }
   }
}
