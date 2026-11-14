package net.hachimi.eventbus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.Event;

public class EventBus implements EventHandler {
   public static final EventBus INSTANCE = new EventBus();
   private final Set<Object> subscribers = Collections.synchronizedSet(new HashSet<>());
   private final Map<Object, PriorityQueue<Listener>> listeners = new ConcurrentHashMap<>();

   public void subscribe(Object obj) {
      if (!this.subscribers.contains(obj)) {
         this.subscribers.add(obj);

         for(Method method : obj.getClass().getMethods()) {
            method.trySetAccessible();
            if (method.isAnnotationPresent(EventListener.class)) {
               EventListener listener = (EventListener)method.getAnnotation(EventListener.class);
               Class<?>[] params;
               if (method.getReturnType() == Void.TYPE && (params = method.getParameterTypes()).length == 1) {
                  PriorityQueue active = (PriorityQueue)this.listeners.computeIfAbsent(params[0], (v) -> new PriorityQueue<>());
                  active.add(new Listener(method, obj, listener.receiveCanceled(), listener.priority()));
               }
            }
         }

      }
   }

   public void unsubscribe(Object obj) {
      if (this.subscribers.remove(obj)) {
         this.listeners.values().forEach((set) -> set.removeIf((l) -> l.getSubscriber() == obj));
         this.listeners.entrySet().removeIf((e) -> ((PriorityQueue)e.getValue()).isEmpty());
      }

   }

   public boolean dispatch(Event event) {
      if (event == null) {
         return false;
      } else {
         PriorityQueue<Listener> active = (PriorityQueue)this.listeners.get(event.getClass());
         if (active != null && !active.isEmpty()) {
            for(Listener listener : new ArrayList<>(active)) {
               if (!event.isCanceled() || listener.isReceiveCanceled()) {
                  listener.invokeSubscriber(event);
               }
            }

            return event.isCanceled();
         } else {
            return false;
         }
      }
   }
}
