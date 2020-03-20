package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.database.DatabaseManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.entities.GuildImpl;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataScraper.class);

    private final DiscordManager discordManager;
    private final DatabaseManager databaseManager;
    private final BatchImageInserter batchImageInserter;
    private final ServerCache serverCache;

    public DataScraper(DiscordManager discordManager, DatabaseManager databaseManager, BatchImageInserter batchImageInserter, ServerCache serverCache) {
        this.discordManager = discordManager;
        this.databaseManager = databaseManager;
        this.batchImageInserter = batchImageInserter;
        this.serverCache = serverCache;
    }

    public CompletableFuture<Void> cleanData() {
        LOGGER.info("Cleaning data...");
        return databaseManager.getServers(false).thenAcceptAsync(incomplete -> {
            LOGGER.info("There are {} incompletely scraped servers. Clearing them now...", incomplete.size());
            CompletableFuture.allOf(incomplete
                    .stream()
                    .map(id -> new GuildImpl(null, id))
                    .map(this::deleteServer)
                    .toArray(CompletableFuture[]::new))
                    .thenRun(() -> LOGGER.info("Cleared dead servers"))
                    .join();
        }).thenApplyAsync($ -> databaseManager.getServers(true).thenAccept(complete -> {
            LOGGER.info("Complete servers: {}", complete);
            complete.forEach(serverCache::addServer);
        }).join());

    }

    public CompletableFuture<Void> deleteServer(Guild guild) {
        return databaseManager.deleteServer(guild)
                .thenRun(() -> databaseManager.deleteImages(guild));
    }

    public CompletableFuture<Void> scrapeServer(Guild guild) {
        return databaseManager.addServer(guild).thenRun(() -> {
            guild.getTextChannels().forEach(channel -> {
//            var channel = guild.getTextChannelById(689489789698834633L);
                LOGGER.info("Scraping {}", channel.getName());

                scrapeImages(channel).join();
            });
        }).exceptionally(t -> {
            LOGGER.error("Error!", t);
            return null;
        });
    }

    /**
     * Gets the byte hashes of the images in a given message.
     *
     * @param message The messages to get the images from
     * @return A list of bytes of all images in the given message
     */
    public List<byte[]> getImagesFrom(Message message) {
        return Stream.concat(
                message.getAttachments().parallelStream()
                        .filter(Message.Attachment::isImage)
                        .map(attachment -> getHash(attachment.retrieveInputStream().join())),
                message.getEmbeds().parallelStream()
                        .map(MessageEmbed::getThumbnail)
                        .filter(Objects::nonNull)
                        .map(MessageEmbed.Thumbnail::getProxyUrl)
                        .map(this::getHash))
                .collect(Collectors.toUnmodifiableList());
    }

    private CompletableFuture<Void> scrapeImages(TextChannel channel) {
        return CompletableFuture.runAsync(() ->
                channel.getIterableHistory().cache(false).forEachAsync(message -> {
                    getImagesFrom(message).forEach(hash -> batchImageInserter.addHash(message, hash));
                    return true;
                }).thenRunAsync(() -> {
                    batchImageInserter.flushSync();
                    databaseManager.updateServer(channel.getGuild(), true).join();
                    LOGGER.info("Done scraping images!");
                }));
    }

    private byte[] getHash(String url) {
        try {
            return getHash(new URL(url).openStream());
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private byte[] getHash(InputStream inputStream) {
        try {
            var md = MessageDigest.getInstance("SHA-512");
            md.update(toSafeByteArray(inputStream));
            return md.digest();
        } catch (NoSuchAlgorithmException ignored) {
            return new byte[0];
        }
    }

    private byte[] toSafeByteArray(InputStream inputStream) {
        try {
            var bytes = IOUtils.toByteArray(inputStream);
            inputStream.close();
            return bytes;
        } catch (IOException ignored) {
            return new byte[0];
        }
    }
}
