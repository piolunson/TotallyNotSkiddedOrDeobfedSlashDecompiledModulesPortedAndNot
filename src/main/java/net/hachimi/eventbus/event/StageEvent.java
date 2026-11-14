package net.hachimi.eventbus.event;

public class StageEvent extends Event {
   private EventStage stage;

   public EventStage getStage() {
      return this.stage;
   }

   public void setStage(EventStage stage) {
      this.stage = stage;
   }

   public static enum EventStage {
      PRE,
      POST;

      // $FF: synthetic method
      private static EventStage[] $values() {
         return new EventStage[]{PRE, POST};
      }
   }
}
