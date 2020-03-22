package com.uddernetworks.contentcop.discord.command;

import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.discord.EmbedUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.Comparator;
import java.util.stream.Collectors;

import static com.uddernetworks.contentcop.utility.Utility.padRight;
import static com.uddernetworks.contentcop.utility.Utility.space;

public class LeaderboardCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderboardCommand.class);
    private static final Color COLOR = new Color(155, 51, 152);

    private final DatabaseManager databaseManager;
    private final JDA jda;

    public LeaderboardCommand(CommandManager commandManager) {
        super("leaderboard", commandManager);
        var contentCop = commandManager.getContentCop();
        this.databaseManager = contentCop.getDatabaseManager();
        this.jda = contentCop.getDiscordManager().getJDA();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        var guild = channel.getGuild();

        databaseManager.getServer(guild).thenAcceptAsync(optional ->
                optional.ifPresentOrElse(complete -> {
                    if (complete) {
                        databaseManager.getUsers(guild).thenAccept(users ->
                                EmbedUtils.sendEmbed(channel, author, "Repost Leaderboard For " + guild.getName(), embed -> embed.setDescription(users.entrySet().stream()
                                        .sorted(Comparator.comparingInt(entry -> -1 * entry.getValue()))
                                        .limit(20)
                                        .map(entry ->  padRight(bold(entry.getValue()), 10) + getName(entry.getKey()))
                                        .collect(Collectors.joining("\n"))).setColor(COLOR)));
                    } else {
                        EmbedUtils.error(channel, author, "The server is currently in the process of being scraped. Just take a chill pill, bro.");
                    }
                }, () -> EmbedUtils.error(channel, author, "The server has not been set up yet. Run **/setup** to start.")));
    }

    private String getName(long id) {
        var user = jda.getUserById(id);
        if (user == null) {
            user = jda.retrieveUserById(id).complete();
        }

        return user != null ? user.getName() : "";
    }
}
