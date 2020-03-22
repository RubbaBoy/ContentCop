package com.uddernetworks.contentcop.utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utility {

    private static final Set<Collector.Characteristics> CH_ID
            = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

    public static String readResource(String resource) {
        return new BufferedReader(new InputStreamReader(Utility.class.getResourceAsStream("/" + resource))).lines().collect(Collectors.joining("\n"));
    }

    public static <T> Optional<T> getFirst(List<T> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(list.get(0));
    }

    public static <T> CompletableFuture<Void> allOf(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public static <T> CompletableFuture<List<T>> allOfResult(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply($ -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toUnmodifiableList()));
    }

    public static <K, V> Map<K, V> mapFrom(Stream<Entry<K, V>> stream) {
        return stream.collect(mapFromList());
    }

    public static <T extends Entry<K, U>, K, U> Collector<T, ?, Map<K,U>> mapFromList() {
        return Collectors.toMap(Entry::getKey, Entry::getValue);
    }

    public static <K, V> void ifPresent(Optional<Entry<K, V>> optional, BiConsumer<K, V> consumer) {
        optional.ifPresent(entry -> consumer.accept(entry.getKey(), entry.getValue()));
    }

    public static <K, V> void ifPresentOrElse(Optional<Entry<K, V>> optional, BiConsumer<K, V> consumer, Runnable runnable) {
        optional.ifPresentOrElse(entry -> consumer.accept(entry.getKey(), entry.getValue()), runnable);
    }
}

