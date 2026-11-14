package net.hachimi.client.api.config.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class ItemListConfig<T extends List<Item>> extends Config<T> {
   public ItemListConfig(String name, String desc, Item... values) {
      super(name, desc, (T)new ArrayList(Arrays.asList(values)));
   }

   public ItemListConfig(String name, String desc, T values) {
      super(name, desc, values);
   }

   public void add(Item item) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      ((List)this.getValue()).add(item);
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public void remove(Item item) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      ((List)this.getValue()).remove(item);
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public void clear() {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      ((List)this.getValue()).clear();
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public boolean contains(Object obj) {
      return obj instanceof Item ? ((List)this.value).contains(obj) : false;
   }

   public JsonObject toJson() {
      JsonObject jsonObj = super.toJson();
      JsonArray array = new JsonArray();

      for(Item item : (List<Item>)this.getValue()) {
         Identifier id = Registries.ITEM.getId(item);
         array.add(id.toString());
      }

      jsonObj.add("value", array);
      return jsonObj;
   }

   public T fromJson(JsonObject jsonObj) {
      if (!jsonObj.has("value")) {
         return null;
      } else {
         JsonElement element = jsonObj.get("value");
         List<Item> temp = new ArrayList<>();

         for(JsonElement je : element.getAsJsonArray()) {
            String val = je.getAsString();
            Item item = (Item)Registries.ITEM.get(Identifier.of(val));
            temp.add(item);
         }

         return (T)temp;
      }
   }
}
