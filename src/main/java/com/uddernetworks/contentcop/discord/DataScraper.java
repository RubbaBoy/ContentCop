package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.ImageProcessor;
import com.uddernetworks.contentcop.database.DatabaseManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataScraper.class);
    private static final Pattern URL_PATTERN = Pattern.compile("(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*))");

    private final DiscordManager discordManager;
    private final DatabaseManager databaseManager;
    private final BatchImageInserter batchImageInserter;
    private final ServerCache serverCache;
    private final ImageProcessor imageProcessor;

    public DataScraper(DiscordManager discordManager, DatabaseManager databaseManager, BatchImageInserter batchImageInserter, ServerCache serverCache, ImageProcessor imageProcessor) {
        this.discordManager = discordManager;
        this.databaseManager = databaseManager;
        this.batchImageInserter = batchImageInserter;
        this.serverCache = serverCache;
        this.imageProcessor = imageProcessor;
    }

    public CompletableFuture<Void> cleanData() {
        LOGGER.info("Cleaning data...");
        return databaseManager.getServers(false).thenAcceptAsync(incomplete -> {
            LOGGER.info("There are {} incompletely scraped servers. Clearing them now...", incomplete.size());
            CompletableFuture.allOf(incomplete
                    .stream()
                    .map(DummyGuild::new)
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
            CompletableFuture.allOf(guild.getTextChannels().stream().map(channel -> {
                LOGGER.info("Scraping #{}", channel.getName());
                return scrapeImages(channel).thenRun(() -> {
                    LOGGER.info("Done scraping #{}", channel.getName());
                });
            }).toArray(CompletableFuture[]::new)).thenRun(() -> {
                databaseManager.updateServer(guild, true).join();
                LOGGER.info("Done scraping everything!");
            }).join();

//            var message = guild.getTextChannelById(689489789698834633L).retrieveMessageById(691128873320185967L).complete();
//            getImagesFrom(message).forEach(hash -> batchImageInserter.addHash(message, hash.toByteArray()).join());

//            batchImageInserter.flushSync();

//            databaseManager.updateServer(guild, true).join();
//            LOGGER.info("bruhhhhh");
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
    public List<BitSet> getImagesFrom(Message message) {
        var embeds = Collections.<String>emptyList();

        if (!message.getFlags().contains(Message.MessageFlag.EMBEDS_SUPPRESSED)) {
            embeds = message.getEmbeds().parallelStream()
                    .map(MessageEmbed::getThumbnail)
                    .filter(Objects::nonNull)
                    .map(MessageEmbed.Thumbnail::getProxyUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableList());
        }

        if (embeds.isEmpty()) {
            embeds = URL_PATTERN.matcher(message.getContentRaw())
                    .results()
                    .map(result -> result.group(1))
                    .collect(Collectors.toUnmodifiableList());
        }

        return Stream.concat(
                message.getAttachments().parallelStream()
                        .filter(Message.Attachment::isImage)
                        .map(attachment -> imageProcessor.getHash(attachment.retrieveInputStream().join())),
                embeds.stream()
                        .map(this::getHash)
                        .filter(Objects::nonNull)
                        .map(imageProcessor::getHash)
        )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableList());
    }

    private CompletableFuture<Void> scrapeImages(TextChannel channel) {
        return CompletableFuture.runAsync(() -> {
            var history = channel.getHistory();

            int maxMessages = 100_000; // 10k messages per channel max
            int retrieveSize = 100; // 100 is the API's max

            for (int i = 0; i < maxMessages / retrieveSize; i++) {
                var curr = history.retrievePast(retrieveSize).complete();

                curr.forEach(message -> getImagesFrom(message).forEach(hash -> batchImageInserter.addHash(message, hash.toByteArray()).join()));

                if (curr.size() != maxMessages) {
                    break;
                }
            }

            batchImageInserter.flushSync();
        });
    }

    private InputStream getHash(String url) {
        try {
            return new URL(url).openStream();
        } catch (IOException ignored) {
            return null;
        }
    }
}
