package com.sagecraft.chatbubble.managers;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import com.sagecraft.chatbubble.objects.ChatBubble;
import com.sagecraft.chatbubble.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BubbleManager {
    
    private final ChatBubblePlugin plugin;
    private final Map<UUID, ChatBubble> activeBubbles;
    private final Map<UUID, Integer> bubbleTasks;
    private final Map<UUID, Integer> positionUpdateTasks;
    
    public BubbleManager(ChatBubblePlugin plugin) {
        this.plugin = plugin;
        this.activeBubbles = new HashMap<>();
        this.bubbleTasks = new HashMap<>();
        this.positionUpdateTasks = new HashMap<>();
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
        // for (Player nearby : player.getWorld().getPlayers()) {
        //     if (nearby.getLocation().distance(player.getLocation()) <= 50) {
        //         plugin.getNMSHandler().showBubbleToPlayer(bubble, nearby);
        //     }
        // }
        
        // 启动位置更新任务（每2tick更新一次，实现丝滑跟随）
        startPositionUpdateTask(player);
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
        
        // 停止位置更新任务
        stopPositionUpdateTask(player);
        
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
        
        for (Integer taskId : positionUpdateTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        positionUpdateTasks.clear();
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
            // 直接调用NMS更新位置，不再依赖ChatBubble的位置计算
            plugin.getNMSHandler().updateBubblePosition(bubble);
        }
    }
    
    private void startPositionUpdateTask(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 取消现有的位置更新任务
        Integer existingTask = positionUpdateTasks.remove(playerId);
        if (existingTask != null) {
            Bukkit.getScheduler().cancelTask(existingTask);
        }
        
        // 创建新的位置更新任务（根据配置调整更新频率）
        int frequency = plugin.getConfigManager().getPositionUpdateFrequency();
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeBubbles.containsKey(playerId) && player.isOnline()) {
                updateBubblePosition(player);
            } else {
                // 玩家离线或气泡已移除，停止任务
                Integer task = positionUpdateTasks.remove(playerId);
                if (task != null) {
                    Bukkit.getScheduler().cancelTask(task);
                }
            }
        }, 0L, frequency + 1L).getTaskId();
        
        positionUpdateTasks.put(playerId, taskId);
    }
    
    private void stopPositionUpdateTask(Player player) {
        UUID playerId = player.getUniqueId();
        Integer taskId = positionUpdateTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
