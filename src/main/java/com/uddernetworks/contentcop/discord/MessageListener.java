package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.ImageProcessor;
import com.uddernetworks.contentcop.config.Config;
import com.uddernetworks.contentcop.config.ConfigManager;
import com.uddernetworks.contentcop.database.DatabaseImage;
import com.uddernetworks.contentcop.database.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.uddernetworks.contentcop.utility.Utility.getFirst;
import static com.uddernetworks.contentcop.utility.Utility.ifPresentOrElse;

public class MessageListener extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);

    private final Map<Long, PendingRepostEmbed> reactions = new ConcurrentHashMap<>();

    private final JDA jda;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final DataScraper dataScraper;
    private final BatchImageInserter batchImageInserter;
    private final ServerCache serverCache;
    private final ImageProcessor imageProcessor;

    private Emote COP;

    public MessageListener(JDA jda, ConfigManager configManager, DatabaseManager databaseManager, DataScraper dataScraper, BatchImageInserter batchImageInserter, ServerCache serverCache, ImageProcessor imageProcessor) {
        this.jda = jda;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.dataScraper = dataScraper;
        this.batchImageInserter = batchImageInserter;
        this.serverCache = serverCache;
        this.imageProcessor = imageProcessor;
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        this.COP = jda.getEmoteById(configManager.<Long>get(Config.EMOTE));
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        var server = event.getGuild();
        var message = event.getMessage();
        var member = event.getMember();

        if (event.getAuthor().isBot()) {
            return;
        }

        serverCache.checkingServer(server.getIdLong()).thenAcceptAsync(checking -> {
            if (checking) {
                try {
                    long start = System.nanoTime();
                    var images = dataScraper.getImagesFrom(event.getMessage());

                    if (images.isEmpty()) {
                        return;
                    }

                    var reposts = new ArrayList<FoundDatabaseImage>();

                    images.forEach(hash -> {
                        try {
                            var topStream = imageProcessor.getMatching(server.getIdLong(), hash).collect(Collectors.toUnmodifiableList());

                            ifPresentOrElse(getFirst(topStream), (image, percentage) -> {
                                if (percentage >= 0.95) {
                                    LOGGER.debug("Repost detected! Matches {}%", percentage * 100D);
                                    reposts.add(new FoundDatabaseImage(image, percentage));
                                } else {
                                    LOGGER.info("Not a repost, closest match was {}% adding to database", percentage * 100D);
                                    batchImageInserter.addHash(message, hash.toByteArray()).join();
                                }
                            }, () -> {
                                LOGGER.debug("Looks unique! Adding it to the database");
                                batchImageInserter.addHash(message, hash.toByteArray()).join();
                            });
                        } catch (InterruptedException | ExecutionException e) {
                            LOGGER.error("An error occurred while finding matching images", e);
                        }
                    });

                    var time = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);

                    batchImageInserter.flushSync();

                    if (reposts.isEmpty()) {
                        return;
                    }

                    var currentReposts = databaseManager.getUser(member).join();

                    if (currentReposts == 0) {
                        databaseManager.addUser(member, reposts.size()).join();
                    } else {
                        databaseManager.incrementUser(member, reposts.size()).join();
                    }

                    try {
                        message.addReaction(COP).queue();
                        reactions.put(message.getIdLong(), new PendingRepostEmbed(jda, member, message.getIdLong(), reposts, reposts.size() + currentReposts, time));
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    LOGGER.error("An error occurred while checking a message", e);
                }
            }
        });
    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return;
        }

        reactions.computeIfPresent(event.getMessageIdLong(), ($, pendingEmbed) -> {
            if (pendingEmbed.isShowing()) {
                return pendingEmbed;
            }

            var username = pendingEmbed.getAuthor().getNickname();
            var reposts = pendingEmbed.getImages();
            var repostCount = pendingEmbed.getUserReposts();

            String description;
            if (reposts.size() == 1) {
                var first = reposts.get(0);
                description = username + " has reposted an image! This was last posted by " + getName(first.getAuthor()) +
                        " [here](https://canary.discordapp.com/channels/" + first.getServer() + "/" + first.getChannel() + "/" + first.getMessage() + ") with a " + first.getDisplayPercent() + " match";
            } else {
                description = username + " has reposted " + reposts.size() + " images! The following are the original sources and authors:\n" +
                        reposts.stream().map(image -> getName(image.getAuthor()) +
                                " [here](https://canary.discordapp.com/channels/" + image.getServer() + "/" + image.getChannel() + "/" + image.getMessage() + ") - " + image.getDisplayPercent() + " match")
                                .collect(Collectors.joining("\n"));
            }

            var sentEmbed = EmbedUtils.sendEmbed(event.getChannel(), event.getMember(), "Repost detected!", "Processed in " + (pendingEmbed.getProcessingTime() / 1000D) + "ms", embed ->
                    embed.setDescription(description + "\n\nYou currently have **" + repostCount + " reposts.** Bruh.")
                            .setColor(Color.RED));

            pendingEmbed.setSentEmbed(sentEmbed);

            return pendingEmbed;
        });
    }

    private String getName(long id) {
        var user = jda.getUserById(id);
        if (user == null) {
            return "Unknown";
        }
        return user.getName();
    }

    private static class FoundDatabaseImage extends DatabaseImage {

        private final double percentage;

        public FoundDatabaseImage(DatabaseImage image, double percentage) {
            super(image);
            this.percentage = percentage;
        }

        public double getPercentage() {
            return percentage;
        }

        public String getDisplayPercent() {
            return NumberFormat.getPercentInstance().format(percentage);
        }
    }

    private static class PendingRepostEmbed {
        private final JDA jda;
        private final Member author;
        private final long originalMessage;
        private final List<FoundDatabaseImage> images;
        private final int userReposts;
        private final long processingTime; // Time in microseconds

        private long sentChannel = -1;
        private long sentEmbed = 1;

        private PendingRepostEmbed(JDA jda, Member author, long originalMessage, List<FoundDatabaseImage> images, int userReposts, long processingTime) {
            this.jda = jda;
            this.author = author;
            this.originalMessage = originalMessage;
            this.images = images;
            this.userReposts = userReposts;
            this.processingTime = processingTime;
        }

        public Member getAuthor() {
            return author;
        }

        public long getOriginalMessage() {
            return originalMessage;
        }

        public List<FoundDatabaseImage> getImages() {
            return images;
        }

        public int getUserReposts() {
            return userReposts;
        }

        public long getProcessingTime() {
            return processingTime;
        }

        public void setSentEmbed(Message embed) {
            this.sentEmbed = embed.getIdLong();
            this.sentChannel = embed.getTextChannel().getIdLong();
        }

        public long getSentEmbed() {
            return sentEmbed;
        }

        public boolean isShowing() {
            if (sentChannel == -1 || sentEmbed == -1) {
                invalidateEmbed();
                return false;
            }

            var channel = jda.getTextChannelById(sentChannel);
            if (channel == null) {
                invalidateEmbed();
                return false;
            }

            try {
                channel.retrieveMessageById(sentEmbed).complete();
                return true;
            } catch (Exception ignored) {
                invalidateEmbed();
                return false;
            }
        }

        private void invalidateEmbed() {
            sentEmbed = sentChannel = -1;
        }
    }
}
