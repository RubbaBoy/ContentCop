package com.uddernetworks.contentcop.database.bind;

public class BindData {
    private final String name;
    private final BindType type;
    private final String description;
    private final String sql;

    public BindData(String name, BindType type, String description, String sql) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.sql = sql;
    }

    public String getName() {
        return name;
    }

    public BindType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getSql() {
        return sql;
    }
}
