package net.hachimi.client.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.command.ConfigArgumentType;
import net.hachimi.client.api.command.ListArgumentType;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BlockListConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EntityListConfig;
import net.hachimi.client.api.config.setting.ItemListConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.macro.Macro;
import net.hachimi.client.api.module.Module;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.util.KeyboardUtil;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.command.CommandSource;
import net.minecraft.block.Block;

public class ModuleCommand extends Command {
   private final Module module;

   public ModuleCommand(Module module) {
      super(module.getName(), module.getDescription(), literal(module.getName().toLowerCase()));
      this.module = module;
   }

   public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
      ((LiteralArgumentBuilder)builder.then(((RequiredArgumentBuilder)argument("setting", ConfigArgumentType.config(this.module)).then(((RequiredArgumentBuilder)argument("value", StringArgumentType.string()).suggests((c, b) -> {
         Config<?> config = ConfigArgumentType.getConfig(c, "setting");
         if (config instanceof BlockListConfig || config instanceof ItemListConfig || config instanceof EntityListConfig) {
            b.suggest("add");
            b.suggest("del");
            b.suggest("remove");
            b.suggest("clear");
            b.suggest("list");
         }

         return b.buildFuture();
      }).executes((c) -> {
         Config<?> config = ConfigArgumentType.getConfig(c, "setting");
         String value = StringArgumentType.getString(c, "value");
         if (value.equalsIgnoreCase("list")) {
            return this.listItems(config, value);
         } else if (value.equalsIgnoreCase("reset")) {
            config.resetValue();
            ChatUtil.clientSendMessage("§7%s§f was reset to default value", config.getName());
            return 1;
         } else if (value.equalsIgnoreCase("clear")) {
            if (config instanceof ItemListConfig) {
               ((ItemListConfig)config).clear();
            } else if (config instanceof BlockListConfig) {
               ((BlockListConfig)config).clear();
            } else if (config instanceof EntityListConfig) {
               ((EntityListConfig)config).clear();
            }

            ChatUtil.clientSendMessage("§7%s§f was cleared", config.getName());
            return 1;
         } else {
            return this.updateValue(config, value);
         }
      })).then(argument("list", ListArgumentType.list()).executes((c) -> {
         Config<?> config = ConfigArgumentType.getConfig(c, "setting");
         if (!(config instanceof BlockListConfig) && !(config instanceof ItemListConfig) && !(config instanceof EntityListConfig)) {
            return 0;
         } else {
            String action = StringArgumentType.getString(c, "value");
            Object value = ListArgumentType.getListItem(c, "list");
            return this.addDeleteItem(config, action, value);
         }
      })))).executes((c) -> {
         ChatUtil.error("Must provide a value!");
         return 1;
      }))).executes((c) -> {
         Module patt0$temp = this.module;
         if (patt0$temp instanceof ToggleModule m) {
            m.toggle();
            Object[] var10001 = new Object[2];
            String var10004 = m.getName();
            var10001[0] = "§7" + var10004 + "§f";
            var10001[1] = m.isEnabled() ? "§senabled§f" : "§cdisabled§f";
            ChatUtil.clientSendMessage("%s is now %s", var10001);
         }

         return 1;
      });
   }

   private int addDeleteItem(Config<?> config, String action, Object value) {
      if (config instanceof ItemListConfig) {
         if (!(value instanceof Item)) {
            ChatUtil.error("Not an item!");
            return 0;
         }

         Item item = (Item)value;
         if (action.equalsIgnoreCase("add")) {
            ((ItemListConfig)config).add(item);
            String var10000 = item.getName().getString();
            ChatUtil.clientSendMessage("Added §s" + var10000 + "§f to §7" + config.getName());
         } else if (action.equalsIgnoreCase("del") || action.equalsIgnoreCase("remove")) {
            ((ItemListConfig)config).remove(item);
            String var8 = item.getName().getString();
            ChatUtil.clientSendMessage("Removed §c" + var8 + "§f from §7" + config.getName());
         }
      } else if (config instanceof BlockListConfig) {
         if (value instanceof Item) {
            Item item = (Item)value;
            value = Block.getBlockFromItem(item);
         }

         if (!(value instanceof Block)) {
            ChatUtil.error("Not a block!");
            return 0;
         }

         Block block = (Block)value;
         if (action.equalsIgnoreCase("add")) {
            ((BlockListConfig)config).add(block);
            String var9 = block.getName().getString();
            ChatUtil.clientSendMessage("Added §s" + var9 + "§f to §7" + config.getName());
         } else if (action.equalsIgnoreCase("del") || action.equalsIgnoreCase("remove")) {
            ((BlockListConfig)config).remove(block);
            String var10 = block.getName().getString();
            ChatUtil.clientSendMessage("Removed §c" + var10 + "§f from §7" + config.getName());
         }
      } else if (config instanceof EntityListConfig) {
         if (!(value instanceof EntityType)) {
            ChatUtil.error("Not an entity type!");
            return 0;
         }

         EntityType entity = (EntityType)value;
         if (action.equalsIgnoreCase("add")) {
            ((EntityListConfig)config).add(entity);
            String var11 = entity.getName().getString();
            ChatUtil.clientSendMessage("Added §s" + var11 + "§f to §7" + config.getName());
         } else if (action.equalsIgnoreCase("del") || action.equalsIgnoreCase("remove")) {
            ((EntityListConfig)config).remove(entity);
            String var12 = entity.getName().getString();
            ChatUtil.clientSendMessage("Removed §c" + var12 + "§f from §7" + config.getName());
         }
      }

      return 1;
   }

   private int listItems(Config<?> config, String action) {
      if (config instanceof ItemListConfig) {
         List<Item> list = (List)config.getValue();
         if (action.equalsIgnoreCase("list")) {
            if (list.isEmpty()) {
               ChatUtil.error("There are no items in the list!");
               return 1;
            }

            List<String> listString = new ArrayList<>();

            for(Item item : list) {
               listString.add(item.getName().getString());
            }

            String var10000 = config.getName();
            ChatUtil.clientSendMessage("§7" + var10000 + "§f: " + String.join(", ", listString));
         }
      } else if (config instanceof BlockListConfig) {
         List<Block> list = (List)config.getValue();
         if (action.equalsIgnoreCase("list")) {
            if (list.isEmpty()) {
               ChatUtil.error("There are no blocks in the list!");
               return 1;
            }

            List<String> listString = new ArrayList<>();

            for(Block block : list) {
               listString.add(block.getName().getString());
            }

            String var15 = config.getName();
            ChatUtil.clientSendMessage("§7" + var15 + "§f: " + String.join(", ", listString));
         }
      } else if (config instanceof EntityListConfig) {
         List<EntityType> list = (List)config.getValue();
         if (action.equalsIgnoreCase("list")) {
            if (list.isEmpty()) {
               ChatUtil.error("There are no entities in the list!");
               return 1;
            }

            List<String> listString = new ArrayList<>();

            for(EntityType entityType : list) {
               listString.add(entityType.getName().getString());
            }

            String var16 = config.getName();
            ChatUtil.clientSendMessage("§7" + var16 + "§f: " + String.join(", ", listString));
         }
      }

      return 1;
   }

   private int updateValue(Config config, String value) {
      if (config != null && value != null) {
         try {
            if (config.getValue() instanceof Integer) {
               Integer val = Integer.parseInt(value);
               if (val.doubleValue() < ((NumberConfig)config).getMin().doubleValue()) {
                  ChatUtil.error("Value less than min!");
                  return 0;
               }

               if (val.doubleValue() > ((NumberConfig)config).getMax().doubleValue()) {
                  ChatUtil.error("Value greater than max!");
                  return 0;
               }

               config.setValue(val);
               ChatUtil.clientSendMessage("§7%s§f was set to §s%s", config.getName(), val.toString());
            } else if (config.getValue() instanceof Float) {
               Float val = Float.parseFloat(value);
               if (val.doubleValue() < ((NumberConfig)config).getMin().doubleValue()) {
                  ChatUtil.error("Value less than min!");
                  return 0;
               }

               if (val.doubleValue() > ((NumberConfig)config).getMax().doubleValue()) {
                  ChatUtil.error("Value greater than max!");
                  return 0;
               }

               config.setValue(val);
               ChatUtil.clientSendMessage("§7%s§f was set to §s%s", config.getName(), val.toString());
            } else if (config.getValue() instanceof Double) {
               Double val = Double.parseDouble(value);
               if (val < ((NumberConfig)config).getMin().doubleValue()) {
                  ChatUtil.error("Value less than min!");
                  return 0;
               }

               if (val > ((NumberConfig)config).getMax().doubleValue()) {
                  ChatUtil.error("Value greater than max!");
                  return 0;
               }

               config.setValue(val);
               ChatUtil.clientSendMessage("§7%s§f was set to §s%s", config.getName(), val.toString());
            }
         } catch (NumberFormatException var8) {
            ChatUtil.error("Not a number!");
         }

         if (config.getValue() instanceof Boolean) {
            Boolean val = this.parseBoolean(value);
            if (val == null) {
               ChatUtil.error("Invalid value!");
               return 0;
            }

            config.setValue(val);
            ChatUtil.clientSendMessage("§7%s§f was set to §s%s", config.getName(), val ? "True" : "False");
         } else if (config.getValue() instanceof Enum) {
            String[] values = (String[])Arrays.stream((Enum[])((Enum)config.getValue()).getClass().getEnumConstants()).map(Enum::name).toArray((x$0) -> new String[x$0]);
            int ix = -1;

            for(int i = 0; i < values.length; ++i) {
               if (values[i].equalsIgnoreCase(value)) {
                  ix = i;
                  break;
               }
            }

            if (ix == -1) {
               ChatUtil.error("Not a valid mode!");
               return 0;
            }

            Enum<?> val = Enum.valueOf(((Enum)config.getValue()).getClass(), values[ix]);
            config.setValue(val);
            ChatUtil.clientSendMessage("§7%s§f was set to mode §s%s", config.getName(), value);
         } else {
            Object color = config.getValue();
            if (color instanceof Macro) {
               Macro macro = (Macro)color;
               if (config.getName().equalsIgnoreCase("Keybind")) {
                  ChatUtil.error("Use the 'bind' command to keybind modules!");
                  return 0;
               }

               config.setValue(new Macro(config.getId(), KeyboardUtil.getKeyCode(value), macro.getRunnable()));
               ChatUtil.clientSendMessage("§7%s§f was set to key §s%s", config.getName(), value);
            } else if (config.getValue() instanceof String) {
               config.setValue(value);
            } else if (config.getValue() instanceof Color) {
               try {
                  Color color_dup = ((ColorConfig)config).parseColor(value);
                  config.setValue(color_dup);
                  ChatUtil.clientSendMessage("§7%s§f was set to §s%s", config.getName(), value);
               } catch (IllegalArgumentException var7) {
                  ChatUtil.error("Invalid color_dup!");
               }
            }
         }

         return 1;
      } else {
         return 0;
      }
   }

   private Boolean parseBoolean(String string) {
      if (!string.equalsIgnoreCase("True") && !string.equalsIgnoreCase("On")) {
         return !string.equalsIgnoreCase("False") && !string.equalsIgnoreCase("Off") ? null : false;
      } else {
         return true;
      }
   }
}
