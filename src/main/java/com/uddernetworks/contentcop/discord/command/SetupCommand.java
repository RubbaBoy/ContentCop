package com.uddernetworks.contentcop.discord.command;

import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.discord.DataScraper;
import com.uddernetworks.contentcop.discord.EmbedUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetupCommand.class);

    private final DatabaseManager databaseManager;
    private final DataScraper dataScraper;

    public SetupCommand(CommandManager commandManager) {
        super("setup", commandManager);
        var contentCop = commandManager.getContentCop();
        this.databaseManager = contentCop.getDatabaseManager();
        this.dataScraper = contentCop.getDataScraper();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        var guild = channel.getGuild();

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
                            LOGGER.info("bruh one");
                            long start = System.currentTimeMillis();
                            dataScraper.scrapeServer(guild)
                                    .thenRun(() -> LOGGER.info("Done scraping server after {}ms", System.currentTimeMillis() - start));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }));
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            EmbedUtils.sendEmbed(channel, author, "Resetting images", "Resetting the scraped images from the Discord server. This is for testing only.");
            dataScraper.deleteServer(guild);
        } else {
            syntaxError(author, channel);
        }
    }

    private void syntaxError(Member author, TextChannel channel) {
        EmbedUtils.error(channel, author, String.format("Usage: %ssetup [reset]", commandManager.getPrefix()));
    }
}
