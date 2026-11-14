package net.hachimi.client.api.account.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.account.type.MinecraftAccount;
import net.hachimi.client.api.account.type.impl.CrackedAccount;
import net.hachimi.client.api.account.type.impl.MicrosoftAccount;
import net.hachimi.client.api.file.ConfigFile;
import net.hachimi.client.init.Managers;

public class AccountFile extends ConfigFile {
   public AccountFile(Path dir, String path) {
      super(dir, path);
   }

   public AccountFile(Path dir) {
      this(dir, "accounts");
   }

   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath, new LinkOption[0])) {
            Files.createFile(filepath);
         }

         this.write(filepath, this.saveAs());
      } catch (IOException e) {
         Hachimi.error("Could not save file for accounts.json!");
         e.printStackTrace();
      }

   }

   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath, new LinkOption[0])) {
            this.readFrom(this.read(filepath));
         }
      } catch (IOException e) {
         Hachimi.error("Could not read file for accounts.json!");
         e.printStackTrace();
      }

   }

   protected String saveAs() {
      JsonArray array = new JsonArray();

      for(MinecraftAccount account : Managers.ACCOUNT.getAccounts()) {
         try {
            array.add(account.toJSON());
         } catch (RuntimeException e) {
            Hachimi.error(e.getMessage());
         }
      }

      return this.serialize(array);
   }

   protected void readFrom(String content) {
      JsonArray json = this.parseArray(content);
      if (json != null) {
         for(JsonElement element : json.asList()) {
            if (element instanceof JsonObject) {
               JsonObject object = (JsonObject)element;
               MinecraftAccount account = null;
               if (object.has("email") && object.has("password")) {
                  account = new MicrosoftAccount(object.get("email").getAsString(), object.get("password").getAsString());
                  if (object.has("username")) {
                     ((MicrosoftAccount)account).setUsername(object.get("username").getAsString());
                  }
               } else if (object.has("token")) {
                  if (!object.has("username")) {
                     Hachimi.error("Browser account does not have a username set?");
                     continue;
                  }

                  account = new MicrosoftAccount(object.get("token").getAsString());
                  ((MicrosoftAccount)account).setUsername(object.get("username").getAsString());
               } else if (object.has("username")) {
                  account = new CrackedAccount(object.get("username").getAsString());
               }

               if (account != null) {
                  Managers.ACCOUNT.register(account);
               } else {
                  Hachimi.error("Could not parse account JSON.\nRaw: {}", object.toString());
               }
            }
         }

      }
   }
}
