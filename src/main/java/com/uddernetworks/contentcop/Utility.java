package com.uddernetworks.contentcop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Utility {
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
}
