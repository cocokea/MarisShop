package com.maris7.shop.inventory.impl;

import com.maris7.shop.MarisShop;
import com.maris7.shop.inventory.InventoryButton;
import com.maris7.shop.inventory.InventoryGUI;
import com.maris7.shop.managers.ShopManager;
import com.maris7.shop.managers.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class ShopGUI extends InventoryGUI {

    private final String category;

    public ShopGUI(String category, String title) {
        super(title);
        this.category = category;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, super.title);
    }

    @Override
    public void decorate(Player player) {
        List<?> items = ShopManager.getInstance().getShopItems().get(category);
        if (items != null) {
            ShopManager.getInstance().getShopItems().get(category).forEach(shopItem ->
                    addButton(shopItem.getPosition(), new InventoryButton()
                            .creator(p -> shopItem.getItem(shopItem.getInitialAmount()))
                            .consumer(event -> {
                                MarisShop.getSoundManager().play(player, SoundManager.SoundType.CLICK);
                                MarisShop.getGUIManager().openGUI(
                                        new ConfirmGUI(shopItem, category,
                                                ShopManager.getInstance().getConfirmTitle(shopItem.getItem().getType())),
                                        player);
                            })));
        }

        // Back button
        addButton(ShopManager.getInstance().getBackItem().getPosition(), new InventoryButton()
                .creator(p -> ShopManager.getInstance().getBackItem().getItem())
                .consumer(event -> {
                    MarisShop.getSoundManager().play(player, SoundManager.SoundType.CLICK);
                    MarisShop.getGUIManager().openGUI(new CategoryGUI(), player);
                }));

        super.decorate(player);
    }
}
