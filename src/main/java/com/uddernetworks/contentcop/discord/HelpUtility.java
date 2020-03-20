package com.uddernetworks.contentcop.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class HelpUtility {

    public static final char ZWS = '\u200b';
    private static String commandPrefix = "/";

    public static void send(Member member, TextChannel channel) {
        EmbedUtils.sendEmbed(channel, member, "Content Cop Command Help", embed -> {
                    embed.setDescription("Help for the Content Cop commands (**base** is just the base command, and no arguments)");
                    addCommand(embed, "help",
                            commandRow("base", "Shows this help menu"));
                    addCommand(embed, "config",
                            commandRow("base", "Shows this help menu") +
                            commandRow("help", "Bruh 1") +
                            commandRow("list", "Bruh 2") +
                            commandRow("add [bruh]", "Bruh 3") +
                            commandRow("remove [bruh]", "Bruh 4")
                    );
                }
        );
    }

    private static void addCommand(EmbedBuilder embed, String command, String description) {
        embed.addField(commandPrefix + command, description, false);
    }

    private static String commandRow(String name, String description) {
        return ("    **" + name + "**" + " ".repeat(7)).replace(" ", ZWS + " ") + " - " + description + "\n";
    }

    public static String space(int amount) {
        return (ZWS + " ").repeat(amount);
    }

    public static void setCommandPrefix(String commandPrefix) {
        HelpUtility.commandPrefix = commandPrefix;
    }

}
