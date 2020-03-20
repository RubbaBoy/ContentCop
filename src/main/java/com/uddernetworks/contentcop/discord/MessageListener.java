package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.database.DatabaseImage;
import com.uddernetworks.contentcop.database.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MessageListener extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);

    private final JDA jda;
    private final DatabaseManager databaseManager;
    private final DataScraper dataScraper;
    private final BatchImageInserter batchImageInserter;
    private final ServerCache serverCache;

    public MessageListener(JDA jda, DatabaseManager databaseManager, DataScraper dataScraper, BatchImageInserter batchImageInserter, ServerCache serverCache) {
        this.jda = jda;
        this.databaseManager = databaseManager;
        this.dataScraper = dataScraper;
        this.batchImageInserter = batchImageInserter;
        this.serverCache = serverCache;
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        var server = event.getGuild();
        var message = event.getMessage();
        var member = event.getMember();

        serverCache.checkingServer(server.getIdLong()).thenAcceptAsync(checking -> {
            if (checking) {
                LOGGER.info("Checking message...");

                var images = dataScraper.getImagesFrom(event.getMessage());

                if (images.isEmpty()) {
                    return;
                }

                images.forEach(hash -> {
                    var reposts = new ArrayList<DatabaseImage>();
                    databaseManager.getImage(server, hash).join().ifPresentOrElse(image -> {
                        LOGGER.info("Repost detected!");
                        reposts.add(image);
                    }, () -> {
                        LOGGER.info("Looks unique! Adding it to the database");
                        batchImageInserter.addHash(message, hash).join();
                    });

                    if (reposts.isEmpty()) {
                        return;
                    }

                    batchImageInserter.flushSync();
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
                                " [here](https://canary.discordapp.com/channels/" + first.getServer() + "/" + first.getChannel() + "/" + first.getMessage() + ")";
                    } else {
                        description = "You have reposted " + reposts.size() + " images! The following are the original sources and authors:\n" +
                                reposts.stream().map(image -> getName(image.getAuthor()) +
                                        " [here](https://canary.discordapp.com/channels/" + image.getServer() + "/" + image.getChannel() + "/" + image.getMessage() + ")")
                                        .collect(Collectors.joining("\n"));
                    }

                    EmbedUtils.sendEmbed(event.getChannel(), member, "Repost detected!", embed ->
                            embed.setDescription(description + "\n\nYou currently have **" + (reposts.size() + currentReposts) + " reposts.** Bruh.")
                                    .setColor(Color.RED));
                });
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
}
