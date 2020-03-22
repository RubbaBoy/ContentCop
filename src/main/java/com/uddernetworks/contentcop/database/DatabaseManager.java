package com.uddernetworks.contentcop.database;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DatabaseManager {
    Connection getConnection() throws SQLException;

    CompletableFuture<Optional<DatabaseImage>> getImage(Guild guild, byte[] content);

    CompletableFuture<List<DatabaseImage>> getImages(Guild guild);

    CompletableFuture<Void> addImage(DatabaseImage databaseImage);

    CompletableFuture<Void> addImage(Message message, byte[] content);

    CompletableFuture<Void> addImages(List<DatabaseImage> data);

    CompletableFuture<Void> addImages(Map<Message, byte[]> data);

    CompletableFuture<Void> deleteImages(Guild guild);

    CompletableFuture<Void> deleteImages(Member member);

    CompletableFuture<Optional<Boolean>> getServer(Guild guild);

    CompletableFuture<Void> addServer(Guild guild);

    CompletableFuture<Void> updateServer(Guild guild, boolean complete);

    CompletableFuture<Void> deleteServer(Guild guild);

    CompletableFuture<List<Long>> getServers(boolean complete);

    CompletableFuture<Map<Long, Integer>> getUsers(Guild guild);

    CompletableFuture<Integer> getUser(Member member);

    CompletableFuture<Void> addUser(Member member, int amount);

    CompletableFuture<Void> incrementUser(Member member, int amount);
}
