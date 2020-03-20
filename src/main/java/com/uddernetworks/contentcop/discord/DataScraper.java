package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.database.DatabaseManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DataScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataScraper.class);

    private final DiscordManager discordManager;
    private final DatabaseManager databaseManager;

    private final Map<Message, byte[]> hashes = new ConcurrentHashMap<>();

    public DataScraper(DiscordManager discordManager, DatabaseManager databaseManager) {
        this.discordManager = discordManager;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> deleteServer(Guild guild) {
        return databaseManager.deleteServer(guild)
                .thenRun(() -> databaseManager.deleteImages(guild));
    }

    public CompletableFuture<Void> scrapeServer(Guild guild) {
        LOGGER.info("scrappeeeeeee");
        return databaseManager.addServer(guild).thenRun(() -> {
//            guild.getTextChannels().forEach(channel -> {
            var channel = guild.getTextChannelById(689489789698834633L);

                LOGGER.info("Scraping {}", channel.getName());

                scrapeImages(channel);
//            });
        }).exceptionally(t -> {
            LOGGER.error("Error!", t);
            return null;
        });
    }

    private CompletableFuture<Void> scrapeImages(TextChannel channel) {
        return CompletableFuture.runAsync(() ->
                channel.getIterableHistory().cache(false).forEachAsync(message -> {
                    message.getAttachments().stream()
                            .filter(Message.Attachment::isImage)
                            .map(attachment -> getHash(attachment.retrieveInputStream().join()))
                            .forEach(hash -> addHash(message, hash));

                    LOGGER.info(message.getContentRaw());

                    return true;
                }).thenRun(() -> {
                    flushHashes().join();
                    LOGGER.info("Done scraping images!");
                }));
    }

    private CompletableFuture<Void> addHash(Message message, byte[] bytes) {
        hashes.put(message, bytes);
        if (hashes.size() >= 10000) {
            return flushHashes();
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> flushHashes() {
        var future = databaseManager.addImages(new HashMap<>(hashes));
        hashes.clear();
        return future;
    }

    private byte[] getHash(InputStream inputStream) {
        try {
            var md = MessageDigest.getInstance("MD5");
            md.update(toSafeByteArray(inputStream));
            return md.digest();
        } catch (NoSuchAlgorithmException ignored) {
            return new byte[0];
        }
    }

    private byte[] toSafeByteArray(InputStream inputStream) {
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (Exception ignored) {
            return new byte[0];
        }
    }
}
