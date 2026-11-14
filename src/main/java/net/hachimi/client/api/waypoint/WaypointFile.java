package net.hachimi.client.api.waypoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.file.ConfigFile;
import net.hachimi.client.init.Managers;

public class WaypointFile extends ConfigFile {
   public WaypointFile(Path dir) {
      super(dir, "waypoints");
   }

   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath, new LinkOption[0])) {
            Files.createFile(filepath);
         }

         JsonArray array = new JsonArray();

         for(Waypoint point : Managers.WAYPOINT.getWaypoints()) {
            if (point instanceof UserWaypoint) {
               array.add(point.toJson());
            }
         }

         this.write(filepath, this.serialize(array));
      } catch (IOException e) {
         Hachimi.error("Could not save file for waypoints.json!");
         e.printStackTrace();
      }

   }

   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath, new LinkOption[0])) {
            String content = this.read(filepath);
            JsonArray array = this.parseArray(content);
            if (array == null) {
               return;
            }

            for(JsonElement e : array.asList()) {
               JsonObject obj = e.getAsJsonObject();
               if (obj.has("tag") && obj.has("dimension") && obj.has("ip")) {
                  JsonElement tag = obj.get("tag");
                  JsonElement ip = obj.get("ip");
                  JsonElement dimension = obj.get("dimension");
                  JsonElement x = obj.get("x");
                  JsonElement y = obj.get("y");
                  JsonElement z = obj.get("z");
                  Managers.WAYPOINT.register((Waypoint)(new UserWaypoint(tag.getAsString(), ip.getAsString(), dimension.getAsInt(), x.getAsDouble(), y.getAsDouble(), z.getAsDouble())));
               }
            }
         }
      } catch (IOException e) {
         Hachimi.error("Could not read file for waypoints.json!");
         e.printStackTrace();
      }

   }
}
