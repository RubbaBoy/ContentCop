package com.uddernetworks.contentcop.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;

import java.io.File;
import java.util.Optional;

public class HOCONConfigManager implements ConfigManager {

    private final File file;

    private FileConfig config;

    public HOCONConfigManager(File file) {
        this.file = file;
        this.config = CommentedFileConfig.builder(file).defaultResource("config.conf").build();
        config.load();
    }

    @Override
    public <T> Optional<T> getOptional(Config key) {
        return config.getOptional(key.getPath());
    }

    @Override
    public <T> T get(Config key) {
        return this.<T>getOptional(key).orElseThrow();
    }

    @Override
    public <T> T get(Config key, T def) {
        return config.getOrElse(key.getPath(), def);
    }
}
