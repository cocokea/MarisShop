package com.maris7.shop.placeholder;

import com.maris7.shop.MarisShop;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class MarisShopExpansion extends PlaceholderExpansion {
    private final MarisShop plugin;

    public MarisShopExpansion(MarisShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "marishop";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Maris";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("name_")) {
            int position = parsePosition(params.substring("name_".length()));
            return plugin.topName(position);
        }
        if (params.startsWith("value_")) {
            int position = parsePosition(params.substring("value_".length()));
            return plugin.topValue(position);
        }
        if ("postion".equalsIgnoreCase(params)) {
            return player == null ? "0" : String.valueOf(plugin.playerPosition(player));
        }
        if ("total".equalsIgnoreCase(params)) {
            return player == null ? plugin.formatShopValue(0D) : plugin.playerTotal(player);
        }
        return null;
    }

    private int parsePosition(String input) {
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }
}
