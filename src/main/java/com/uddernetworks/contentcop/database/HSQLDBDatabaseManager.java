package com.uddernetworks.contentcop.database;

import com.uddernetworks.contentcop.ContentCop;
import com.uddernetworks.contentcop.database.bind.BindType;
import com.uddernetworks.contentcop.database.bind.Binder;
import com.uddernetworks.contentcop.database.bind.ResourceBinder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.uddernetworks.contentcop.Utility.getFirst;

public class HSQLDBDatabaseManager implements DatabaseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HSQLDBDatabaseManager.class);

    private final ContentCop contentCop;
    private final DataSource dataSource;
    private final Binder binder;

    public HSQLDBDatabaseManager(ContentCop contentCop, String databasePath) throws SQLException {
        this.contentCop = contentCop;

        long start = System.currentTimeMillis();
        var config = new HikariConfig();

        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Couldn't find HSQLDB driver", e);
        }

        var filePath = new File(databasePath + File.separator + "ContentCop");
        filePath.getParentFile().mkdirs();
        config.setJdbcUrl("jdbc:hsqldb:file:" + filePath.getAbsolutePath());
        config.setUsername("SA");
        config.setPassword("");

        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "1000");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "8192");

        dataSource = new HikariDataSource(config);

        try (var connection = dataSource.getConnection();
             var useMySQL = connection.prepareStatement("SET DATABASE SQL SYNTAX MYS TRUE")) {
            useMySQL.executeUpdate();
        }

        binder = new ResourceBinder("queries.sql");

        for (var table : binder.getAll(BindType.TABLE)) {
            LOGGER.info("Creating table from \"{}\"", table.getName());
            try (var connection = getConnection();
                 var statement = connection.prepareStatement(table.getSql())) {
                statement.executeUpdate();
            }
        }

        LOGGER.info("Initialized HSQLDBDatabaseManager in {}ms", System.currentTimeMillis() - start);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public CompletableFuture<Optional<DatabaseImage>> getImage(Guild guild, byte[] content) {
        return query("select_image", statement -> {
            statement.setLong(1, guild.getIdLong());
            statement.setBytes(2, content);

            var first = getFirst(iterResultSet(statement.executeQuery()));

            return first
                    .map(res -> new DatabaseImage(guild.getIdLong(), res.get("channel"), res.get("message"), res.get("author"), content));
        }, Optional.empty());
    }

    @Override
    public CompletableFuture<Void> addImage(Message message, byte[] content) {
        return update("add_image", statement -> {
            statement.setLong(1, message.getGuild().getIdLong());
            statement.setLong(2, message.getChannel().getIdLong());
            statement.setLong(3, message.getIdLong());
            statement.setLong(4, message.getAuthor().getIdLong());
            statement.setBytes(5, content);
        });
    }

    @Override
    public CompletableFuture<Void> addImages(Map<Message, byte[]> data) {
        LOGGER.info("Adding {} images", data.size());
        return update("add_image", statement -> {
            data.forEach((message, content) -> {
                try {
                    statement.setLong(1, message.getGuild().getIdLong());
                    statement.setLong(2, message.getChannel().getIdLong());
                    statement.setLong(3, message.getIdLong());
                    statement.setLong(4, message.getAuthor().getIdLong());
                    statement.setBytes(5, content);
                    statement.addBatch();
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                }
            });
            statement.executeBatch();
        }, false);
    }

    @Override
    public CompletableFuture<Void> deleteImages(Guild guild) {
        return update("delete_image_server", statement ->
                statement.setLong(1, guild.getIdLong()));
    }

    @Override
    public CompletableFuture<Void> deleteImages(Member member) {
        return update("delete_image_user", statement ->
                statement.setLong(1, member.getIdLong()));
    }

    @Override
    public CompletableFuture<Optional<Boolean>> getServer(Guild guild) {
        return query("server_status", statement -> {
            statement.setLong(1, guild.getIdLong());

            return getFirst(iterResultSet(statement.executeQuery()))
                    .map(res -> (Boolean) res.get("complete"));
        }, Optional.empty());
    }

    @Override
    public CompletableFuture<Void> addServer(Guild guild) {
        LOGGER.info("Adding server!");
        return update("add_server", statement -> statement.setLong(1, guild.getIdLong()));
    }

    @Override
    public CompletableFuture<Void> updateServer(Guild guild, boolean complete) {
        return update("update_server", statement -> {
            statement.setBoolean(1, complete);
            statement.setLong(2, guild.getIdLong());
        });
    }

    @Override
    public CompletableFuture<Void> deleteServer(Guild guild) {
        return update("delete_server", statement ->
                statement.setLong(1, guild.getIdLong()));
    }

    @Override
    public CompletableFuture<List<Long>> getServers(boolean complete) {
        return query("select_servers", statement -> {
            statement.setBoolean(1, complete);
            return iterResultSet(statement.executeQuery())
                    .stream()
                    .map(res -> res.<Long>get("server"))
                    .collect(Collectors.toUnmodifiableList());
        }, Collections.emptyList());
    }

    @Override
    public CompletableFuture<Map<Long, Integer>> getUsers(Guild guild) {
        return query("select_users", statement -> {
            statement.setLong(1, guild.getIdLong());

            return iterResultSet(statement.executeQuery())
                    .stream()
                    .collect(Collectors.toUnmodifiableMap(res -> res.<Long>get("user"), res -> res.<Integer>get("reposts")));
        }, Collections.emptyMap());
    }

    @Override
    public CompletableFuture<Integer> getUser(Member member) {
        return query("select_user", statement -> {
            statement.setString(1, member.getId());
            statement.setString(2, member.getGuild().getId());

            return getFirst(iterResultSet(statement.executeQuery()))
                    .map(res -> res.<Integer>get("reposts")).orElse(0);
        }, 0);
    }

    @Override
    public CompletableFuture<Void> addUser(Member member, int amount) {
        return update("add_user", statement -> {
            statement.setLong(1, member.getIdLong());
            statement.setLong(2, member.getGuild().getIdLong());
            statement.setInt(3, amount);
        });
    }

    @Override
    public CompletableFuture<Void> incrementUser(Member member, int amount) {
        return update("increment_user", statement -> {
            statement.setInt(1, amount);
            statement.setLong(2, member.getIdLong());
            statement.setLong(3, member.getGuild().getIdLong());
        });
    }

    private List<GenericMap> iterResultSet(ResultSet resultSet) throws SQLException {
        var meta = resultSet.getMetaData();
        int columns = meta.getColumnCount();

        var rows = new ArrayList<GenericMap>();
        while (resultSet.next()) {
            var row = new GenericMap();
            for (int i = 1; i <= columns; ++i) {
                row.put(meta.getColumnName(i).toLowerCase(), resultSet.getObject(i));
            }
            rows.add(row);
        }

        return List.copyOf(rows);
    }

    private interface PSCreator {
        PreparedStatement create(Connection connection) throws SQLException;
    }

    private interface DatabaseQuery<T> {
        T run(PreparedStatement statement) throws SQLException;
    }

    private interface DatabaseUpdate {
        void run(PreparedStatement statement) throws SQLException;
    }

    private CompletableFuture<Void> update(String queryName, DatabaseUpdate accessor) {
        return update(connection -> connection.prepareStatement(binder.getUpdate(queryName)), accessor, true);
    }

    private CompletableFuture<Void> update(String queryName, DatabaseUpdate accessor, boolean autoExecute) {
        return update(connection -> connection.prepareStatement(binder.getUpdate(queryName)), accessor, autoExecute);
    }

    private CompletableFuture<Void> update(PSCreator creator, DatabaseUpdate accessor, boolean autoExecute) {
        return useDatabase(creator, statement -> {
            try {
                accessor.run(statement);

                if (autoExecute) {
                    statement.executeUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }, null);
    }

    private <T> CompletableFuture<T> query(String queryName, DatabaseQuery<T> accessor) {
        return query(queryName, accessor, null);
    }

    private <T> CompletableFuture<T> query(String queryName, DatabaseQuery<T> accessor, T def) {
        return useDatabase(connection -> connection.prepareStatement(binder.getQuery(queryName)), accessor, def);
    }

    private <T> CompletableFuture<T> query(PSCreator creator, DatabaseQuery<T> accessor) {
        return useDatabase(creator, accessor, null);
    }

    private <T> CompletableFuture<T> useDatabase(PSCreator creator, DatabaseQuery<T> accessor, T def) {
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = getConnection();
                 var statement = creator.create(connection)) {
                return accessor.run(statement);
            } catch (Exception e) {
                LOGGER.error("An error has occurred", e);
                return def;
            }
        });
    }
}
