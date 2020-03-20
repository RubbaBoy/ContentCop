package com.uddernetworks.contentcop.discord.command;

import com.uddernetworks.contentcop.discord.HelpUtility;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class HelpCommand extends Command {

    public HelpCommand(CommandManager commandManager) {
        super("help", commandManager);
    }

    @Override
    public void onCommand(Member author, TextChannel channel, String rawMessage) {
        HelpUtility.send(author, channel);
    }
}
