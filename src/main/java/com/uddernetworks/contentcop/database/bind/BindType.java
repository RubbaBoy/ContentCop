package com.uddernetworks.contentcop.database.bind;

import java.util.Arrays;
import java.util.Optional;

public enum BindType {
    QUERY("query"),
    TABLE("table"),
    UPDATE("update");

    private final String name;

    BindType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Optional<BindType> fromName(String name) {
        return Arrays.stream(values()).filter(type -> type.name.equalsIgnoreCase(name)).findFirst();
    }
}
