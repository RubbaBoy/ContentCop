package com.uddernetworks.contentcop.database.bind;

import java.util.List;

public interface Binder {

    String getTable(String name);

    String getQuery(String name);

    String getUpdate(String name);

    String get(String name);

    String get(String name, BindType type);

    List<BindData> getAll(BindType type);
}
