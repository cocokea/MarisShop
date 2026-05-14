package com.maris7.shop.objectholders;

import com.maris7.shop.utils.ChatUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ConfigItem {

    private final Material material;
    private final List<String> lore;
    private final String displayName;
    private final int position;

    public ConfigItem(Material material, List<String> lore, String displayName, int position) {
        this.material    = material;
        this.lore        = lore;
        this.displayName = displayName;
        this.position    = position;
    }

    public ItemStack getItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.c(displayName));
            meta.setLore(ChatUtil.c(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public int getPosition() { return position; }
}
