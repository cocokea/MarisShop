package com.maris7.shop.objectholders;

import com.maris7.shop.MarisShop;
import com.maris7.shop.managers.ShopManager;
import com.maris7.shop.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopItem {

    private final Material material;
    private final String displayName;   // "" = tự lấy tên material
    private final int price;            // Vault price (0 = không dùng)
    private final int priceShards;      // Shard price (0 = không dùng shards)
    private final String shardPlaceholder;
    private final int initialAmount;
    private final int position;
    private final List<String> commands; // [] = give item directly; có lệnh = chỉ chạy lệnh

    public ShopItem(Material material, String displayName, int price, int priceShards,
                    String shardPlaceholder, int initialAmount, int position, List<String> commands) {
        this.material         = material;
        this.displayName      = displayName;
        this.price            = price;
        this.priceShards      = priceShards;
        this.shardPlaceholder = shardPlaceholder;
        this.initialAmount    = Math.max(1, initialAmount);
        this.position         = position;
        this.commands         = commands;
    }

    // ── Item building ──────────────────────────────────

    /** Build display item for GUI (uses currentAmount for price calculation in lore) */
    public ItemStack getItem(int currentAmount) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Name: use configured name, or auto-generate from material
            String name = (displayName == null || displayName.isEmpty())
                    ? "&f" + ShopManager.formatMaterialName(material)
                    : displayName;
            meta.setDisplayName(ChatUtil.c(name));

            // Lore: auto-generated from config item_lore template
            List<String> lore = ShopManager.getInstance().getAutoLore(
                    price * currentAmount, priceShards * currentAmount);
            meta.setLore(ChatUtil.c(lore));
            item.setItemMeta(meta);
        }
        item.setAmount(Math.max(1, currentAmount));
        return item;
    }

    public ItemStack getItem() {
        return getItem(initialAmount);
    }

    // ── Purchase logic ─────────────────────────────────

    /**
     * true  = dùng shard để mua
     * false = dùng tiền Vault
     * Logic: nếu priceShards > 0 thì là shard purchase
     */
    public boolean isShard() {
        return priceShards > 0;
    }

    /**
     * Tặng item hoặc chạy lệnh sau khi mua thành công.
     *
     * - commands rỗng → give item trực tiếp vào inventory
     * - commands có lệnh → chạy lệnh (3-tick delay), KHÔNG give item
     *
     * Placeholders trong commands: {player}, {amount}, {price}
     */
    public void deliverPurchase(Player player, int amount) {
        if (commands.isEmpty()) {
            // Give item directly — runs on entity thread (called from ConfirmGUI which is already on entity thread)
            ItemStack toGive = new ItemStack(material, amount);
            player.getInventory().addItem(toGive).forEach((slot, leftover) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        } else {
            // Run commands with 3-tick delay (anti-dupe buffer)
            String playerName  = player.getName();
            int totalPrice     = isShard() ? priceShards * amount : price * amount;

            MarisShop.getFoliaLib().getScheduler().runLater(task -> {
                for (String cmd : commands) {
                    String finalCmd = cmd
                            .replace("{player}", playerName)
                            .replace("{amount}", String.valueOf(amount))
                            .replace("{price}", String.valueOf(totalPrice));
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                }
            }, 3L);
        }
    }

    // ── Getters ────────────────────────────────────────

    public Material getMaterial()        { return material; }
    public String   getDisplayName()     { return displayName; }
    public int      getPrice()           { return price; }
    public int      getPriceShards()     { return priceShards; }
    public String   getShardPlaceholder(){ return shardPlaceholder; }
    public int      getInitialAmount()   { return initialAmount; }
    public int      getPosition()        { return position; }
    public List<String> getCommands()    { return commands; }
    public boolean  isCommandOnly()      { return !commands.isEmpty(); }
}
