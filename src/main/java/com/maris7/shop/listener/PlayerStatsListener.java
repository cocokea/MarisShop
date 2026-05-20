package com.maris7.shop.listener;

import com.maris7.shop.MarisShop;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerStatsListener implements Listener {
    private final MarisShop plugin;

    public PlayerStatsListener(MarisShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.refreshPlayerStatsAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.forgetPlayerStats(event.getPlayer().getUniqueId());
    }
}
