package com.maris7.shop.inventory.impl;

import com.maris7.shop.MarisShop;
import com.maris7.shop.events.PlayerShopSellEvent;
import com.maris7.shop.inventory.InventoryButton;
import com.maris7.shop.inventory.InventoryGUI;
import com.maris7.shop.inventory.gui.GUIManager;
import com.maris7.shop.managers.ShopManager;
import com.maris7.shop.managers.SoundManager;
import com.maris7.shop.objectholders.ShopItem;
import com.maris7.shop.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.List;

public class ConfirmGUI extends InventoryGUI {

    private final ShopItem shopItem;
    private final String category;
    private int currentAmount;
    private final FileConfiguration config;

    public ConfirmGUI(ShopItem shopItem, String category, String title) {
        super(title);
        this.shopItem = shopItem;
        this.category = category;
        this.currentAmount = shopItem.getInitialAmount();
        this.config = MarisShop.getInstance().getConfig();
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, super.title);
    }

    private Material getConfigMaterial(String path) {
        String s = config.getString("gui." + path + ".material", "STONE");
        try {
            return Material.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return Material.STONE;
        }
    }

    private int getConfigPosition(String path) {
        return config.getInt("gui." + path + ".position", 0);
    }

    private String getConfigName(String path) {
        return config.getString("gui." + path + ".name", path);
    }

    private List<String> getConfigLore(String path) {
        return config.getStringList("gui." + path + ".lore");
    }

    @Override
    public void decorate(Player player) {
        int maxStack = shopItem.getItem().getType().getMaxStackSize();
        boolean amountEnabled = shopItem.isAmountEnabled();

        addButton(13, new InventoryButton()
                .creator(p -> shopItem.getItem(currentAmount))
                .consumer(event -> {}));

        if (amountEnabled) {
            addButton(getConfigPosition("remove.max"), new InventoryButton()
                    .creator(p -> currentAmount > 1
                            ? guiItem(getConfigMaterial("remove.max"),
                            getConfigName("remove.max").replace("{maxAmount}", String.valueOf(maxStack)),
                            getConfigLore("remove.max"))
                            : new ItemStack(Material.AIR))
                    .consumer(event -> changeAmount(player, -maxStack)));

            addButton(getConfigPosition("remove.ten"), new InventoryButton()
                    .creator(p -> currentAmount > 10
                            ? guiItem(getConfigMaterial("remove.ten"), getConfigName("remove.ten"), getConfigLore("remove.ten"))
                            : new ItemStack(Material.AIR))
                    .consumer(event -> changeAmount(player, -10)));

            addButton(getConfigPosition("remove.one"), new InventoryButton()
                    .creator(p -> currentAmount > 1
                            ? guiItem(getConfigMaterial("remove.one"), getConfigName("remove.one"), getConfigLore("remove.one"))
                            : new ItemStack(Material.AIR))
                    .consumer(event -> changeAmount(player, -1)));

            addButton(getConfigPosition("add.one"), new InventoryButton()
                    .creator(p -> currentAmount < maxStack
                            ? guiItem(getConfigMaterial("add.one"), getConfigName("add.one"), getConfigLore("add.one"))
                            : new ItemStack(Material.AIR))
                    .consumer(event -> changeAmount(player, 1)));

            addButton(getConfigPosition("add.ten"), new InventoryButton()
                    .creator(p -> (currentAmount + 10) <= maxStack
                            ? guiItem(getConfigMaterial("add.ten"), getConfigName("add.ten"), getConfigLore("add.ten"))
                            : new ItemStack(Material.AIR))
                    .consumer(event -> changeAmount(player, 10)));

            addButton(getConfigPosition("add.max"), new InventoryButton()
                    .creator(p -> currentAmount < maxStack
                            ? guiItem(getConfigMaterial("add.max"),
                            getConfigName("add.max").replace("{maxAmount}", String.valueOf(maxStack)),
                            getConfigLore("add.max"))
                            : new ItemStack(Material.AIR))
                    .consumer(event -> setAmount(player, maxStack)));
        }

        addButton(amountEnabled ? getConfigPosition("cancel") : 11, new InventoryButton()
                .creator(p -> guiItem(getConfigMaterial("cancel"), getConfigName("cancel"), getConfigLore("cancel")))
                .consumer(event -> {
                    MarisShop.getSoundManager().play(player, SoundManager.SoundType.CLICK);
                    MarisShop.getGUIManager().openGUI(
                            new ShopGUI(category, ShopManager.getInstance().getShopTitle(category)), player);
                }));

        addButton(amountEnabled ? getConfigPosition("confirm") : 15, new InventoryButton()
                .creator(p -> guiItem(getConfigMaterial("confirm"), getConfigName("confirm"), getConfigLore("confirm")))
                .consumer(event -> handlePurchase(player)));

        super.decorate(player);
    }

    private void handlePurchase(Player player) {
        if (!GUIManager.tryBeginPurchase(player.getUniqueId())) {
            return;
        }

        if (!shopItem.isCommandOnly() && !canFitItems(player, shopItem.getMaterial(), currentAmount)) {
            sendMsg(player, ShopManager.MSG_INVENTORY_FULL, 0);
            MarisShop.getSoundManager().play(player, SoundManager.SoundType.PURCHASE_FAILURE);
            GUIManager.clearCooldown(player.getUniqueId());
            return;
        }

        if (shopItem.isShard()) {
            handleShardPurchase(player);
        } else {
            handleMoneyPurchase(player);
        }
    }

    private void handleMoneyPurchase(Player player) {
        double finalPrice = (double) currentAmount * shopItem.getPrice();
        double balance = MarisShop.getEconomy().getBalance(player);

        if (balance < finalPrice) {
            sendMsg(player, ShopManager.MSG_FAILURE_MONEY, finalPrice);
            MarisShop.getSoundManager().play(player, SoundManager.SoundType.PURCHASE_FAILURE);
            GUIManager.clearCooldown(player.getUniqueId());
            return;
        }

        EconomyResponse response = MarisShop.getEconomy().withdrawPlayer(player, finalPrice);
        if (response == null || !response.transactionSuccess()) {
            sendMsg(player, ShopManager.MSG_FAILURE_MONEY, finalPrice);
            MarisShop.getSoundManager().play(player, SoundManager.SoundType.PURCHASE_FAILURE);
            GUIManager.clearCooldown(player.getUniqueId());
            return;
        }

        finishSuccessfulPurchase(player, finalPrice, ShopManager.MSG_SUCCESS_MONEY);
    }

    private void handleShardPurchase(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            player.sendMessage(ChatUtil.c("&cPlaceholderAPI required for shard purchases!"));
            GUIManager.clearCooldown(player.getUniqueId());
            return;
        }

        double shardBalance;
        try {
            String raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, shopItem.getShardPlaceholder());
            String normalized = raw == null ? "" : raw.trim();
            if (normalized.contains("%")) {
                throw new IllegalArgumentException("Unresolved placeholder: " + normalized);
            }
            normalized = normalized.replace(",", "");
            shardBalance = new BigDecimal(normalized).doubleValue();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatUtil.c("&cFailed to read shard balance!"));
            GUIManager.clearCooldown(player.getUniqueId());
            return;
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtil.c("&cFailed to expand shard placeholder!"));
            GUIManager.clearCooldown(player.getUniqueId());
            return;
        }

        double finalPrice = (double) currentAmount * shopItem.getPriceShards();
        if (shardBalance < finalPrice) {
            sendMsg(player, ShopManager.MSG_FAILURE_SHARDS, finalPrice);
            MarisShop.getSoundManager().play(player, SoundManager.SoundType.PURCHASE_FAILURE);
            GUIManager.clearCooldown(player.getUniqueId());
            return;
        }

        finishSuccessfulPurchase(player, finalPrice, ShopManager.MSG_SUCCESS_SHARDS);
    }

    private void finishSuccessfulPurchase(Player player, double finalPrice, String messagePath) {
        Bukkit.getPluginManager().callEvent(new PlayerShopSellEvent(player, shopItem, finalPrice));
        sendMsg(player, messagePath, finalPrice);
        MarisShop.getSoundManager().play(player, SoundManager.SoundType.PURCHASE_SUCCESS);
        shopItem.deliverPurchase(player, currentAmount);
        MarisShop.getInstance().recordPurchaseAsync(player.getUniqueId(), finalPrice);
    }

    private void sendMsg(Player player, String path, double finalPrice) {
        FileConfiguration cfg = MarisShop.getInstance().getConfig();
        if (!cfg.getBoolean("messages.enabled", true)) return;
        if (!cfg.getBoolean(path + ".enabled", true)) return;
        String text = cfg.getString(path + ".text", "");
        if (text.isEmpty()) return;
        text = text
                .replace("{amount}", String.valueOf(currentAmount))
                .replace("{item}", ShopManager.formatMaterialName(shopItem.getMaterial()))
                .replace("{price}", ShopManager.FORMATTER.format(finalPrice));
        String translated = ChatUtil.c(text);
        player.sendMessage(translated);
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(text));
    }

    private boolean canFitItems(Player player, Material material, int amount) {
        if (material == null || material == Material.AIR) return true;
        int space = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                space += material.getMaxStackSize();
            } else if (slot.getType() == material && !slot.hasItemMeta()) {
                space += material.getMaxStackSize() - slot.getAmount();
            }
            if (space >= amount) return true;
        }
        return false;
    }

    private void changeAmount(Player player, int delta) {
        int maxStack = shopItem.getItem().getType().getMaxStackSize();
        currentAmount = Math.max(1, Math.min(currentAmount + delta, maxStack));
        MarisShop.getSoundManager().play(player, SoundManager.SoundType.CLICK);
        decorate(player);
    }

    private void setAmount(Player player, int amount) {
        int maxStack = shopItem.getItem().getType().getMaxStackSize();
        currentAmount = Math.max(1, Math.min(amount, maxStack));
        MarisShop.getSoundManager().play(player, SoundManager.SoundType.CLICK);
        decorate(player);
    }

    private ItemStack guiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.c(name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(ChatUtil.c(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
