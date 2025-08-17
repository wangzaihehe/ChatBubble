package com.sagecraft.chatbubble.nms;

import com.sagecraft.chatbubble.ChatBubblePlugin;
import com.sagecraft.chatbubble.objects.ChatBubble;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
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
            // 创建ArmorStand实体
            Location location = bubble.getLocation();
            // 使用反射获取CraftBukkit类，因为包名可能因版本而异
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftWorldClass = Class.forName(craftBukkitPackage + ".CraftWorld");
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            // 获取ServerLevel
            Object craftWorld = craftWorldClass.cast(location.getWorld());
            Method getHandleMethod = craftWorldClass.getMethod("getHandle");
            ServerLevel serverLevel = (ServerLevel) getHandleMethod.invoke(craftWorld);
            
            ArmorStand armorStand = new ArmorStand(serverLevel, location.getX(), location.getY(), location.getZ());
            armorStand.setInvisible(true);
            armorStand.setMarker(true);
            armorStand.setNoGravity(true);
            armorStand.setCustomName(Component.literal("§f" + bubble.getMessage()));
            armorStand.setCustomNameVisible(true);
            
            int entityId = entityIdCounter.incrementAndGet();
            armorStand.setId(entityId);
            
            // 存储实体ID和位置
            bubbleEntityIds.put(playerName, entityId);
            bubbleLocations.put(playerName, location.clone());
            
            // 发送实体创建包给所有玩家
            ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(armorStand, 0, armorStand.blockPosition());
            
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Object craftPlayer = craftPlayerClass.cast(onlinePlayer);
                Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
                ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);
                serverPlayer.connection.send(addPacket);
                
                // 发送实体数据包
                SynchedEntityData data = armorStand.getEntityData();
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
            
            // 创建ArmorStand实体
            Location location = bubble.getLocation();
            
            // 使用反射获取CraftBukkit类
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftWorldClass = Class.forName(craftBukkitPackage + ".CraftWorld");
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            // 获取ServerLevel
            Object craftWorld = craftWorldClass.cast(location.getWorld());
            Method getHandleMethod = craftWorldClass.getMethod("getHandle");
            ServerLevel serverLevel = (ServerLevel) getHandleMethod.invoke(craftWorld);
            
            ArmorStand armorStand = new ArmorStand(serverLevel, location.getX(), location.getY(), location.getZ());
            armorStand.setInvisible(true);
            armorStand.setMarker(true);
            armorStand.setNoGravity(true);
            armorStand.setCustomName(Component.literal("§f" + bubble.getMessage()));
            armorStand.setCustomNameVisible(true);
            armorStand.setId(entityId);
            
            Object craftPlayer = craftPlayerClass.cast(viewer);
            Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
            ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);
            
            // 发送实体创建包
            ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(armorStand, 0, armorStand.blockPosition());
            serverPlayer.connection.send(addPacket);
            
            // 发送实体数据包
            SynchedEntityData data = armorStand.getEntityData();
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
            
            // 获取当前位置
            Location currentLocation = bubbleLocations.get(playerName);
            if (currentLocation == null) {
                return; // 没有位置记录
            }
            
            // 更新位置
            bubble.updateLocation();
            Location newLocation = bubble.getLocation();
            
            // 计算相对位置偏移（使用更精确的计算）
            double deltaX = newLocation.getX() - currentLocation.getX();
            double deltaY = newLocation.getY() - currentLocation.getY();
            double deltaZ = newLocation.getZ() - currentLocation.getZ();
            
            // 检查位置是否真正发生变化（避免微小变化导致的抖动）
            if (Math.abs(deltaX) < 0.001 && Math.abs(deltaY) < 0.001 && Math.abs(deltaZ) < 0.001) {
                return; // 位置变化太小，跳过更新
            }
            
            // 转换为网络包格式（32倍精度，128倍缩放）
            short packetDeltaX = (short) (deltaX * 32 * 128);
            short packetDeltaY = (short) (deltaY * 32 * 128);
            short packetDeltaZ = (short) (deltaZ * 32 * 128);
            
            // 发送位置更新包给所有玩家
            ClientboundMoveEntityPacket.PosRot movePacket = new ClientboundMoveEntityPacket.PosRot(entityId, packetDeltaX, packetDeltaY, packetDeltaZ, (byte) newLocation.getYaw(), (byte) newLocation.getPitch(), false);
            
            // 使用反射获取CraftPlayer类
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Object craftPlayer = craftPlayerClass.cast(onlinePlayer);
                Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
                ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod.invoke(craftPlayer);
                serverPlayer.connection.send(movePacket);
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
