package com.sagecraft.chatbubble.listeners;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {
    
    private final ChatBubblePlugin plugin;
    
    public ChatListener(ChatBubblePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // 检查权限
        if (plugin.getConfigManager().isPermissionRequired()) {
            if (!event.getPlayer().hasPermission(plugin.getConfigManager().getPermissionNode())) {
                return;
            }
        }
        
        String message = event.getMessage();
        
        // 在主线程中创建气泡
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getBubbleManager().createBubble(event.getPlayer(), message);
        });
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 更新气泡位置
        if (plugin.getBubbleManager().hasBubble(event.getPlayer())) {
            plugin.getBubbleManager().updateBubblePosition(event.getPlayer());
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家退出时移除气泡
        plugin.getBubbleManager().removeBubble(event.getPlayer());
    }
}
