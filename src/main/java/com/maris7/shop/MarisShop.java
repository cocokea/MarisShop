package com.maris7.shop;

import com.maris7.shop.commands.ShopCommand;
import com.maris7.shop.commands.ShopReloadCommand;
import com.maris7.shop.enums.DirectorySelector;
import com.maris7.shop.inventory.gui.GUIListener;
import com.maris7.shop.inventory.gui.GUIManager;
import com.maris7.shop.listener.PlayerStatsListener;
import com.maris7.shop.managers.ShopManager;
import com.maris7.shop.managers.SoundManager;
import com.maris7.shop.placeholder.MarisShopExpansion;
import com.maris7.shop.storage.DatabaseManager;
import com.tcoded.folialib.FoliaLib;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarisShop extends JavaPlugin {
    private static MarisShop instance;
    private static GUIManager guiManager;
    private static Economy economy;
    private static FoliaLib foliaLib;
    private static SoundManager soundManager;
    private final Map<Integer, String> topNameCache = new ConcurrentHashMap<>();
    private final Map<Integer, String> topValueCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTotalCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerPositionCache = new ConcurrentHashMap<>();
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        
        saveDefaultConfig();
        MarisPluginStartup.bootstrap(this, "cocokea/MarisShop");
instance = this;
        foliaLib = new FoliaLib(this);
        saveDefaultConfig();
        saveResourceIfMissing("sounds.yml");

        guiManager = new GUIManager();
        soundManager = new SoundManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.start();

        for (DirectorySelector dir : DirectorySelector.values()) {
            dir.initialize();
        }

        ShopManager.getInstance().load();

        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(new ShopCommand());
        }
        if (getCommand("shopreload") != null) {
            getCommand("shopreload").setExecutor(new ShopReloadCommand());
        }
        if (getCommand("marisshop") != null) {
            getCommand("marisshop").setExecutor(new ShopReloadCommand());
        }

        Bukkit.getPluginManager().registerEvents(new GUIListener(guiManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerStatsListener(this), this);

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling MarisShop.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MarisShopExpansion(this).register();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayerStatsAsync(player.getUniqueId());
        }
        refreshTopStatsAsync();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.stop();
        }
        topNameCache.clear();
        topValueCache.clear();
        playerTotalCache.clear();
        playerPositionCache.clear();
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static MarisShop getInstance() { return instance; }
    public static GUIManager getGUIManager() { return guiManager; }
    public static Economy getEconomy() { return economy; }
    public static FoliaLib getFoliaLib() { return foliaLib; }
    public static SoundManager getSoundManager() { return soundManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }

    public void recordPurchaseAsync(UUID playerId, double totalSpent) {
        if (databaseManager == null || playerId == null || totalSpent <= 0D) {
            return;
        }
        databaseManager.recordPurchaseAsync(playerId, totalSpent)
            .thenRun(() -> {
                refreshPlayerStatsAsync(playerId);
                refreshTopStatsAsync();
            })
            .exceptionally(exception -> {
                getLogger().warning("Failed to record shop purchase for " + playerId + ": " + exception.getMessage());
                return null;
            });
    }

    public void refreshPlayerStatsAsync(UUID playerId) {
        if (databaseManager == null || playerId == null) {
            return;
        }
        databaseManager.playerTotalAsync(playerId)
            .thenAccept(total -> playerTotalCache.put(playerId, formatShopValue(total)))
            .exceptionally(exception -> {
                getLogger().warning("Failed to refresh shop total cache for " + playerId + ": " + exception.getMessage());
                return null;
            });
        databaseManager.playerPositionAsync(playerId)
            .thenAccept(position -> playerPositionCache.put(playerId, position))
            .exceptionally(exception -> {
                getLogger().warning("Failed to refresh shop position cache for " + playerId + ": " + exception.getMessage());
                return null;
            });
    }

    public void forgetPlayerStats(UUID playerId) {
        playerTotalCache.remove(playerId);
        playerPositionCache.remove(playerId);
    }

    public String topName(int position) {
        int normalized = Math.max(1, position);
        return topNameCache.getOrDefault(normalized, "");
    }

    public String topValue(int position) {
        int normalized = Math.max(1, position);
        return topValueCache.getOrDefault(normalized, "---");
    }

    public int playerPosition(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            return 0;
        }
        UUID playerId = player.getUniqueId();
        refreshPlayerStatsAsync(playerId);
        return playerPositionCache.getOrDefault(playerId, 0);
    }

    public String playerTotal(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            return "---";
        }
        UUID playerId = player.getUniqueId();
        refreshPlayerStatsAsync(playerId);
        return playerTotalCache.getOrDefault(playerId, "---");
    }

    public String formatShopValue(double value) {
        if (value <= 0D) {
            return "---";
        }
        double absolute = Math.abs(value);
        if (absolute >= 1_000_000_000_000D) {
            return formatCompact(value / 1_000_000_000_000D) + "T";
        }
        if (absolute >= 1_000_000_000D) {
            return formatCompact(value / 1_000_000_000D) + "B";
        }
        if (absolute >= 1_000_000D) {
            return formatCompact(value / 1_000_000D) + "M";
        }
        if (absolute >= 1_000D) {
            return formatCompact(value / 1_000D) + "K";
        }
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return formatCompact(value);
    }

    private void saveResourceIfMissing(String path) {
        if (!new File(getDataFolder(), path).exists()) {
            saveResource(path, false);
        }
    }

    private void refreshTopStatsAsync() {
        if (databaseManager == null) {
            return;
        }
        for (int position = 1; position <= 10; position++) {
            final int currentPosition = position;
            databaseManager.topNameAsync(currentPosition)
                .thenAccept(uuid -> topNameCache.put(currentPosition, resolveTopPlayerName(uuid)))
                .exceptionally(exception -> {
                    getLogger().warning("Failed to refresh shop top name cache at position " + currentPosition + ": " + exception.getMessage());
                    return null;
                });
            databaseManager.topValueAsync(currentPosition)
                .thenAccept(value -> topValueCache.put(currentPosition, formatShopValue(value)))
                .exceptionally(exception -> {
                    getLogger().warning("Failed to refresh shop top value cache at position " + currentPosition + ": " + exception.getMessage());
                    return null;
                });
        }
    }

    private String resolveTopPlayerName(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return "---";
        }
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            return offlinePlayer.getName() == null ? "---" : offlinePlayer.getName();
        } catch (IllegalArgumentException exception) {
            return "---";
        }
    }

    private String formatCompact(double value) {
        String text = String.format(java.util.Locale.US, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

}

