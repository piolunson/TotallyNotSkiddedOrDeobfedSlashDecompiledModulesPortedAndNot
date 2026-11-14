package net.hachimi.client.impl.event.world;

import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;

public class RemoveEntityEvent extends Event implements Globals {
   private final net.minecraft.entity.Entity entity;
   private final net.minecraft.entity.Entity.RemovalReason removalReason;

   public RemoveEntityEvent(net.minecraft.entity.Entity entity, net.minecraft.entity.Entity.RemovalReason removalReason) {
      this.entity = entity;
      this.removalReason = removalReason;
   }

   public net.minecraft.entity.Entity getEntity() {
      return this.entity;
   }

   public net.minecraft.entity.Entity.RemovalReason getRemovalReason() {
      return this.removalReason;
   }
}
