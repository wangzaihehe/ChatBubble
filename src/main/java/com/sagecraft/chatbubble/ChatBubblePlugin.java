package com.sagecraft.chatbubble;

import com.sagecraft.chatbubble.commands.ChatBubbleCommand;
import com.sagecraft.chatbubble.listeners.ChatListener;
import com.sagecraft.chatbubble.managers.BubbleManager;
import com.sagecraft.chatbubble.managers.ConfigManager;
import com.sagecraft.chatbubble.managers.ResourcePackManager;
import com.sagecraft.chatbubble.nms.NMSHandler;
import com.sagecraft.chatbubble.nms.NMSHandler_1_21;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class ChatBubblePlugin extends JavaPlugin {
    
    private static ChatBubblePlugin instance;
    private ConfigManager configManager;
    private BubbleManager bubbleManager;
    private ResourcePackManager resourcePackManager;
    private NMSHandler nmsHandler;
    private Logger logger;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        // 检查服务器版本
        if (!checkServerVersion()) {
            logger.severe("不支持的服务器版本！此插件需要 Paper 1.21+");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 检查ViaVersion（可选）
        if (checkViaVersion()) {
            logger.info("检测到 ViaVersion 插件，将支持多版本客户端。");
        } else {
            logger.info("未检测到 ViaVersion 插件，仅支持 1.21+ 客户端。");
        }
        
        // 初始化管理器
        initializeManagers();
        
        // 生成材质包
        generateResourcePack();
        
        // 注册监听器
        registerListeners();
        
        // 注册命令
        registerCommands();
        
        logger.info("ChatBubble 插件已启用！");
    }
    
    @Override
    public void onDisable() {
        if (bubbleManager != null) {
            bubbleManager.removeAllBubbles();
        }
        logger.info("ChatBubble 插件已禁用！");
    }
    
    private boolean checkServerVersion() {
        String version = Bukkit.getServer().getBukkitVersion();
        return version.contains("1.21") || version.contains("1.22") || version.contains("1.23");
    }
    
    private boolean checkViaVersion() {
        return Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
    }
    
    private void initializeManagers() {
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 初始化NMS处理器
        initializeNMSHandler();
        
        // 初始化气泡管理器
        bubbleManager = new BubbleManager(this);
        
        // 初始化材质包管理器
        resourcePackManager = new ResourcePackManager(this);
    }
    
    private void generateResourcePack() {
        if (configManager.isGenerateResourcePackOnStart()) {
            logger.info("开始生成材质包...");
            try {
                resourcePackManager.generate();
                logger.info("材质包生成完成！");
            } catch (Exception e) {
                logger.severe("材质包生成失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void initializeNMSHandler() {
        String version = Bukkit.getServer().getBukkitVersion();
        if (version.contains("1.21")) {
            nmsHandler = new NMSHandler_1_21();
        } else {
            // 可以在这里添加其他版本的处理器
            logger.warning("未找到对应版本的NMS处理器，使用默认处理器");
            nmsHandler = new NMSHandler_1_21();
        }
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }
    
    private void registerCommands() {
        getCommand("chatbubble").setExecutor(new ChatBubbleCommand(this));
    }
    
    // Getter方法
    public static ChatBubblePlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public BubbleManager getBubbleManager() {
        return bubbleManager;
    }
    
    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
    
    public NMSHandler getNmsHandler() {
        return nmsHandler;
    }
}
