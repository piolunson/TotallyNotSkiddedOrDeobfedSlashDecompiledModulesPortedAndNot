package net.hachimi.client.util;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Stream;

public class StreamUtils {
   public static <T, U extends Comparable<? super U>> Stream<T> sortCached(Stream<T> stream, Function<? super T, ? extends U> keyExtractor) {
      return stream.map((t) -> {
         U key = (U)((Comparable)keyExtractor.apply(t));
         return new Intermediary<T, U>(t, key);
      }).sorted(Comparator.comparing((Intermediary<T, U> i) -> i.key())).map((Intermediary<T, U> i) -> i.value());
   }

   private static record Intermediary<T, U extends Comparable<? super U>>(T value, U key) {
   }
}
