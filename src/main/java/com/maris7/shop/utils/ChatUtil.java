package com.maris7.shop.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatUtil {

    // Match cả &#RRGGBB lẫn #RRGGBB (6 ký tự hex)
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})|#([0-9a-fA-F]{6})");

    public static String c(String text) {
        if (text == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            // group(1) = từ &#RRGGBB, group(2) = từ #RRGGBB
            String hex = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(ChatColor.of("#" + hex).toString()));
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }

    public static List<String> c(List<String> list) {
        return list.stream().map(ChatUtil::c).collect(Collectors.toList());
    }
}
