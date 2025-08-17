package com.sagecraft.chatbubble.commands;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import com.sagecraft.chatbubble.utils.TextUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatBubbleCommand implements CommandExecutor {
    
    private final ChatBubblePlugin plugin;
    
    public ChatBubbleCommand(ChatBubblePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatbubble.admin")) {
            sender.sendMessage(TextUtils.colorize("&c[ChatBubble] 您没有权限使用此命令！"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "toggle":
                if (sender instanceof Player) {
                    handleToggle((Player) sender);
                } else {
                    sender.sendMessage(TextUtils.colorize("&c[ChatBubble] 此命令只能由玩家使用！"));
                }
                break;
            case "info":
                sendInfoMessage(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    private void handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage(TextUtils.colorize("&a[ChatBubble] 配置已重新加载！"));
        } catch (Exception e) {
            sender.sendMessage(TextUtils.colorize("&c[ChatBubble] 重新加载配置时发生错误：" + e.getMessage()));
            plugin.getPluginLogger().severe("重新加载配置时发生错误: " + e.getMessage());
        }
    }
    
    private void handleToggle(Player player) {
        // 这里可以实现玩家个人的开关功能
        // 暂时只是显示消息
        player.sendMessage(TextUtils.colorize("&a[ChatBubble] 功能开关功能正在开发中..."));
    }
    
    private void sendInfoMessage(CommandSender sender) {
        sender.sendMessage(TextUtils.colorize("&6=== ChatBubble 插件信息 ==="));
        sender.sendMessage(TextUtils.colorize("&e版本: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(TextUtils.colorize("&e作者: &f" + String.join(", ", plugin.getDescription().getAuthors())));
        sender.sendMessage(TextUtils.colorize("&eNMS版本: &f" + plugin.getNMSHandler().getVersion()));
        sender.sendMessage(TextUtils.colorize("&e活跃气泡数: &f" + plugin.getBubbleManager().getActiveBubblesCount()));
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(TextUtils.colorize("&6=== ChatBubble 命令帮助 ==="));
        sender.sendMessage(TextUtils.colorize("&e/chatbubble reload &7- 重新加载配置"));
        sender.sendMessage(TextUtils.colorize("&e/chatbubble toggle &7- 切换个人开关"));
        sender.sendMessage(TextUtils.colorize("&e/chatbubble info &7- 显示插件信息"));
    }
}
