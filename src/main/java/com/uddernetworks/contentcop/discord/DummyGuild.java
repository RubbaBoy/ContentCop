package com.uddernetworks.contentcop.discord;

import net.dv8tion.jda.internal.entities.GuildImpl;

public class DummyGuild extends GuildImpl {
    public DummyGuild(long id) {
        super(null, id);
    }
}
