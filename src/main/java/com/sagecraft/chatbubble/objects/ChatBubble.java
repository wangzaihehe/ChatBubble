package com.sagecraft.chatbubble.objects;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChatBubble {
    
    private final Player player;
    private final String message;
    private final ChatBubblePlugin plugin;
    private final UUID bubbleId;
    private Location location;
    private long creationTime;
    
    public ChatBubble(Player player, String message, ChatBubblePlugin plugin) {
        this.player = player;
        this.message = message;
        this.plugin = plugin;
        this.bubbleId = UUID.randomUUID();
        this.creationTime = System.currentTimeMillis();
        updateLocation();
    }
    
    public void updateLocation() {
        Location playerLoc = player.getLocation();
        double heightOffset = plugin.getConfigManager().getHeightOffset();
        
        // 使用玩家的眼睛位置作为基准，确保完全对齐
        Location eyeLocation = player.getEyeLocation();
        
        this.location = new Location(
            playerLoc.getWorld(),
            eyeLocation.getX(),  // 使用眼睛位置的X坐标
            eyeLocation.getY() + heightOffset,  // 在眼睛位置上方
            eyeLocation.getZ(),  // 使用眼睛位置的Z坐标
            playerLoc.getYaw(),  // 保持相同的朝向
            playerLoc.getPitch()  // 保持相同的俯仰角
        );
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public UUID getBubbleId() {
        return bubbleId;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public double getScale() {
        return plugin.getConfigManager().getScale();
    }
    
    public boolean isAnimationEnabled() {
        return plugin.getConfigManager().isAnimationEnabled();
    }
    
    public double getAnimationDuration() {
        return plugin.getConfigManager().getAnimationDuration();
    }
    
    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        long displayDuration = plugin.getConfigManager().getDisplayDuration() * 1000L;
        return (currentTime - creationTime) > displayDuration;
    }
}
