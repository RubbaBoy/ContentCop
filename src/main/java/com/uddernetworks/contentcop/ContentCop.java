package com.uddernetworks.contentcop;

import com.uddernetworks.contentcop.config.ConfigManager;
import com.uddernetworks.contentcop.config.HOCONConfigManager;
import com.uddernetworks.contentcop.database.DatabaseManager;
import com.uddernetworks.contentcop.database.HSQLDBDatabaseManager;
import com.uddernetworks.contentcop.discord.BatchImageInserter;
import com.uddernetworks.contentcop.discord.DataScraper;
import com.uddernetworks.contentcop.discord.DiscordManager;
import com.uddernetworks.contentcop.discord.HelpUtility;
import com.uddernetworks.contentcop.discord.ServerCache;
import com.uddernetworks.contentcop.image.DBBackedImageStore;
import com.uddernetworks.contentcop.image.DHashImageProcessor;
import com.uddernetworks.contentcop.image.ImageStore;
import com.uddernetworks.contentcop.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;

import static com.uddernetworks.contentcop.config.Config.DATABASE_PATH;
import static com.uddernetworks.contentcop.config.Config.PREFIX;

public class ContentCop {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentCop.class);

    private final ConfigManager configManager;
    private final DiscordManager discordManager;
    private final ServerCache serverCache;
    private final DatabaseManager databaseManager;
    private final ImageStore imageStore;
    private final BatchImageInserter batchImageInserter;
    private final ImageProcessor imageProcessor;
    private final DataScraper dataScraper;

    public static void main(String[] args) {
        var configPath = "config.conf";
        if (args.length < 1) {
            LOGGER.info("Using default config at \"config.conf\"");
        } else {
            configPath = args[0];
        }

        try {
            var config = new File(configPath);
            if (!config.exists()) {
                LOGGER.info("Given config does not exist! Generating it at {}", config.getAbsolutePath());
                Files.write(config.toPath(), Utility.readResource("config.conf").getBytes(), StandardOpenOption.CREATE);
                LOGGER.info("Fill out the config and restart.");
                return;
            }

            new ContentCop(config).main();
        } catch (SQLException | LoginException | IOException e) {
            LOGGER.error("An error has occurred during initialization", e);
        }
    }

    public ContentCop(File config) throws SQLException, LoginException {
        this.configManager = new HOCONConfigManager(config);
        this.databaseManager = new HSQLDBDatabaseManager(this, configManager.get(DATABASE_PATH));
        this.serverCache = new ServerCache(databaseManager);
        this.discordManager = new DiscordManager(this, configManager);
        this.imageStore = new DBBackedImageStore(databaseManager);
        this.batchImageInserter = new BatchImageInserter(imageStore);
        this.imageProcessor = new DHashImageProcessor(imageStore);
        this.dataScraper = new DataScraper(discordManager, databaseManager, batchImageInserter, serverCache, imageProcessor);

        dataScraper.cleanData().join();

        discordManager.init();

        imageStore.init();

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

    public ImageStore getImageStore() {
        return imageStore;
    }

    public ServerCache getServerCache() {
        return serverCache;
    }

    public BatchImageInserter getBatchImageInserter() {
        return batchImageInserter;
    }

    public ImageProcessor getImageProcessor() {
        return imageProcessor;
    }

    public DataScraper getDataScraper() {
        return dataScraper;
    }
}
