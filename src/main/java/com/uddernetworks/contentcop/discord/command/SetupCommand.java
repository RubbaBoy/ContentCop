package com.uddernetworks.contentcop.discord.command;

import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.discord.DataScraper;
import com.uddernetworks.contentcop.discord.EmbedUtils;
import com.uddernetworks.contentcop.discord.ServerCache;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupCommand.class);

    private final DatabaseManager databaseManager;
    private final DataScraper dataScraper;
    private final ServerCache serverCache;

    public SetupCommand(CommandManager commandManager) {
        super("setup", commandManager);
        var contentCop = commandManager.getContentCop();
        this.databaseManager = contentCop.getDatabaseManager();
        this.dataScraper = contentCop.getDataScraper();
        this.serverCache = contentCop.getServerCache();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        var guild = channel.getGuild();

//        databaseManager.getUser(author).thenAccept(currentReposts -> {
//            System.out.println("currentReposts = " + currentReposts);
//        }).join();
//
//        databaseManager.getUsers(author.getGuild()).thenAccept(map -> {
//            System.out.println("map = " + map);
//        }).join();

//        if (true) return;

         if (args.length == 0) {
            databaseManager.getServer(guild).thenAcceptAsync(optional ->
                    optional.ifPresentOrElse(complete -> {
                        if (complete) {
                            EmbedUtils.error(channel, author, "The server has already been set up!");
                        } else {
                            EmbedUtils.error(channel, author, "The server is currently in the process of being scraped. If it isn't, you're shit out of luck.");
                        }
                    }, () -> {
                        try {
                            EmbedUtils.sendEmbed(channel, author, "Setting up", "Setting up the server! Reading all images and storing them into the database.");
                            long start = System.currentTimeMillis();
                            dataScraper.scrapeServer(guild)
                                    .thenRun(() -> {
                                        databaseManager.updateServer(guild, true).join();
                                        serverCache.addServer(guild.getIdLong());
                                        LOGGER.info("Done scraping server after {}ms", System.currentTimeMillis() - start);
                                    });
                        } catch (Exception e) {
                            LOGGER.error("Error setting up server " + guild.getId(), e);
                        }
                    }));
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            EmbedUtils.sendEmbed(channel, author, "Resetting images", "Resetting the scraped images from the Discord server. This is for testing only.");
            dataScraper.deleteServer(guild);
            serverCache.removeServer(guild.getIdLong());
        } else {
            syntaxError(author, channel);
        }
    }

    private void syntaxError(Member author, TextChannel channel) {
        EmbedUtils.error(channel, author, String.format("Usage: %ssetup [reset]", commandManager.getPrefix()));
    }
}
