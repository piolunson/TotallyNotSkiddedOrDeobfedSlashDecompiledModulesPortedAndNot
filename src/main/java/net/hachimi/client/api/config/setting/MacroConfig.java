package net.hachimi.client.api.config.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.macro.Macro;
import net.hachimi.client.init.Managers;

public class MacroConfig extends Config<Macro> {
   public MacroConfig(String name, String desc, Macro val) {
      super(name, desc, val);
   }

   public void setValue(int keycode) {
      ((Macro)this.value).setKeycode(keycode);
      if (Managers.isInitialized()) {
         Managers.MACRO.setMacro((Macro)this.getValue(), keycode);
      }

   }

   public Macro fromJson(JsonObject jsonObj) {
      if (jsonObj.has("value")) {
         JsonElement element = jsonObj.get("value");
         return new Macro(this.getMacroId(), element.getAsInt(), this.getRunnable());
      } else {
         return null;
      }
   }

   public JsonObject toJson() {
      JsonObject object = super.toJson();
      object.addProperty("value", this.getKeycode());
      return object;
   }

   public String getMacroId() {
      return ((Macro)this.value).getName();
   }

   public Runnable getRunnable() {
      return ((Macro)this.value).getRunnable();
   }

   public int getKeycode() {
      return ((Macro)this.value).getKeycode();
   }

   public String getKeyName() {
      return ((Macro)this.value).getKeyName();
   }
}
