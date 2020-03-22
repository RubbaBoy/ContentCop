package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.ImageProcessor;
import com.uddernetworks.contentcop.database.DatabaseImage;
import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.utility.Utility;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.uddernetworks.contentcop.utility.Utility.getFirst;
import static com.uddernetworks.contentcop.utility.Utility.ifPresentOrElse;

public class MessageListener extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);

    private final JDA jda;
    private final DatabaseManager databaseManager;
    private final DataScraper dataScraper;
    private final BatchImageInserter batchImageInserter;
    private final ServerCache serverCache;
    private final ImageProcessor imageProcessor;

    public MessageListener(JDA jda, DatabaseManager databaseManager, DataScraper dataScraper, BatchImageInserter batchImageInserter, ServerCache serverCache, ImageProcessor imageProcessor) {
        this.jda = jda;
        this.databaseManager = databaseManager;
        this.dataScraper = dataScraper;
        this.batchImageInserter = batchImageInserter;
        this.serverCache = serverCache;
        this.imageProcessor = imageProcessor;
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        var server = event.getGuild();
        var message = event.getMessage();
        var member = event.getMember();

        if (event.getAuthor().isBot()) {
            return;
        }

//        if (server.getIdLong() != 265592530828001281L) {
//            return;
//        }

        serverCache.checkingServer(server.getIdLong()).thenAcceptAsync(checking -> {
            if (checking) {
                LOGGER.debug("Checking message...");

                try {
                    var images = dataScraper.getImagesFrom(event.getMessage());

                    if (images.isEmpty()) {
                        return;
                    }

                    images.forEach(hash -> {
                        var reposts = new ArrayList<FoundDatabaseImage>();

                        try {
                            var topStream = imageProcessor.getMatching(server.getIdLong(), hash).collect(Collectors.toUnmodifiableList());
                            System.out.println("topStream = " + topStream);

                            ifPresentOrElse(getFirst(topStream), (image, percentage) -> {
                                if (percentage >= 0.8) {
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

                        String description;
                        if (reposts.size() == 1) {
                            var first = reposts.get(0);
                            description = "You have reposted an image! This was last posted by " + getName(first.getAuthor()) +
                                    " [here](https://canary.discordapp.com/channels/" + first.getServer() + "/" + first.getChannel() + "/" + first.getMessage() + ") with a " + first.getDisplayPercent() + " match";
                        } else {
                            description = "You have reposted " + reposts.size() + " images! The following are the original sources and authors:\n" +
                                    reposts.stream().map(image -> getName(image.getAuthor()) +
                                            " [here](https://canary.discordapp.com/channels/" + image.getServer() + "/" + image.getChannel() + "/" + image.getMessage() + ") - " + image.getDisplayPercent() + " match")
                                            .collect(Collectors.joining("\n"));
                        }

                        EmbedUtils.sendEmbed(event.getChannel(), member, "Repost detected!", embed ->
                                embed.setDescription(description + "\n\nYou currently have **" + (reposts.size() + currentReposts) + " reposts.** Bruh.")
                                        .setColor(Color.RED), false);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
}
