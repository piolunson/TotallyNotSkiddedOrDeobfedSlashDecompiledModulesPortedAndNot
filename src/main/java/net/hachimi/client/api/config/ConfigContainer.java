package net.hachimi.client.api.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.Identifiable;
import net.hachimi.client.api.config.setting.BlockListConfig;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.ItemListConfig;
import net.hachimi.client.api.config.setting.MacroConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.config.setting.StringConfig;
import net.hachimi.client.api.config.setting.ToggleConfig;
import net.hachimi.client.api.macro.Macro;
import net.hachimi.client.util.Globals;

public class ConfigContainer implements Identifiable, Serializable<Config<?>>, Globals {
   protected final String name;
   private final Map<String, Config<?>> configurations = Collections.synchronizedMap(new LinkedHashMap<>());

   public ConfigContainer(String name) {
      this.name = name;
   }

   protected <T> Config<T> register(Config<T> config) {
      config.setContainer(this);
      this.configurations.put(config.getId(), config);
      return config;
   }

   protected void register(Config<?>... configs) {
      for(Config<?> config : configs) {
         this.register(config);
      }

   }

   protected void unregister(Config<?> config) {
      this.configurations.remove(config.getId());
   }

   protected void unregister(Config<?>... configs) {
      for(Config<?> config : configs) {
         this.unregister(config);
      }

   }

   public JsonObject toJson() {
      JsonObject out = new JsonObject();
      out.addProperty("name", this.getName());
      out.addProperty("id", this.getId());
      JsonArray array = new JsonArray();

      for(Config<?> config : this.getConfigs()) {
         if (!config.getName().equalsIgnoreCase("Keybind")) {
            array.add(config.toJson());
         }
      }

      out.add("configs", array);
      return out;
   }

   public Config<?> fromJson(JsonObject jsonObj) {
      if (jsonObj.has("configs")) {
         JsonElement element = jsonObj.get("configs");
         if (!element.isJsonArray()) {
            return null;
         }

         for(JsonElement je : element.getAsJsonArray()) {
            if (je.isJsonObject()) {
               JsonObject configObj = je.getAsJsonObject();
               JsonElement id = configObj.get("id");
               Config<?> config = this.getConfig(id.getAsString());
               if (config != null) {
                  try {
                     if (config instanceof ToggleConfig) {
                        ToggleConfig cfg = (ToggleConfig)config;
                        Boolean val = cfg.fromJson(configObj);
                        if (mc.world != null) {
                           if (val) {
                              cfg.disable();
                              cfg.enable();
                           } else {
                              cfg.disable();
                           }
                        } else {
                           cfg.setValue(val);
                        }
                     } else if (config instanceof BooleanConfig) {
                        BooleanConfig cfg = (BooleanConfig)config;
                        Boolean val = cfg.fromJson(configObj);
                        cfg.setValue(val);
                     } else if (config instanceof ColorConfig) {
                        ColorConfig cfg = (ColorConfig)config;
                        Color val = cfg.fromJson(configObj);
                        cfg.setValue(val);
                     } else if (config instanceof EnumConfig) {
                        EnumConfig cfg = (EnumConfig)config;
                        Enum<?> val = cfg.fromJson(configObj);
                        if (val != null) {
                           cfg.setValue(val);
                        }
                     } else if (config instanceof ItemListConfig) {
                        ItemListConfig cfg = (ItemListConfig)config;
                        List<?> val = cfg.fromJson(configObj);
                        cfg.setValue(val);
                     } else if (config instanceof BlockListConfig) {
                        BlockListConfig cfg = (BlockListConfig)config;
                        List<?> val = cfg.fromJson(configObj);
                        cfg.setValue(val);
                     } else if (config instanceof NumberConfig) {
                        NumberConfig cfg = (NumberConfig)config;
                        Number val = cfg.fromJson(configObj);
                        cfg.setValue(val);
                     } else if (config instanceof StringConfig) {
                        StringConfig cfg = (StringConfig)config;
                        String val = cfg.fromJson(configObj);
                        cfg.setValue(val);
                     } else if (config instanceof MacroConfig) {
                        MacroConfig cfg = (MacroConfig)config;
                        Macro val = cfg.fromJson(configObj);
                        cfg.setValue(val);
                     }
                  } catch (Exception e) {
                     Hachimi.error("Couldn't parse Json for {}!", config.getName());
                     e.printStackTrace();
                  }
               }
            }
         }
      }

      return null;
   }

   public String getName() {
      return this.name;
   }

   public String getId() {
      return String.format("%s-container", this.name.toLowerCase());
   }

   public Config<?> getConfig(String id) {
      return (Config)this.configurations.get(id);
   }

   public Collection<Config<?>> getConfigs() {
      return this.configurations.values();
   }
}
