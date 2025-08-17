package com.sagecraft.chatbubble.nms;

import com.sagecraft.chatbubble.objects.ChatBubble;
import org.bukkit.entity.Player;

public interface NMSHandler {
    
    /**
     * 显示气泡给所有玩家
     */
    void showBubble(ChatBubble bubble);
    
    /**
     * 显示气泡给特定玩家
     */
    void showBubbleToPlayer(ChatBubble bubble, Player viewer);
    
    /**
     * 移除气泡
     */
    void removeBubble(ChatBubble bubble);
    
    /**
     * 更新气泡位置
     */
    void updateBubblePosition(ChatBubble bubble);
    
    /**
     * 获取服务器版本
     */
    String getVersion();
}
