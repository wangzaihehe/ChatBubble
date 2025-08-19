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
        return config.getDouble("bubble.height-offset", 0.5);
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
    
    public int getPositionUpdateFrequency() {
        return config.getInt("bubble.position-update-frequency", 0);
    }
    
    public double getPositionThreshold() {
        return config.getDouble("bubble.position-threshold", 0.0001);
    }
    
    // 材质包相关配置
    public boolean isGenerateResourcePackOnStart() {
        return config.getBoolean("resource-pack.generate-on-start", true);
    }
    
    public int getPackFormat() {
        return config.getInt("resource-pack.pack-format", 22);
    }
    
    public String getPackDescription() {
        return config.getString("resource-pack.description", "ChatBubble Resource Pack");
    }
    
    // Zip相关配置
    public boolean isGenerateZip() {
        return config.getBoolean("resource-pack.generate-zip", true);
    }
    
    public String getZipFilename() {
        return config.getString("resource-pack.zip-filename", "chatbubble-resourcepack.zip");
    }
    
    public boolean isKeepFolder() {
        return config.getBoolean("resource-pack.keep-folder", true);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}
