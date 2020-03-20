package com.uddernetworks.contentcop.config;

public enum Config {
    TOKEN("discord.token"),
    PREFIX("discord.prefix"),

    DATABASE_PATH("general.database")
    ;

    private String path;

    Config(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
