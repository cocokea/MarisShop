package com.maris7.shop.inventory.impl;

import com.maris7.shop.MarisShop;
import com.maris7.shop.inventory.InventoryButton;
import com.maris7.shop.inventory.InventoryGUI;
import com.maris7.shop.managers.ShopManager;
import com.maris7.shop.managers.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CategoryGUI extends InventoryGUI {

    public CategoryGUI() {
        super(ShopManager.getInstance().getCategoryTitle());
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, super.title);
    }

    @Override
    public void decorate(Player player) {
        ShopManager.getInstance().getCategoryItems().forEach(categoryItem ->
                addButton(categoryItem.getPosition(), new InventoryButton()
                        .creator(p -> categoryItem.getItem())
                        .consumer(event -> {
                            MarisShop.getSoundManager().play(player, SoundManager.SoundType.CLICK);
                            MarisShop.getGUIManager().openGUI(
                                    new ShopGUI(categoryItem.getCategory(),
                                            ShopManager.getInstance().getShopTitle(categoryItem.getCategory())),
                                    player);
                        })));
        super.decorate(player);
    }
}
