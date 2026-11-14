package net.hachimi.client.api.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.file.ConfigFile;
import net.hachimi.client.init.Managers;

public class SocialFile extends ConfigFile {
   private final SocialRelation relation;

   public SocialFile(Path dir, SocialRelation relation) {
      super(dir, relation.name());
      this.relation = relation;
   }

   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath, new LinkOption[0])) {
            Files.createFile(filepath);
         }

         JsonArray array = new JsonArray();

         for(String socials : Managers.SOCIAL.getRelations(this.relation)) {
            array.add(new JsonPrimitive(socials));
         }

         this.write(filepath, this.serialize(array));
      } catch (IOException e) {
         Hachimi.error("Could not save file for {}.json!", this.relation.name().toLowerCase());
         e.printStackTrace();
      }

   }

   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath, new LinkOption[0])) {
            String content = this.read(filepath);
            JsonArray json = this.parseArray(content);
            if (json == null) {
               return;
            }

            for(JsonElement element : json.asList()) {
               Managers.SOCIAL.addRelation(element.getAsString(), this.relation);
            }
         }
      } catch (IOException e) {
         Hachimi.error("Could not read file for {}.json!", this.relation.name().toLowerCase());
         e.printStackTrace();
      }

   }
}
