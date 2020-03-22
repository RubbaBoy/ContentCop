package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.database.DatabaseImage;
import com.uddernetworks.contentcop.image.ImageStore;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BatchImageInserter {

    private final ImageStore imageStore;
    private final List<DatabaseImage> hashes = Collections.synchronizedList(new ArrayList<>());

    public BatchImageInserter(ImageStore imageStore) {
        this.imageStore = imageStore;
    }

    public CompletableFuture<Void> addHash(Message message, byte[] bytes) {
        hashes.add(new DatabaseImage(message, bytes));
        if (hashes.size() >= 10000) {
            return flush();
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> flush() {
        if (hashes.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var future = imageStore.addImages(new ArrayList<>(hashes));
        hashes.clear();
        return future;
    }

    public void flushSync() {
        flush().join();
    }

}
