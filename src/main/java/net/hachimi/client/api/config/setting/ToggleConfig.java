package net.hachimi.client.api.config.setting;

import net.hachimi.client.api.config.ConfigContainer;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.EventBus;

public class ToggleConfig extends BooleanConfig {
   public ToggleConfig(String name, String desc, Boolean val) {
      super(name, desc, val);
   }

   public void setValue(Boolean val) {
      super.setValue(val);
      ConfigContainer container = this.getContainer();
      if (container instanceof ToggleModule toggle) {
         Animation anim = toggle.getAnimation();
         anim.setState(val);
         if (val) {
            EventBus.INSTANCE.subscribe(toggle);
         } else {
            EventBus.INSTANCE.unsubscribe(toggle);
         }
      }

   }

   public void enable() {
      ConfigContainer container = this.getContainer();
      if (container instanceof ToggleModule toggle) {
         toggle.enable();
      }

   }

   public void disable() {
      ConfigContainer container = this.getContainer();
      if (container instanceof ToggleModule toggle) {
         toggle.disable();
      }

   }
}
