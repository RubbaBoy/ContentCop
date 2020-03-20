package com.uddernetworks.contentcop.discord.command;

import com.uddernetworks.contentcop.ContentCop;
import com.uddernetworks.contentcop.config.ConfigManager;
import com.uddernetworks.contentcop.discord.DiscordManager;
import net.dv8tion.jda.api.JDA;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.uddernetworks.contentcop.config.Config.PREFIX;

public class CommandManager {

    private final ContentCop contentCop;
    private final CommandHandler commandHandler;
    private final JDA jda;

    private List<Command> commands = new ArrayList<>();
    private String prefix;

    public CommandManager(ContentCop contentCop, DiscordManager discordManager, ConfigManager configManager) {
        this.contentCop = contentCop;
        this.jda = discordManager.getJDA();
        this.prefix = configManager.get(PREFIX);

        jda.addEventListener(this.commandHandler = new CommandHandler(contentCop, this));
    }

    public CommandManager registerCommand(Command command) {
        commands.add(command);
        return this;
    }

    public CommandManager registerCommand(Function<CommandManager, Command> commandGenerator) {
        commands.add(commandGenerator.apply(this));
        return this;
    }

    public <T extends Command> Optional<T> getCommand(Class<T> clazz) {
        return commands.stream().filter(command -> command.getClass().equals(clazz)).findFirst().map(command -> (T) command);
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }

    public ContentCop getContentCop() {
        return contentCop;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
}
