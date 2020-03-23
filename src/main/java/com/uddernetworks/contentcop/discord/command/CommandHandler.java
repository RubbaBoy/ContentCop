package com.uddernetworks.contentcop.discord.command;

import com.uddernetworks.contentcop.ContentCop;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class CommandHandler extends ListenerAdapter {

    private final ContentCop contentCop;
    private final CommandManager commandManager;

    public CommandHandler(ContentCop contentCop, CommandManager commandManager) {
        this.contentCop = contentCop;
        this.commandManager = commandManager;
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        var author = event.getAuthor();
        var member = event.getGuild().getMember(author);
        var channel = event.getChannel();
        if (author.isBot() || author.isFake()) return;
        var message = event.getMessage();
        var raw = message.getContentRaw();
        var args = raw.split("\\s+");
        var base = args[0];
        var prefix = commandManager.getPrefix();
        if (!base.startsWith(prefix)) return;
        var strippedBase = base.substring(prefix.length());
        commandManager.getCommands().stream().filter(command -> command.commandMatches(strippedBase)).findFirst().ifPresent(command ->  {
            command.onCommand(member, channel, Arrays.copyOfRange(args, 1, args.length));
            command.onCommand(member, channel, raw);
            command.onCommand(member, channel, event);
        });
    }
}
