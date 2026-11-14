package net.hachimi.client.impl.gui.click;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.file.ConfigFile;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.gui.click.impl.config.CategoryFrame;
import net.hachimi.client.impl.module.client.ClickGuiModule;

public class ClickGuiFile extends ConfigFile {
   public ClickGuiFile(Path dir) {
      super(dir, "clickgui");
   }

   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath, new LinkOption[0])) {
            Files.createFile(filepath);
         }

         JsonArray object = new JsonArray();

         for(CategoryFrame frame : ClickGuiModule.CLICK_GUI_SCREEN.getCategoryFrames()) {
            object.add(frame.toJson());
         }

         this.write(filepath, this.serialize(object));
      } catch (IOException e) {
         Hachimi.error("Could not save clickgui file!");
         e.printStackTrace();
      }

   }

   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath, new LinkOption[0])) {
            String content = this.read(filepath);
            JsonArray object = this.parseArray(content);

            for(JsonElement element : object.getAsJsonArray()) {
               JsonObject jsonObject = element.getAsJsonObject();
               if (jsonObject.has("category")) {
                  JsonElement category = jsonObject.get("category");
                  CategoryFrame frame = this.getFrame(ModuleCategory.valueOf(category.getAsString()));
                  frame.fromJson(jsonObject);
               }
            }
         }
      } catch (IOException e) {
         Hachimi.error("Could not read clickgui file!");
         e.printStackTrace();
      }

   }

   public CategoryFrame getFrame(ModuleCategory category) {
      for(CategoryFrame frame : ClickGuiModule.CLICK_GUI_SCREEN.getCategoryFrames()) {
         if (frame.getCategory().equals(category)) {
            return frame;
         }
      }

      return null;
   }
}
