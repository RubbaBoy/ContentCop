package com.uddernetworks.contentcop.database;

public class DatabaseImage {

    private long server;
    private long channel;
    private long message;
    private long author;
    private byte[] content;

    public DatabaseImage(long server, long channel, long message, long author, byte[] content) {
        this.server = server;
        this.channel = channel;
        this.message = message;
        this.author = author;
        this.content = content;
    }

    public long getServer() {
        return server;
    }

    public void setServer(long server) {
        this.server = server;
    }

    public long getChannel() {
        return channel;
    }

    public void setChannel(long channel) {
        this.channel = channel;
    }

    public long getMessage() {
        return message;
    }

    public void setMessage(long message) {
        this.message = message;
    }

    public long getAuthor() {
        return author;
    }

    public void setAuthor(long author) {
        this.author = author;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
