package com.sagecraft.chatbubble.nms;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import com.sagecraft.chatbubble.objects.ChatBubble;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NMSHandler_1_21 implements NMSHandler {
    
    private final ChatBubblePlugin plugin;
    private final AtomicInteger entityIdCounter;
    private final ConcurrentHashMap<String, Integer> bubbleEntityIds;
    private final ConcurrentHashMap<String, Location> bubbleLocations;
    
    public NMSHandler_1_21() {
        this.plugin = ChatBubblePlugin.getInstance();
        this.entityIdCounter = new AtomicInteger(1000000);
        this.bubbleEntityIds = new ConcurrentHashMap<>();
        this.bubbleLocations = new ConcurrentHashMap<>();
    }
    
    @Override
    public void showBubble(ChatBubble bubble) {
        Player player = bubble.getPlayer();
        String playerName = player.getName();
        
        try {
            // 创建TextDisplay实体
            Location location = bubble.getLocation();
            // 使用反射获取CraftBukkit类，因为包名可能因版本而异
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftWorldClass = Class.forName(craftBukkitPackage + ".CraftWorld");
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            // 获取ServerLevel
            Object craftWorld = craftWorldClass.cast(location.getWorld());
            Method getHandleMethod = craftWorldClass.getMethod("getHandle");
            ServerLevel serverLevel = (ServerLevel) getHandleMethod.invoke(craftWorld);
            
            // 创建TextDisplay实体（无体积，专门用于显示文本）
            TextDisplay textDisplay = new TextDisplay(EntityType.TEXT_DISPLAY, serverLevel);
            textDisplay.setPos(location.getX(), location.getY(), location.getZ());
            textDisplay.setText(Component.literal("§f" + bubble.getMessage()));
            // 使用反射设置私有属性
            try {
                Method setBackgroundColorMethod = TextDisplay.class.getDeclaredMethod("setBackgroundColor", int.class);
                setBackgroundColorMethod.setAccessible(true);
                setBackgroundColorMethod.invoke(textDisplay, 0x40000000); // 半透明黑色背景
                
                Method setLineWidthMethod = TextDisplay.class.getDeclaredMethod("setLineWidth", int.class);
                setLineWidthMethod.setAccessible(true);
                setLineWidthMethod.invoke(textDisplay, 200); // 设置行宽
            } catch (Exception e) {
                plugin.getPluginLogger().warning("无法设置TextDisplay属性: " + e.getMessage());
            }
            textDisplay.setShadowRadius(0.0f); // 无阴影
            textDisplay.setShadowStrength(0.0f); // 无阴影强度
            textDisplay.setViewRange(1.0f); // 视野范围
            textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER); // 始终面向玩家
            
            int entityId = entityIdCounter.incrementAndGet();
            textDisplay.setId(entityId);
            
            // 存储实体ID和位置
            bubbleEntityIds.put(playerName, entityId);
            bubbleLocations.put(playerName, location.clone());
            
            // 发送实体创建包给所有玩家
            ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(textDisplay, 0, textDisplay.blockPosition());
            
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Object craftPlayer = craftPlayerClass.cast(onlinePlayer);
                Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
                ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);
                serverPlayer.connection.send(addPacket);
                
                // 发送实体数据包
                SynchedEntityData data = textDisplay.getEntityData();
                ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, data.getNonDefaultValues());
                serverPlayer.connection.send(dataPacket);
            }
            
            plugin.getPluginLogger().info("显示气泡给玩家 " + playerName + ": " + bubble.getMessage());
            
        } catch (Exception e) {
            plugin.getPluginLogger().severe("显示气泡时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void showBubbleToPlayer(ChatBubble bubble, Player viewer) {
        Player player = bubble.getPlayer();
        String playerName = player.getName();
        
        try {
            Integer entityId = bubbleEntityIds.get(playerName);
            if (entityId == null) {
                return; // 没有气泡实体
            }
            
            // 创建TextDisplay实体
            Location location = bubble.getLocation();
            
            // 使用反射获取CraftBukkit类
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftWorldClass = Class.forName(craftBukkitPackage + ".CraftWorld");
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            // 获取ServerLevel
            Object craftWorld = craftWorldClass.cast(location.getWorld());
            Method getHandleMethod = craftWorldClass.getMethod("getHandle");
            ServerLevel serverLevel = (ServerLevel) getHandleMethod.invoke(craftWorld);
            
            // 创建TextDisplay实体（无体积，专门用于显示文本）
            TextDisplay textDisplay = new TextDisplay(EntityType.TEXT_DISPLAY, serverLevel);
            textDisplay.setPos(location.getX(), location.getY(), location.getZ());
            textDisplay.setText(Component.literal("§f" + bubble.getMessage()));
            // 使用反射设置私有属性
            try {
                Method setBackgroundColorMethod = TextDisplay.class.getDeclaredMethod("setBackgroundColor", int.class);
                setBackgroundColorMethod.setAccessible(true);
                setBackgroundColorMethod.invoke(textDisplay, 0x40000000); // 半透明黑色背景
                
                Method setLineWidthMethod = TextDisplay.class.getDeclaredMethod("setLineWidth", int.class);
                setLineWidthMethod.setAccessible(true);
                setLineWidthMethod.invoke(textDisplay, 200); // 设置行宽
            } catch (Exception e) {
                plugin.getPluginLogger().warning("无法设置TextDisplay属性: " + e.getMessage());
            }
            textDisplay.setShadowRadius(0.0f); // 无阴影
            textDisplay.setShadowStrength(0.0f); // 无阴影强度
            textDisplay.setViewRange(1.0f); // 视野范围
            textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER); // 始终面向玩家
            textDisplay.setId(entityId);
            
            Object craftPlayer = craftPlayerClass.cast(viewer);
            Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
            ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);
            
            // 发送实体创建包
            ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(textDisplay, 0, textDisplay.blockPosition());
            serverPlayer.connection.send(addPacket);
            
            // 发送实体数据包
            SynchedEntityData data = textDisplay.getEntityData();
            ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, data.getNonDefaultValues());
            serverPlayer.connection.send(dataPacket);
            
        } catch (Exception e) {
            plugin.getPluginLogger().severe("显示气泡给玩家时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void removeBubble(ChatBubble bubble) {
        Player player = bubble.getPlayer();
        String playerName = player.getName();
        
        try {
            Integer entityId = bubbleEntityIds.remove(playerName);
            bubbleLocations.remove(playerName);
            if (entityId == null) {
                return; // 没有气泡实体
            }
            
            // 发送实体移除包给所有玩家
            ClientboundRemoveEntitiesPacket removePacket = new ClientboundRemoveEntitiesPacket(entityId);
            
            // 使用反射获取CraftPlayer类
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Object craftPlayer = craftPlayerClass.cast(onlinePlayer);
                Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
                ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod.invoke(craftPlayer);
                serverPlayer.connection.send(removePacket);
            }
            
            plugin.getPluginLogger().info("移除玩家 " + playerName + " 的气泡");
            
        } catch (Exception e) {
            plugin.getPluginLogger().severe("移除气泡时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void updateBubblePosition(ChatBubble bubble) {
        Player player = bubble.getPlayer();
        String playerName = player.getName();
        
        try {
            Integer entityId = bubbleEntityIds.get(playerName);
            if (entityId == null) {
                return; // 没有气泡实体
            }
            
            // 更新位置
            bubble.updateLocation();
            Location newLocation = bubble.getLocation();
            
            // 检查位置是否真正发生变化（避免微小变化导致的抖动）
            Location currentLocation = bubbleLocations.get(playerName);
            if (currentLocation != null) {
                double deltaX = newLocation.getX() - currentLocation.getX();
                double deltaY = newLocation.getY() - currentLocation.getY();
                double deltaZ = newLocation.getZ() - currentLocation.getZ();
                
                if (Math.abs(deltaX) < 0.001 && Math.abs(deltaY) < 0.001 && Math.abs(deltaZ) < 0.001) {
                    return; // 位置变化太小，跳过更新
                }
            }
            
            // 使用ClientboundSetEntityDataPacket更新位置（参考Custom-Nameplates的实现）
            // 创建临时的TextDisplay实体用于生成数据包
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftWorldClass = Class.forName(craftBukkitPackage + ".CraftWorld");
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            Object craftWorld = craftWorldClass.cast(newLocation.getWorld());
            Method getHandleMethod = craftWorldClass.getMethod("getHandle");
            ServerLevel serverLevel = (ServerLevel) getHandleMethod.invoke(craftWorld);
            
            TextDisplay textDisplay = new TextDisplay(EntityType.TEXT_DISPLAY, serverLevel);
            textDisplay.setPos(newLocation.getX(), newLocation.getY(), newLocation.getZ());
            textDisplay.setId(entityId);
            
            // 只发送实体数据包来更新位置
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Object craftPlayer = craftPlayerClass.cast(onlinePlayer);
                Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
                ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);
                
                // 发送实体数据包
                SynchedEntityData data = textDisplay.getEntityData();
                ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, data.getNonDefaultValues());
                serverPlayer.connection.send(dataPacket);
            }
            
            // 更新存储的位置
            bubbleLocations.put(playerName, newLocation.clone());
            
        } catch (Exception e) {
            plugin.getPluginLogger().severe("更新气泡位置时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public String getVersion() {
        return "1.21";
    }
}
