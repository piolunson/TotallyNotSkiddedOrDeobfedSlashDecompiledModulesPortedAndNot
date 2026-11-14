package net.hachimi.client.impl.event.world;

import java.awt.Color;
import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.util.math.Vec3d;

public class SkyboxEvent extends Event {
   private Color color;

   public Color getColor() {
      return this.color;
   }

   public Vec3d getColorVec() {
      return new Vec3d((double)this.color.getRed() / (double)255.0F, (double)this.color.getGreen() / (double)255.0F, (double)this.color.getBlue() / (double)255.0F);
   }

   public int getRGB() {
      return this.color.getRGB();
   }

   public void setColor(Color color) {
      this.color = color;
   }

   @Cancelable
   public static class Sky extends SkyboxEvent {
   }

   @Cancelable
   public static class Cloud extends SkyboxEvent {
   }

   @Cancelable
   public static class Fog extends SkyboxEvent {
      private final float tickDelta;

      public Fog(float tickDelta) {
         this.tickDelta = tickDelta;
      }

      public float getTickDelta() {
         return this.tickDelta;
      }
   }
}
