package com.uddernetworks.contentcop.discord;

import com.uddernetworks.contentcop.database.DatabaseManager;
import net.dv8tion.jda.internal.entities.GuildImpl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCache {

    private final DatabaseManager databaseManager;
    private final Map<Long, Boolean> checkingServers = new ConcurrentHashMap<>();

    public ServerCache(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void addServer(long server) {
        checkingServers.put(server, true);
    }

    public void removeServer(long server) {
        checkingServers.remove(server);
    }

    public CompletableFuture<Boolean> checkingServer(long server) {
        if (checkingServers.containsKey(server)) {
            return CompletableFuture.completedFuture(checkingServers.get(server));
        }

        return databaseManager.getServer(new GuildImpl(null, server))
                .thenApply(optional -> optional.orElse(false)).thenApply(bool -> {
            checkingServers.put(server, bool);
            return bool;
        });
    }
}
