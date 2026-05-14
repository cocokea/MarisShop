package com.maris7.shop.commands;

import com.maris7.shop.MarisShop;
import com.maris7.shop.enums.DirectorySelector;
import com.maris7.shop.managers.ShopManager;
import com.maris7.shop.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ShopReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("marisshop")) {
            if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(ChatUtil.c("&7Dùng: &f/marisshop reload"));
                return true;
            }
        }

        if (!sender.hasPermission("marisshop.admin")) {
            sender.sendMessage(ChatUtil.c("&cBạn không có quyền dùng lệnh này!"));
            return true;
        }

        try {
            MarisShop.getInstance().reloadConfig();
            MarisShop.getSoundManager().reload();

            for (DirectorySelector dir : DirectorySelector.values()) {
                dir.initialize();
            }

            ShopManager.getInstance().load();
            sender.sendMessage(ChatUtil.c("&a[MarisShop] &fReload thành công!"));
        } catch (Exception e) {
            sender.sendMessage(ChatUtil.c("&c[MarisShop] Reload thất bại: " + e.getMessage()));
            MarisShop.getInstance().getLogger().severe("Reload failed: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
