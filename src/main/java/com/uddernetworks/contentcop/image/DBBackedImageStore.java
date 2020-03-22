package com.uddernetworks.contentcop.image;

import com.uddernetworks.contentcop.database.DatabaseImage;
import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.discord.DummyGuild;
import com.uddernetworks.contentcop.utility.SEntry;
import com.uddernetworks.contentcop.utility.Utility;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DBBackedImageStore implements ImageStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBBackedImageStore.class);

    private static final ExecutorService common = Executors.newCachedThreadPool();

    private final DatabaseManager databaseManager;
    private final Map<Long, List<DatabaseImage>> images = new ConcurrentHashMap<>();

    public DBBackedImageStore(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private CompletableFuture<Map.Entry<Long, List<DatabaseImage>>> getPairedImages(Guild guild) {
        return databaseManager.getImages(guild).thenApply(images -> new SEntry<>(guild.getIdLong(), new ArrayList<>(images)));
    }

    public List<DatabaseImage> getImages(Guild guild) {
        return images.getOrDefault(guild.getIdLong(), Collections.emptyList());
    }

    @Override
    public CompletableFuture<Void> init() {
        return databaseManager.getServers(true).thenAccept(servers -> {
            Utility.allOfResult(servers.stream()
                    .map(DummyGuild::new)
                    .map(this::getPairedImages)
                    .collect(Collectors.toUnmodifiableList()))
                    .thenAccept(map -> images.putAll(map.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                    .join();
        });
    }

    @Override
    public CompletableFuture<Void> addImage(DatabaseImage image) {
        images.computeIfAbsent(image.getServer(), $ -> new ArrayList<>()).add(image);
        return databaseManager.addImage(image);
    }

    @Override
    public CompletableFuture<Void> addImages(List<DatabaseImage> images) {
        return Utility.allOf(images.stream().collect(Collectors.groupingBy(DatabaseImage::getServer)).entrySet().stream().map(entry -> {
            this.images.computeIfAbsent(entry.getKey(), $ -> new ArrayList<>()).addAll(entry.getValue());
            return databaseManager.addImages(entry.getValue());
        }).collect(Collectors.toUnmodifiableList()));
    }

    @Override
    public <T> Future<Stream<T>> iterateUntilImages(long server, Function<DatabaseImage, T> action) {
        if (!images.containsKey(server)) {
            return CompletableFuture.completedFuture(Stream.empty());
        }

        return common.submit(() -> images.get(server).parallelStream().map(action));
    }
}
