package com.sagecraft.chatbubble.objects;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import com.sagecraft.chatbubble.managers.ResourcePackManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ChatBubble {
    
    private final Player player;
    private final String message;
    private final long creationTime;
    private final ChatBubblePlugin plugin;
    
    public ChatBubble(ChatBubblePlugin plugin, Player player, String message) {
        this.plugin = plugin;
        this.player = player;
        this.message = message;
        this.creationTime = System.currentTimeMillis();
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public String getMessage() {
        return message;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * 生成带气泡背景的完整文本
     * @return 包含气泡背景的文本
     */
    public String getBubbleText() {
        // 获取消息的宽度（这里简化处理，实际应该计算文本宽度）
        int messageWidth = message.length() * 6; // 假设每个字符6像素宽
        
        // 根据消息长度选择合适的背景尺寸
        String backgroundSize = getBackgroundSize(messageWidth);
        
        // 构建气泡背景文本
        StringBuilder bubbleText = new StringBuilder();
        
        // 添加左侧边框
        bubbleText.append(getUnicodeForImage("chatl_" + backgroundSize));
        
        // 添加中间部分（根据消息长度重复）
        int middleCount = Math.max(1, (messageWidth + 20) / 30); // 简化的计算
        for (int i = 0; i < middleCount; i++) {
            bubbleText.append(getUnicodeForImage("chatm_" + backgroundSize));
        }
        
        // 添加右侧边框
        bubbleText.append(getUnicodeForImage("chatr_" + backgroundSize));
        
        // 添加尾部箭头
        bubbleText.append(getUnicodeForImage("chatt_" + backgroundSize));
        
        return bubbleText.toString();
    }
    
    /**
     * 根据消息宽度选择合适的背景尺寸
     * @param messageWidth 消息宽度
     * @return 背景尺寸 (13, 23, 33)
     */
    private String getBackgroundSize(int messageWidth) {
        if (messageWidth <= 50) {
            return "13";
        } else if (messageWidth <= 100) {
            return "23";
        } else {
            return "33";
        }
    }
    
    /**
     * 获取图片对应的Unicode字符
     * @param imageKey 图片键名
     * @return Unicode字符
     */
    private String getUnicodeForImage(String imageKey) {
        ResourcePackManager resourcePackManager = plugin.getResourcePackManager();
        String unicode = resourcePackManager.getUnicodeForImage(imageKey);
        if (unicode == null) {
            // 如果找不到对应的Unicode，返回默认字符
            return "\uE000";
        }
        return unicode;
    }
    
    /**
     * 获取完整的显示文本（包含气泡背景和消息）
     * @return 完整的显示文本
     */
    public String getFullDisplayText() {
        String bubbleBackground = getBubbleText();
        return bubbleBackground + " " + message;
    }
}
