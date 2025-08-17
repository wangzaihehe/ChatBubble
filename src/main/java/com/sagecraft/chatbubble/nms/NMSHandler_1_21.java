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
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

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
            // 直接使用玩家当前位置，与passengers系统保持一致
            Location playerLocation = player.getLocation();
            textDisplay.setPos(playerLocation.getX(), playerLocation.getY(), playerLocation.getZ());
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
            bubbleLocations.put(playerName, playerLocation.clone());
            
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
            // 直接使用玩家当前位置，与passengers系统保持一致
            Location playerLocation = player.getLocation();
            
            // 使用反射获取CraftBukkit类
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftWorldClass = Class.forName(craftBukkitPackage + ".CraftWorld");
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            // 获取ServerLevel
            Object craftWorld = craftWorldClass.cast(playerLocation.getWorld());
            Method getHandleMethod = craftWorldClass.getMethod("getHandle");
            ServerLevel serverLevel = (ServerLevel) getHandleMethod.invoke(craftWorld);
            
            // 创建TextDisplay实体（无体积，专门用于显示文本）
            TextDisplay textDisplay = new TextDisplay(EntityType.TEXT_DISPLAY, serverLevel);
            textDisplay.setPos(playerLocation.getX(), playerLocation.getY(), playerLocation.getZ());
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
            
            // 获取当前存储的位置
            Location currentLocation = bubbleLocations.get(playerName);
            if (currentLocation == null) {
                // 如果没有存储的位置，直接更新
                bubbleLocations.put(playerName, newLocation.clone());
                return;
            }
            
            // 计算位置变化（使用更精确的计算）
            double deltaX = newLocation.getX() - currentLocation.getX();
            double deltaY = newLocation.getY() - currentLocation.getY();
            double deltaZ = newLocation.getZ() - currentLocation.getZ();
            
            // 使用配置的阈值，确保更精确的跟踪
            double threshold = plugin.getConfigManager().getPositionThreshold();
            if (Math.abs(deltaX) < threshold && Math.abs(deltaY) < threshold && Math.abs(deltaZ) < threshold) {
                return; // 位置变化太小，跳过更新
            }
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getPluginLogger().info("更新气泡位置: " + playerName + " 从 (" + 
                    String.format("%.3f,%.3f,%.3f", currentLocation.getX(), currentLocation.getY(), currentLocation.getZ()) + 
                    ") 到 (" + String.format("%.3f,%.3f,%.3f", newLocation.getX(), newLocation.getY(), newLocation.getZ()) + ")");
            }
            
            // 使用Custom-Nameplates的优化机制：通过Translation属性更新位置
            // 关键：实体位置固定，只更新Translation属性
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            // 创建临时的TextDisplay实体用于生成数据包
            String craftWorldPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftWorldClass = Class.forName(craftWorldPackage + ".CraftWorld");
            
            Object craftWorld = craftWorldClass.cast(newLocation.getWorld());
            Method getHandleMethod = craftWorldClass.getMethod("getHandle");
            ServerLevel serverLevel = (ServerLevel) getHandleMethod.invoke(craftWorld);
            
            TextDisplay textDisplay = new TextDisplay(EntityType.TEXT_DISPLAY, serverLevel);
            // 关键：实体位置直接设置在玩家位置，完全依赖passengers系统
            // 这样可以让初始生成和移动更新使用相同的机制
            textDisplay.setPos(newLocation.getX(), newLocation.getY(), newLocation.getZ());
            textDisplay.setText(Component.literal("§f" + bubble.getMessage()));
            textDisplay.setId(entityId);
            
            // 设置必要的属性
            try {
                Method setBackgroundColorMethod = TextDisplay.class.getDeclaredMethod("setBackgroundColor", int.class);
                setBackgroundColorMethod.setAccessible(true);
                setBackgroundColorMethod.invoke(textDisplay, 0x40000000);
                
                Method setLineWidthMethod = TextDisplay.class.getDeclaredMethod("setLineWidth", int.class);
                setLineWidthMethod.setAccessible(true);
                setLineWidthMethod.invoke(textDisplay, 200);
            } catch (Exception e) {
                plugin.getPluginLogger().warning("无法设置TextDisplay属性: " + e.getMessage());
            }
            textDisplay.setShadowRadius(0.0f);
            textDisplay.setShadowStrength(0.0f);
            textDisplay.setViewRange(1.0f);
            textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
            
            // 关键：使用Passengers系统让TextDisplay跟随玩家移动
            // 这是Custom-Nameplates实现流畅跟踪的核心技术
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Object craftPlayer = craftPlayerClass.cast(onlinePlayer);
                Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
                ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);
                
                // 方法1：使用ClientboundSetPassengersPacket让TextDisplay成为玩家的乘客
                try {
                    // 获取玩家当前的乘客列表
                    Set<Integer> currentPassengers = new HashSet<>();
                    for (org.bukkit.entity.Entity passenger : player.getPassengers()) {
                        currentPassengers.add(passenger.getEntityId());
                    }
                    
                    // 添加我们的TextDisplay实体到乘客列表
                    currentPassengers.add(entityId);
                    
                    // 创建SetPassengersPacket
                    int[] passengerArray = currentPassengers.stream().mapToInt(Integer::intValue).toArray();
                    Object setPassengersPacket = createSetPassengersPacket(player.getEntityId(), passengerArray);
                    
                    // 发送数据包
                    serverPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) setPassengersPacket);
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getPluginLogger().info("设置乘客: 玩家 " + player.getName() + " 现在有 " + currentPassengers.size() + " 个乘客");
                    }
                } catch (Exception e) {
                    plugin.getPluginLogger().warning("设置乘客时出错: " + e.getMessage());
                }
                
                // 方法2：使用Translation属性设置正确的高度偏移
                try {
                    // 关键理解：Translation用于设置相对于实体位置的偏移
                    // 实体位置在玩家位置，通过Translation设置到玩家头顶上方
                    double heightOffset = plugin.getConfigManager().getHeightOffset();
                    double offsetX = 0.01;  // 水平微调
                    double offsetY = 1.6 + heightOffset;  // 垂直偏移到玩家头顶上方
                    double offsetZ = 0.01;  // 水平微调
                    
                    // 使用反射创建Vector3f对象
                    Object vector3f = createVector3f(offsetX, offsetY, offsetZ);
                    
                    // 创建Translation数据值
                    List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();
                    
                    // 获取EntityDataAccessor
                    Object entityDataAccessor = createEntityDataAccessor(11, EntityDataSerializers.VECTOR3);
                    
                    // 创建DataValue
                    Object dataValue = createDataValue(entityDataAccessor, vector3f);
                    dataValues.add((SynchedEntityData.DataValue<?>) dataValue);
                    
                    // 发送数据包
                    if (!dataValues.isEmpty()) {
                        ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues);
                        serverPlayer.connection.send(dataPacket);
                    }
                } catch (Exception e) {
                    plugin.getPluginLogger().warning("创建Translation数据值时出错: " + e.getMessage());
                }
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
    
    // 辅助方法：创建Vector3f对象
    private Object createVector3f(double x, double y, double z) throws Exception {
        // 使用反射创建Vector3f
        Class<?> vector3fClass = Class.forName("org.joml.Vector3f");
        return vector3fClass.getConstructor(float.class, float.class, float.class)
                .newInstance((float) x, (float) y, (float) z);
    }
    
    // 辅助方法：创建EntityDataAccessor
    private Object createEntityDataAccessor(int id, Object serializer) throws Exception {
        // 使用反射创建EntityDataAccessor
        Class<?> entityDataAccessorClass = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
        return entityDataAccessorClass.getConstructor(int.class, Class.forName("net.minecraft.network.syncher.EntityDataSerializer"))
                .newInstance(id, serializer);
    }
    
    // 辅助方法：创建DataValue
    private Object createDataValue(Object entityDataAccessor, Object value) throws Exception {
        // 使用反射创建DataValue
        Class<?> dataValueClass = Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue");
        return dataValueClass.getMethod("create", entityDataAccessor.getClass(), Object.class)
                .invoke(null, entityDataAccessor, value);
    }
    
    // 辅助方法：创建SetPassengersPacket
    private Object createSetPassengersPacket(int vehicleId, int[] passengers) throws Exception {
        // 使用反射创建ClientboundSetPassengersPacket（参考Custom-Nameplates的实现）
        Class<?> setPassengersPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPassengersPacket");
        
        // 使用UNSAFE创建实例（参考Custom-Nameplates的实现）
        java.lang.reflect.Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Object packet = unsafe.getClass().getMethod("allocateInstance", Class.class)
                .invoke(unsafe, setPassengersPacketClass);
        
        // 设置vehicle字段
        java.lang.reflect.Field vehicleField = setPassengersPacketClass.getDeclaredField("vehicle");
        vehicleField.setAccessible(true);
        vehicleField.set(packet, vehicleId);
        
        // 设置passengers字段
        java.lang.reflect.Field passengersField = setPassengersPacketClass.getDeclaredField("passengers");
        passengersField.setAccessible(true);
        passengersField.set(packet, passengers);
        
        return packet;
    }
}
