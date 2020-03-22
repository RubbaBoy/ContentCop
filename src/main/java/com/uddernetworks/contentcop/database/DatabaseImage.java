package com.uddernetworks.contentcop.database;

import net.dv8tion.jda.api.entities.Message;

import java.util.BitSet;

public class DatabaseImage {

    private final long server;
    private final long channel;
    private final long message;
    private final long author;
    private final byte[] content;

    private final BitSet bitSet;

    public DatabaseImage(Message message, byte[] content) {
        this(message.getGuild().getIdLong(), message.getChannel().getIdLong(), message.getIdLong(), message.getAuthor().getIdLong(), content);
    }

    public DatabaseImage(DatabaseImage image) {
        this.server = image.server;
        this.channel = image.channel;
        this.message = image.message;
        this.author = image.author;
        this.content = image.content;
        this.bitSet = image.bitSet;
    }

    public DatabaseImage(long server, long channel, long message, long author, byte[] content) {
        this.server = server;
        this.channel = channel;
        this.message = message;
        this.author = author;
        this.content = content;

        this.bitSet = BitSet.valueOf(content);
    }

    public long getServer() {
        return server;
    }

    public long getChannel() {
        return channel;
    }

    public long getMessage() {
        return message;
    }

    public long getAuthor() {
        return author;
    }

    public byte[] getContent() {
        return content;
    }

    public BitSet getBitSet() {
        return bitSet;
    }
}
