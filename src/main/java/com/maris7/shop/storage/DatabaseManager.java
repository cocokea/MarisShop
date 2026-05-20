package com.maris7.shop.storage;

import com.maris7.shop.MarisShop;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.file.FileConfiguration;

public final class DatabaseManager {
    private final MarisShop plugin;
    private HikariDataSource dataSource;
    private boolean mysql;
    private ExecutorService executor;

    public DatabaseManager(MarisShop plugin) {
        this.plugin = plugin;
    }

    public void start() {
        FileConfiguration configuration = plugin.getConfig();
        this.mysql = configuration.getBoolean("database.mysql.enabled", false);
        this.executor = Executors.newSingleThreadExecutor(new DbThreadFactory());
        HikariConfig hikari = new HikariConfig();
        if (mysql) {
            String host = configuration.getString("database.mysql.host", "localhost");
            int port = configuration.getInt("database.mysql.port", 3306);
            String database = configuration.getString("database.mysql.database", "marisshop");
            hikari.setJdbcUrl("jdbc:mysql://" + host + ':' + port + '/' + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            hikari.setUsername(configuration.getString("database.mysql.username", "root"));
            hikari.setPassword(configuration.getString("database.mysql.password", ""));
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File file = plugin.getDataFolder().toPath().resolve("data.db").toFile();
            hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
        }
        int poolSize = Math.max(1, configuration.getInt("database.pool-size", 1));
        hikari.setMaximumPoolSize(poolSize);
        hikari.setPoolName("MarisShop-Hikari");
        hikari.setConnectionTimeout(10_000L);
        this.dataSource = new HikariDataSource(hikari);
        bootstrap();
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(10L, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Executor getExecutor() {
        return executor;
    }

    public CompletableFuture<Void> recordPurchaseAsync(UUID playerId, double totalSpent) {
        ExecutorService currentExecutor = executor;
        if (currentExecutor == null || playerId == null || totalSpent <= 0D) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> recordPurchase(playerId, totalSpent), currentExecutor);
    }

    public CompletableFuture<String> topNameAsync(int position) {
        ExecutorService currentExecutor = executor;
        if (currentExecutor == null) {
            return CompletableFuture.completedFuture("");
        }
        return CompletableFuture.supplyAsync(() -> topName(position), currentExecutor);
    }

    public CompletableFuture<Double> topValueAsync(int position) {
        ExecutorService currentExecutor = executor;
        if (currentExecutor == null) {
            return CompletableFuture.completedFuture(0D);
        }
        return CompletableFuture.supplyAsync(() -> topValue(position), currentExecutor);
    }

    public CompletableFuture<Double> playerTotalAsync(UUID playerId) {
        ExecutorService currentExecutor = executor;
        if (currentExecutor == null || playerId == null) {
            return CompletableFuture.completedFuture(0D);
        }
        return CompletableFuture.supplyAsync(() -> playerTotal(playerId), currentExecutor);
    }

    public CompletableFuture<Integer> playerPositionAsync(UUID playerId) {
        ExecutorService currentExecutor = executor;
        if (currentExecutor == null || playerId == null) {
            return CompletableFuture.completedFuture(0);
        }
        return CompletableFuture.supplyAsync(() -> playerPosition(playerId), currentExecutor);
    }

    public void recordPurchase(UUID playerId, double totalSpent) {
        execute("record purchase", () -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(playerStatsUpsertSql())) {
                statement.setString(1, playerId.toString());
                statement.setDouble(2, totalSpent);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public String topName(int position) {
        try {
            return execute("load top name", () -> {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT player_uuid FROM marisshop_player_stats ORDER BY total_spent DESC, player_uuid ASC LIMIT 1 OFFSET ?")) {
                    statement.setInt(1, Math.max(0, position - 1));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? resultSet.getString(1) : "";
                    }
                }
            });
        } catch (IllegalStateException exception) {
            return "";
        }
    }

    public double topValue(int position) {
        try {
            return execute("load top value", () -> {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT total_spent FROM marisshop_player_stats ORDER BY total_spent DESC, player_uuid ASC LIMIT 1 OFFSET ?")) {
                    statement.setInt(1, Math.max(0, position - 1));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? resultSet.getDouble(1) : 0D;
                    }
                }
            });
        } catch (IllegalStateException exception) {
            return 0D;
        }
    }

    public double playerTotal(UUID playerId) {
        try {
            return execute("load player total", () -> {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT total_spent FROM marisshop_player_stats WHERE player_uuid = ?")) {
                    statement.setString(1, playerId.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? resultSet.getDouble(1) : 0D;
                    }
                }
            });
        } catch (IllegalStateException exception) {
            return 0D;
        }
    }

    public int playerPosition(UUID playerId) {
        try {
            return execute("load player position", () -> {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                         "SELECT ranked.position FROM (" +
                             "SELECT player_uuid, ROW_NUMBER() OVER (ORDER BY total_spent DESC, player_uuid ASC) AS position " +
                             "FROM marisshop_player_stats" +
                         ") ranked WHERE ranked.player_uuid = ?")) {
                    statement.setString(1, playerId.toString());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? resultSet.getInt(1) : 0;
                    }
                }
            });
        } catch (IllegalStateException exception) {
            return 0;
        }
    }

    private void bootstrap() {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            if (mysql) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS marisshop_player_stats (player_uuid VARCHAR(36) NOT NULL, total_spent DOUBLE NOT NULL, PRIMARY KEY (player_uuid))");
            } else {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS marisshop_player_stats (player_uuid VARCHAR(36) PRIMARY KEY, total_spent DOUBLE NOT NULL)");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to bootstrap database", exception);
        }
    }

    private String playerStatsUpsertSql() {
        if (mysql) {
            return "INSERT INTO marisshop_player_stats (player_uuid, total_spent) VALUES (?, ?) ON DUPLICATE KEY UPDATE total_spent = total_spent + VALUES(total_spent)";
        }
        return "INSERT INTO marisshop_player_stats (player_uuid, total_spent) VALUES (?, ?) ON CONFLICT(player_uuid) DO UPDATE SET total_spent = total_spent + excluded.total_spent";
    }

    private <T> T execute(String action, SqlSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to " + action, exception);
        }
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    private static final class DbThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "MarisShop-DB");
            thread.setDaemon(true);
            return thread;
        }
    }
}
