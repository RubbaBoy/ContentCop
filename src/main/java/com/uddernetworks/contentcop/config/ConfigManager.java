package com.uddernetworks.contentcop.config;

import java.util.Optional;

public interface ConfigManager {
    <T> Optional<T> getOptional(Config key);

    <T> T get(Config key);

    <T> T get(Config key, T def);
}
