package com.maris7.shop.commands;

import com.maris7.shop.MarisShop;
import com.maris7.shop.inventory.impl.CategoryGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        MarisShop.getFoliaLib().getScheduler().runAtEntity(player, task ->
                MarisShop.getGUIManager().openGUI(new CategoryGUI(), player));
        return true;
    }
}
