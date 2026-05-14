package com.maris7.shop.inventory;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public interface InventoryHandler {
    void onClick(InventoryClickEvent event);
    void onOpen(InventoryOpenEvent event);
    void onClose(InventoryCloseEvent event);
    default void onDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
