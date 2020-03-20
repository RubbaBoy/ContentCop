package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.ContentCop;
import com.uddernetworks.contentcop.config.ConfigManager;
import com.uddernetworks.contentcop.discord.command.CommandManager;
import com.uddernetworks.contentcop.discord.command.HelpCommand;
import com.uddernetworks.contentcop.discord.command.SetupCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

import static com.uddernetworks.contentcop.config.Config.TOKEN;

public class DiscordManager extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordManager.class);

    private final ContentCop contentCop;
    private final ConfigManager configManager;
    private final CommandManager commandManager;
    private final JDA jda;

    public DiscordManager(ContentCop contentCop, ConfigManager configManager) throws LoginException {
        this.contentCop = contentCop;
        this.configManager = configManager;

        this.jda = JDABuilder.createDefault(configManager.get(TOKEN))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(this)
                .addEventListeners(new EmbedUtils())
                .build();

        this.commandManager = new CommandManager(contentCop, this, configManager);
    }

    /**
     * Registers commands and listeners. Should be invoked once all managers have been initialized.
     */
    public void init() {
          commandManager
                .registerCommand(HelpCommand::new)
                .registerCommand(SetupCommand::new);

          jda.addEventListener(new MessageListener(jda, contentCop.getDatabaseManager(), contentCop.getDataScraper(), contentCop.getBatchImageInserter(), contentCop.getServerCache()));
    }

    public JDA getJDA() {
        return jda;
    }
}
