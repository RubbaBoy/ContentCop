package com.uddernetworks.contentcop.database.bind;

import com.uddernetworks.contentcop.utility.Utility;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResourceBinder implements Binder {

    private static final Pattern BINDER_PATTERN = Pattern.compile("--\\s*\\[(.*?),\\s*(.*?)\\]\\s*(.*?)(?:\\n|\\r\\n)((?:.|\\n|\\n\\r)*?(?=(?:\\n|\\r\\n)(?:--|$)))");

    private final String resourceName;
    private final List<BindData> data;

    public ResourceBinder(String resourceName) {
        this.resourceName = resourceName;

        this.data = BINDER_PATTERN.matcher(Utility.readResource(resourceName)).results()
                .map(result -> new BindData(result.group(1), BindType.fromName(result.group(2)).orElseThrow(), result.group(3), result.group(4).trim()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String getTable(String name) {
        return get(name, BindType.TABLE);
    }

    @Override
    public String getQuery(String name) {
        return get(name, BindType.QUERY);
    }

    @Override
    public String getUpdate(String name) {
        return get(name, BindType.UPDATE);
    }

    @Override
    public String get(String name) {
        return get(name, null);
    }

    @Override
    public String get(String name, BindType type) {
        return data.stream()
                .filter(data -> (type == null || data.getType() == type) && data.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(BindData::getSql)
                .orElseThrow(() -> new RuntimeException("No bindable SQL code found for \"" + name + "\"" + (type != null ?  " with the type \"" + type.getName() + "\"" : "")));
    }

    @Override
    public List<BindData> getAll(BindType type) {
        return data.stream()
                .filter(data -> data.getType() == type)
                .collect(Collectors.toUnmodifiableList());
    }
}
