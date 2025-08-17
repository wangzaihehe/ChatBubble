package com.sagecraft.chatbubble.managers;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    
    private final ChatBubblePlugin plugin;
    private FileConfiguration config;
    
    public ConfigManager(ChatBubblePlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public int getDisplayDuration() {
        return config.getInt("bubble.display-duration", 10);
    }
    
    public double getHeightOffset() {
        return config.getDouble("bubble.height-offset", 2.5);
    }
    
    public double getScale() {
        return config.getDouble("bubble.scale", 1.0);
    }
    
    public boolean isAnimationEnabled() {
        return config.getBoolean("bubble.enable-animation", true);
    }
    
    public double getAnimationDuration() {
        return config.getDouble("bubble.animation-duration", 0.5);
    }
    
    public String getMessageFormat() {
        return config.getString("messages.format", "&7[&f%player%&7] &f%message%");
    }
    
    public boolean isShowPlayerName() {
        return config.getBoolean("messages.show-player-name", true);
    }
    
    public int getMaxMessageLength() {
        return config.getInt("messages.max-length", 50);
    }
    
    public boolean isPermissionRequired() {
        return config.getBoolean("permissions.require-permission", false);
    }
    
    public String getPermissionNode() {
        return config.getString("permissions.permission-node", "chatbubble.use");
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }
    
    public String getLogLevel() {
        return config.getString("debug.log-level", "INFO");
    }
}
