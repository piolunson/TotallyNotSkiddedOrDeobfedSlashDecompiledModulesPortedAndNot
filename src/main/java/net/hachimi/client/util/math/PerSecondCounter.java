package net.hachimi.client.util.math;

import java.util.LinkedList;

public class PerSecondCounter {
   private final LinkedList<Long> counter = new LinkedList();

   public void updateCounter() {
      this.counter.add(System.currentTimeMillis() + 1000L);
   }

   public int getPerSecond() {
      long time = System.currentTimeMillis();

      try {
         while(!this.counter.isEmpty() && this.counter.peek() != null && (Long)this.counter.peek() < time) {
            this.counter.remove();
         }
      } catch (Exception e) {
         this.counter.clear();
         e.printStackTrace();
      }

      return this.counter.size();
   }
}
