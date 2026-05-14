package com.maris7.shop.managers;

import com.maris7.shop.MarisShop;
import com.maris7.shop.enums.DirectorySelector;
import com.maris7.shop.objectholders.CategoryItem;
import com.maris7.shop.objectholders.ConfigItem;
import com.maris7.shop.objectholders.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    public static final DecimalFormat FORMATTER = new DecimalFormat("#,##0.##");

    private final List<CategoryItem> categoryItems = new ArrayList<>();
    private final Map<String, List<ShopItem>> shopItems = new HashMap<>();

    private ConfigItem backItem;
    private String categoryTitle;
    private String shopTitle;
    private String confirmTitle;

    // Message config paths
    public static final String MSG_SUCCESS_MONEY  = "messages.purchase.success.money";
    public static final String MSG_SUCCESS_SHARDS = "messages.purchase.success.shards";
    public static final String MSG_FAILURE_MONEY  = "messages.purchase.failure.money";
    public static final String MSG_FAILURE_SHARDS = "messages.purchase.failure.shards";
    public static final String MSG_INVENTORY_FULL = "messages.purchase.inventory_full";

    // ── Singleton ──────────────────────────────────────

    private static final class LazyHolder {
        static final ShopManager INSTANCE = new ShopManager();
    }

    public static ShopManager getInstance() { return LazyHolder.INSTANCE; }
    private ShopManager() {}

    // ── Load ───────────────────────────────────────────

    public void load() {
        categoryItems.clear();
        shopItems.clear();

        DirectorySelector.CATEGORIES.getAllYamlFiles().forEach(file -> {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String categoryKey = file.getName().replace(".yml", "");

            // Use "title" as display name if present, fallback to filename
            String displayTitle = cfg.getString("title", categoryKey);

            // Main category icon
            int    pos      = cfg.getInt("main.position", 0);
            String mainName = cfg.getString("main.name", displayTitle);
            List<String> mainLore = cfg.getStringList("main.lore");
            Material mainMat = parseMaterial(cfg.getString("main.material", "CHEST"), file.getName());

            categoryItems.add(new CategoryItem(displayTitle, mainMat, mainLore, mainName, pos));

            // Items
            List<ShopItem> itemList = new ArrayList<>();
            ConfigurationSection itemsSection = cfg.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    String base = "items." + key + ".";

                    Material mat   = parseMaterial(cfg.getString(base + "material", "STONE"), file.getName() + " item:" + key);
                    String name    = cfg.getString(base + "name", "");          // "" = auto from material
                    int price      = cfg.getInt(base + "price", 0);
                    int priceShards = cfg.getInt(base + "price_shards", 0);
                    String shardPh  = cfg.getString(base + "shard_placeholder", "");
                    int initAmount  = cfg.getInt(base + "initial_amount", 1);
                    int itemPos     = cfg.getInt(base + "position", 0);
                    List<String> commands = cfg.getStringList(base + "commands");

                    itemList.add(new ShopItem(mat, name, price, priceShards, shardPh,
                            initAmount, itemPos, commands));
                }
            }
            shopItems.put(displayTitle, itemList);
        });

        backItem = loadConfigItem("back");

        FileConfiguration cfg = MarisShop.getInstance().getConfig();
        categoryTitle = cfg.getString("titles.category", "&8Shop");
        shopTitle     = cfg.getString("titles.shop", "&8{currentCategory}");
        confirmTitle  = cfg.getString("titles.confirm", "&8Confirm");
    }

    private ConfigItem loadConfigItem(String itemName) {
        FileConfiguration cfg = MarisShop.getInstance().getConfig();
        String base    = "gui." + itemName + ".";
        int pos        = cfg.getInt(base + "position", 26);
        String name    = cfg.getString(base + "name", itemName);
        List<String> lore = cfg.getStringList(base + "lore");
        Material mat   = parseMaterial(cfg.getString(base + "material", "STONE"), "config.yml gui." + itemName);
        return new ConfigItem(mat, lore, name, pos);
    }

    private Material parseMaterial(String name, String context) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            MarisShop.getInstance().getLogger().warning("Invalid material '" + name + "' in " + context + ", using STONE");
            return Material.STONE;
        }
    }

    // ── Getters ────────────────────────────────────────

    public List<CategoryItem>          getCategoryItems() { return categoryItems; }
    public Map<String, List<ShopItem>> getShopItems()     { return shopItems; }
    public ConfigItem                  getBackItem()      { return backItem; }
    public String getCategoryTitle()                      { return categoryTitle; }
    public String getShopTitle(String category)           { return shopTitle.replace("{currentCategory}", category); }
    public String getConfirmTitle(Material material)      { return confirmTitle.replace("{currentItem}", formatMaterialName(material)); }

    /**
     * Lore tự động từ config.yml item_lore.
     * - Dòng chứa {price_money}  → chỉ hiện nếu item dùng tiền (priceShards == 0)
     * - Dòng chứa {price_shards} → chỉ hiện nếu item dùng shards (priceShards > 0)
     * - Dòng không chứa placeholder nào → luôn hiện
     */
    public List<String> getAutoLore(int price, int priceShards) {
        List<String> template = MarisShop.getInstance().getConfig().getStringList("item_lore");
        boolean useShards = priceShards > 0;
        List<String> result = new ArrayList<>();
        for (String line : template) {
            boolean hasMoneyPlaceholder  = line.contains("{price_money}");
            boolean hasShardsPlaceholder = line.contains("{price_shards}");

            if (hasMoneyPlaceholder && useShards) continue;   // item dùng shards → ẩn dòng money
            if (hasShardsPlaceholder && !useShards) continue; // item dùng tiền   → ẩn dòng shards

            line = line.replace("{price_money}",  FORMATTER.format(price));
            line = line.replace("{price_shards}", FORMATTER.format(priceShards));
            result.add(line);
        }
        return result;
    }

    public static String formatMaterialName(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                sb.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
                if (i < words.length - 1) sb.append(" ");
            }
        }
        return sb.toString();
    }
}
