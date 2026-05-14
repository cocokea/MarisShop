package com.maris7.shop;

import com.maris7.shop.commands.ShopCommand;
import com.maris7.shop.commands.ShopReloadCommand;
import com.maris7.shop.enums.DirectorySelector;
import com.maris7.shop.inventory.gui.GUIListener;
import com.maris7.shop.inventory.gui.GUIManager;
import com.maris7.shop.managers.ShopManager;
import com.maris7.shop.managers.SoundManager;
import com.tcoded.folialib.FoliaLib;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MarisShop extends JavaPlugin {

    private static MarisShop instance;
    private static GUIManager guiManager;
    private static Economy economy;
    private static FoliaLib foliaLib;
    private static SoundManager soundManager;

    @Override
    public void onEnable() {
        instance = this;
        foliaLib = new FoliaLib(this);
        saveDefaultConfig();
        saveResourceIfMissing("sounds.yml");

        guiManager = new GUIManager();
        soundManager = new SoundManager(this);

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

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling MarisShop.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

    }

    @Override
    public void onDisable() {
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

    private void saveResourceIfMissing(String path) {
        if (!new File(getDataFolder(), path).exists()) {
            saveResource(path, false);
        }
    }

}

