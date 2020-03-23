package com.uddernetworks.contentcop.discord.command;

import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.discord.EmbedUtils;
import com.uddernetworks.contentcop.image.DBBackedImageStore;
import com.uddernetworks.contentcop.image.ImageStore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

import static com.uddernetworks.contentcop.utility.Utility.padRight;

public class InfoCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoCommand.class);
    private static final Color COLOR = new Color(155, 51, 152);

    private final DatabaseManager databaseManager;
    private final ImageStore imageStore;
    private final JDA jda;

    public InfoCommand(CommandManager commandManager) {
        super("info", commandManager);
        var contentCop = commandManager.getContentCop();
        this.databaseManager = contentCop.getDatabaseManager();
        this.imageStore = contentCop.getImageStore();
        this.jda = contentCop.getDiscordManager().getJDA();
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String[] args) {
        var guild = channel.getGuild();

        databaseManager.getServer(guild).thenAccept(optional ->
                optional.ifPresentOrElse(complete -> {
                    if (complete) {
                        var users = databaseManager.getUsers(guild).join();
                        EmbedUtils.sendEmbed(channel, author, "Repost Cop Information On " + guild.getName(), embed ->
                                embed.setDescription("Information on stuff for the current guild.\n\n" +
                                        padRight(bold(getImageCount(guild)), 12) + "Images stored\n" +
                                        padRight(bold(users.size()), 12) + "Users who have reposted\n" +
                                        padRight(bold(users.values().stream().mapToInt(i -> i).sum()), 12) + "Total reposts"
                                ).setColor(COLOR)
                        );
                    } else {
                        EmbedUtils.error(channel, author, "The server is currently in the process of being scraped. If it isn't, you're shit out of luck.");
                    }
                }, () -> EmbedUtils.error(channel, author, "The server has not been set up yet. Run **/setup** to start.")));
    }

    private String getImageCount(Guild guild) {
        if (imageStore instanceof DBBackedImageStore) {
            return String.valueOf(((DBBackedImageStore) imageStore).getImages(guild).size());
        }

        return "Unsupported by ImageStore implementation";
    }

    private String getName(long id) {
        var user = jda.getUserById(id);
        if (user == null) {
            user = jda.retrieveUserById(id).complete();
        }

        return user != null ? user.getName() : "";
    }
}
