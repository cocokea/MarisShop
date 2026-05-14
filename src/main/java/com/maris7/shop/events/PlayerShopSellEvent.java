package com.maris7.shop.events;

import com.maris7.shop.objectholders.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerShopSellEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ShopItem shopItem;
    private final double finalPrice;

    public PlayerShopSellEvent(Player player, ShopItem shopItem, double finalPrice) {
        this.player     = player;
        this.shopItem   = shopItem;
        this.finalPrice = finalPrice;
    }

    public Player   getPlayer()    { return player; }
    public ShopItem getShopItem()  { return shopItem; }
    public double   getFinalPrice(){ return finalPrice; }

    @Override public HandlerList getHandlers()             { return HANDLERS; }
    public static  HandlerList   getHandlerList()          { return HANDLERS; }
}
