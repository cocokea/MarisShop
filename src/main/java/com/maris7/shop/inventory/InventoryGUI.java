package com.maris7.shop.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class InventoryGUI implements InventoryHandler {

    private final Inventory inventory;
    private final Map<Integer, InventoryButton> buttonMap = new HashMap<>();
    protected final String title;

    public InventoryGUI(String title) {
        this.title = title;
        this.inventory = createInventory();
    }

    public Inventory getInventory() { return inventory; }

    public void addButton(int slot, InventoryButton button) {
        buttonMap.put(slot, button);
    }

    public void decorate(Player player) {
        inventory.clear();
        buttonMap.forEach((slot, button) -> {
            ItemStack icon = button.getIconCreator().apply(player);
            inventory.setItem(slot, icon);
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) {
            return;
        }

        if (rawSlot >= topSize) {
            return;
        }

        ClickType click = event.getClick();
        InventoryAction action = event.getAction();
        if (click == ClickType.DOUBLE_CLICK
                || click == ClickType.NUMBER_KEY
                || click == ClickType.DROP
                || click == ClickType.CONTROL_DROP
                || click == ClickType.SWAP_OFFHAND
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        }

        InventoryButton button = buttonMap.get(event.getSlot());
        if (button != null && button.getEventConsumer() != null) {
            button.getEventConsumer().accept(event);
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        decorate((Player) event.getPlayer());
    }

    @Override
    public void onClose(InventoryCloseEvent event) {}

    @Override
    public void onDrag(InventoryDragEvent event) {
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    protected abstract Inventory createInventory();
}
