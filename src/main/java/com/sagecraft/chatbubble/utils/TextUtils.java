package com.sagecraft.chatbubble.utils;

import org.bukkit.ChatColor;

public class TextUtils {
    
    /**
     * 将颜色代码转换为ChatColor
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * 移除颜色代码
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(colorize(text));
    }
    
    /**
     * 限制文本长度
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 检查文本是否为空或只包含空格
     */
    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
}
