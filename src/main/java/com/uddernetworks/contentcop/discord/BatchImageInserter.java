package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.database.DatabaseManager;
import net.dv8tion.jda.api.entities.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BatchImageInserter {

    private final DatabaseManager databaseManager;
    private final Map<Message, byte[]> hashes = new ConcurrentHashMap<>();

    public BatchImageInserter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> addHash(Message message, byte[] bytes) {
        hashes.put(message, bytes);
        if (hashes.size() >= 10000) {
            return flush();
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> flush() {
        if (hashes.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var future = databaseManager.addImages(new HashMap<>(hashes));
        hashes.clear();
        return future;
    }

    public void flushSync() {
        flush().join();
    }

}
