package com.maris7.shop.inventory.gui;

import com.maris7.shop.inventory.InventoryGUI;
import com.maris7.shop.inventory.InventoryHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager {

    private final Map<Inventory, InventoryHandler> activeInventories = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> purchaseCooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> openCooldowns = new ConcurrentHashMap<>();

    private static final long PURCHASE_COOLDOWN_MS = 150L; // 3 ticks at 20 TPS
    private static final long OPEN_GUARD_MS = 150L;

    public void openGUI(InventoryGUI gui, Player player) {
        registerHandledInventory(gui.getInventory(), gui);
        openCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.openInventory(gui.getInventory());
    }

    public void registerHandledInventory(Inventory inventory, InventoryHandler handler) {
        activeInventories.put(inventory, handler);
    }

    public void unregisterInventory(Inventory inventory) {
        activeInventories.remove(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        InventoryHandler handler = activeInventories.get(event.getView().getTopInventory());
        if (handler == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getWhoClicked() instanceof Player player) {
            Long openedAt = openCooldowns.get(player.getUniqueId());
            if (openedAt != null && (System.currentTimeMillis() - openedAt) < OPEN_GUARD_MS) {
                return;
            }
        }

        handler.onClick(event);
    }

    public void handleDrag(InventoryDragEvent event) {
        InventoryHandler handler = activeInventories.get(event.getView().getTopInventory());
        if (handler != null) {
            handler.onDrag(event);
        }
    }

    public void handleOpen(InventoryOpenEvent event) {
        InventoryHandler handler = activeInventories.get(event.getInventory());
        if (handler != null) handler.onOpen(event);
    }

    public void handleClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHandler handler = activeInventories.remove(inventory);
        if (event.getPlayer() instanceof Player player) {
            openCooldowns.remove(player.getUniqueId());
        }
        if (handler != null) handler.onClose(event);
    }

    public static boolean tryBeginPurchase(UUID playerUUID) {
        long now = System.currentTimeMillis();
        long[] result = {0L};
        purchaseCooldowns.compute(playerUUID, (uuid, lastTime) -> {
            if (lastTime != null && (now - lastTime) < PURCHASE_COOLDOWN_MS) {
                result[0] = -1L;
                return lastTime;
            }
            result[0] = now;
            return now;
        });
        return result[0] != -1L;
    }

    public static void clearCooldown(UUID playerUUID) {
        purchaseCooldowns.remove(playerUUID);
    }
}
