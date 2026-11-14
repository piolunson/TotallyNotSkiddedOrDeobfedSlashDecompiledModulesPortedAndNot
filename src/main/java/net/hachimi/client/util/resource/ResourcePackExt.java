package net.hachimi.client.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.resource.ModResourcePack;
import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePackInfo;
import org.jetbrains.annotations.Nullable;

public final class ResourcePackExt implements net.minecraft.resource.ResourcePack, ModResourcePack {
   public static final Set<String> REGISTERED_SOUND_FILES = new HashSet<>();
   private final ModNioResourcePack parent;

   public ResourcePackExt(ModNioResourcePack parent) {
      this.parent = parent;
   }

   public @Nullable InputSupplier<InputStream> openRoot(String... segments) {
      return this.parent.openRoot(segments);
   }

   public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
      String formattedName = String.format("assets/hachimiclient/%s", id.getPath());
      InputStream is = (InputStream)this.getResourceInternal(formattedName);
      return is != null ? () -> is : this.parent.open(type, id);
   }

   private Object getResourceInternal(String name) {
      InputStream is = this.getClass().getClassLoader().getResourceAsStream(name);
      if (is == null) {
         if (name.startsWith("assets/")) {
            is = this.getClass().getClassLoader().getResourceAsStream(name.substring(7));
         }

         if (is == null && !name.startsWith("/")) {
            is = this.getClass().getClassLoader().getResourceAsStream("/" + name);
         }
      }

      return is;
   }

   public void findResources(ResourceType type, String namespace, String prefix, net.minecraft.resource.ResourcePack.ResultConsumer consumer) {
      if (prefix.equals("sounds")) {
         for(String soundFile : REGISTERED_SOUND_FILES) {
            String formattedName = String.format("assets/hachimiclient/sounds/%s", soundFile);
            InputStream is = (InputStream)this.getResourceInternal(formattedName);
            if (is != null) {
               Identifier id = Identifier.of("hachimi", String.format("sounds/%s", soundFile));
               consumer.accept(id, (InputSupplier)() -> is);
            }
         }
      }

      this.parent.findResources(type, namespace, prefix, consumer);
   }

   public Set<String> getNamespaces(ResourceType type) {
      return this.parent.getNamespaces(type);
   }

   public <T> @Nullable T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
      return (T)this.parent.parseMetadata(metaReader);
   }

   public ResourcePackInfo getInfo() {
      return this.parent.getInfo();
   }

   public ModMetadata getFabricModMetadata() {
      return this.parent.getFabricModMetadata();
   }

   public ModResourcePack createOverlay(String overlay) {
      return this.parent.createOverlay(overlay);
   }

   public void close() {
      this.parent.close();
   }
}
