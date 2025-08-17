package com.sagecraft.chatbubble.managers;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import com.sagecraft.chatbubble.objects.ChatBubble;
import com.sagecraft.chatbubble.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BubbleManager {
    
    private final ChatBubblePlugin plugin;
    private final Map<UUID, ChatBubble> activeBubbles;
    private final Map<UUID, Integer> bubbleTasks;
    
    public BubbleManager(ChatBubblePlugin plugin) {
        this.plugin = plugin;
        this.activeBubbles = new HashMap<>();
        this.bubbleTasks = new HashMap<>();
    }
    
    public void createBubble(Player player, String message) {
        // 检查权限
        if (plugin.getConfigManager().isPermissionRequired()) {
            if (!player.hasPermission(plugin.getConfigManager().getPermissionNode())) {
                return;
            }
        }
        
        // 移除现有气泡
        removeBubble(player);
        
        // 格式化消息
        String formattedMessage = formatMessage(player, message);
        
        // 创建新气泡
        ChatBubble bubble = new ChatBubble(player, formattedMessage, plugin);
        
        // 存储气泡
        activeBubbles.put(player.getUniqueId(), bubble);
        
        // 显示气泡
        showBubble(bubble);
        
        // 设置自动移除任务
        scheduleBubbleRemoval(player);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getPluginLogger().info("为玩家 " + player.getName() + " 创建气泡: " + message);
        }
    }
    
    private String formatMessage(Player player, String message) {
        String format = plugin.getConfigManager().getMessageFormat();
        String playerName = plugin.getConfigManager().isShowPlayerName() ? player.getName() : "";
        
        String formatted = format
                .replace("%player%", playerName)
                .replace("%message%", message);
        
        // 限制消息长度
        int maxLength = plugin.getConfigManager().getMaxMessageLength();
        if (formatted.length() > maxLength) {
            formatted = formatted.substring(0, maxLength - 3) + "...";
        }
        
        return TextUtils.colorize(formatted);
    }
    
    private void showBubble(ChatBubble bubble) {
        Player player = bubble.getPlayer();
        
        // 使用NMS显示气泡
        plugin.getNMSHandler().showBubble(bubble);
        
        // 向附近玩家显示气泡
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getLocation().distance(player.getLocation()) <= 50) {
                plugin.getNMSHandler().showBubbleToPlayer(bubble, nearby);
            }
        }
    }
    
    private void scheduleBubbleRemoval(Player player) {
        int duration = plugin.getConfigManager().getDisplayDuration() * 20; // 转换为tick
        
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeBubble(player);
        }, duration).getTaskId();
        
        bubbleTasks.put(player.getUniqueId(), taskId);
    }
    
    public void removeBubble(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 取消移除任务
        Integer taskId = bubbleTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        
        // 移除气泡
        ChatBubble bubble = activeBubbles.remove(playerId);
        if (bubble != null) {
            plugin.getNMSHandler().removeBubble(bubble);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getPluginLogger().info("移除玩家 " + player.getName() + " 的气泡");
            }
        }
    }
    
    public void removeAllBubbles() {
        for (ChatBubble bubble : activeBubbles.values()) {
            plugin.getNMSHandler().removeBubble(bubble);
        }
        activeBubbles.clear();
        
        for (Integer taskId : bubbleTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        bubbleTasks.clear();
    }
    
    public boolean hasBubble(Player player) {
        return activeBubbles.containsKey(player.getUniqueId());
    }
    
    public ChatBubble getBubble(Player player) {
        return activeBubbles.get(player.getUniqueId());
    }
    
    public int getActiveBubblesCount() {
        return activeBubbles.size();
    }
    
    public void updateBubblePosition(Player player) {
        ChatBubble bubble = activeBubbles.get(player.getUniqueId());
        if (bubble != null) {
            plugin.getNMSHandler().updateBubblePosition(bubble);
        }
    }
}
