package com.maris7.shop.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SoundManager {

    private FileConfiguration soundsConfig;
    private final JavaPlugin plugin;

    public SoundManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File soundsFile = new File(plugin.getDataFolder(), "sounds.yml");
        if (!soundsFile.exists()) {
            plugin.saveResource("sounds.yml", false);
        }
        soundsConfig = YamlConfiguration.loadConfiguration(soundsFile);
    }

    public enum SoundType {
        CLICK("click"),
        PURCHASE_SUCCESS("purchase_success"),
        PURCHASE_FAILURE("purchase_failure");

        public final String key;
        SoundType(String key) { this.key = key; }
    }

    public void play(Player player, SoundType type) {
        String path = type.key;
        if (!soundsConfig.getBoolean(path + ".enabled", true)) return;

        String soundName = soundsConfig.getString(path + ".sound", "UI_BUTTON_CLICK");
        float volume = (float) soundsConfig.getDouble(path + ".volume", 1.0);
        float pitch  = (float) soundsConfig.getDouble(path + ".pitch", 1.0);

        Sound sound = resolveSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private Sound resolveSound(String name) {
        if (name == null || name.isEmpty()) return null;

        // Try enum name first (e.g. "UI_BUTTON_CLICK")
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        // Try NamespacedKey (e.g. "minecraft:ui.button.click")
        try {
            String key = name.toLowerCase().replace("_", ".");
            Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(key));
            if (s != null) return s;
        } catch (Exception ignored) {}

        return null;
    }
}
