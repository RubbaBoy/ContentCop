package com.uddernetworks.contentcop;

import com.uddernetworks.contentcop.config.ConfigManager;
import com.uddernetworks.contentcop.config.HOCONConfigManager;
import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.database.HSQLDBDatabaseManager;
import com.uddernetworks.contentcop.discord.DataScraper;
import com.uddernetworks.contentcop.discord.DiscordManager;
import com.uddernetworks.contentcop.discord.HelpUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.sql.SQLException;

import static com.uddernetworks.contentcop.config.Config.DATABASE_PATH;
import static com.uddernetworks.contentcop.config.Config.PREFIX;

public class ContentCop {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentCop.class);

    private final ConfigManager configManager;
    private final DiscordManager discordManager;
    private final DatabaseManager databaseManager;
    private final DataScraper dataScraper;

    public static void main(String[] args) {
        if (args.length < 1) {
            LOGGER.error("You must supply the config file to use");
            return;
        }

        try {
            new ContentCop(args[0]).main();
        } catch (SQLException | LoginException e) {
            LOGGER.error("An error has occurred during initialization", e);
        }
    }

    public ContentCop(String config) throws SQLException, LoginException {
        this.configManager = new HOCONConfigManager(new File(config));
        this.databaseManager = new HSQLDBDatabaseManager(this, configManager.get(DATABASE_PATH));
        this.discordManager = new DiscordManager(this, configManager);
        this.dataScraper = new DataScraper(discordManager, databaseManager);

        HelpUtility.setCommandPrefix(configManager.get(PREFIX));
    }

    private void main() {

    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DataScraper getDataScraper() {
        return dataScraper;
    }
}
