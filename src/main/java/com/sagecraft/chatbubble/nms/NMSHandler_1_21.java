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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
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

import java.util.UUID;
import com.sagecraft.chatbubble.utils.SelfIncreaseEntityID;

public class NMSHandler_1_21 implements NMSHandler {
    
    private final ChatBubblePlugin plugin;
    private final AtomicInteger entityIdCounter;
    private final ConcurrentHashMap<String, Integer> bubbleEntityIds;
    
    public NMSHandler_1_21() {
        this.plugin = ChatBubblePlugin.getInstance();
        this.entityIdCounter = new AtomicInteger(1000000);
        this.bubbleEntityIds = new ConcurrentHashMap<>();
    }
    
    @Override
    public void showBubble(ChatBubble bubble) {
        Player player = bubble.getPlayer();
        String playerName = player.getName();
        
        try {
            // 直接使用玩家位置，不再依赖ChatBubble的位置计算
            Location playerLocation = player.getLocation();
            // 使用反射获取CraftBukkit类，因为包名可能因版本而异
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            int entityId = SelfIncreaseEntityID.getAndIncrease();
            java.util.UUID uuid = java.util.UUID.randomUUID();
            
            // 仅存实体ID，不缓存位置
            bubbleEntityIds.put(playerName, entityId);
            
            // 关键：初始Y直接放到"最终高度"（头高+蹲伏补偿+配置偏移），避免生成时再跳到目标高度
            double baseOffsetY = 1.3 + (player.isSneaking() ? -0.3 : 0.0) + plugin.getConfigManager().getHeightOffset();
            
            // 通过UNSAFE构造 AddEntity 包（不创建实体实例）
            Object addPacket = createAddEntityPacketUnsafe(
                entityId, uuid,
                playerLocation.getX(), playerLocation.getY() + baseOffsetY, playerLocation.getZ(),
                (byte) 0, (byte) 0, (byte) 0,
                EntityType.TEXT_DISPLAY, 0,
                0, 0, 0
            );
            
            // 构造必要的元数据（不创建实体，直接使用静态访问器）
            List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();
            
            // 文本内容：支持材质包背景
            String bubbleText = bubble.getBubbleText();
            String messageText = " " + bubble.getMessage();
            Component fullComponent;
            
            if (bubbleText != null && !bubbleText.isEmpty()) {
                // 使用材质包背景
                ResourceLocation fontLocation = createResourceLocation("chatbubble", "default");
                Component backgroundComponent = Component.literal(bubbleText)
                    .withStyle(Style.EMPTY.withFont(fontLocation));
                Component messageComponent = Component.literal(messageText);
                fullComponent = Component.empty().append(backgroundComponent).append(messageComponent);
            } else {
                // 回退到普通文本
                fullComponent = Component.literal("§f" + bubble.getMessage());
            }
            
            // Text - ID: 23
            Object dataTextAccessor = createEntityDataAccessor(23, EntityDataSerializers.COMPONENT);
            Object dataTextValue = createDataValue(dataTextAccessor, fullComponent);
            dataValues.add((SynchedEntityData.DataValue<?>) dataTextValue);
            
            // Billboard 约束（始终面向观察者）- ID: 15
            Object billboardAccessor = createEntityDataAccessor(15, EntityDataSerializers.BYTE);
            Object billboardValue = createDataValue(billboardAccessor, (byte) 3);
            dataValues.add((SynchedEntityData.DataValue<?>) billboardValue);
            
            // 背景颜色（透明）- ID: 25
            Object bgColorAccessor = createEntityDataAccessor(25, EntityDataSerializers.INT);
            Object bgColorValue = createDataValue(bgColorAccessor, 0x00000000);
            dataValues.add((SynchedEntityData.DataValue<?>) bgColorValue);
            
            // 行宽 - ID: 24
            Object lineWidthAccessor = createEntityDataAccessor(24, EntityDataSerializers.INT);
            Object lineWidthValue = createDataValue(lineWidthAccessor, 220);
            dataValues.add((SynchedEntityData.DataValue<?>) lineWidthValue);
            
            // Translation（用于微调；垂直为0，避免再次抬升导致跳变）- ID: 11
            double offsetX = 0.01;
            double offsetY = 0;
            double offsetZ = 0.01;
            Object translationAccessor = createEntityDataAccessor(11, EntityDataSerializers.VECTOR3);
            Object translationVector = createVector3f(offsetX, offsetY, offsetZ);
            Object translationValue = createDataValue(translationAccessor, translationVector);
            dataValues.add((SynchedEntityData.DataValue<?>) translationValue);
            
            // 使用 Bundle 一次性发送所有包，减少客户端渲染间隙
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Object craftPlayer = craftPlayerClass.cast(onlinePlayer);
                Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
                ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);
                
                try {
                    // 创建所有包的列表
                    List<Object> packets = new ArrayList<>();
                    
                    // 1. AddEntity 包
                    packets.add(addPacket);
                    
                    // 2. SetEntityData 包
                    if (!dataValues.isEmpty()) {
                        ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues);
                        packets.add(dataPacket);
                    }
                    
                    // 3. SetPassengers 包
                    int[] passengers = new int[]{entityId};
                    Object setPassengersPacket = createSetPassengersPacket(player.getEntityId(), passengers);
                    packets.add(setPassengersPacket);
                    
                    // 使用 Bundle 一次性发送所有包
                    if (onlinePlayer.getProtocolVersion() >= 764) { // 1.20.2+
                        Object bundlePacket = createBundlePacket(packets);
                        serverPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) bundlePacket);
                    } else {
                        // 1.20.2以下版本单独发送
                        for (Object packet : packets) {
                            serverPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) packet);
                        }
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("发送 Bundle 包时出错: " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("显示气泡给玩家 " + playerName + ": " + bubble.getMessage());
            
        } catch (Exception e) {
            plugin.getLogger().severe("显示气泡时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void showBubbleToPlayer(ChatBubble bubble, Player viewer) {
        plugin.getLogger().info("showBubbleToPlayer");
        Player player = bubble.getPlayer();
        String playerName = player.getName();
        
        try {
            Integer entityId = bubbleEntityIds.get(playerName);
            if (entityId == null) {
                return; // 没有气泡实体
            }
            
            // 直接使用玩家当前位置，与passengers系统保持一致
            Location playerLocation = player.getLocation();
            
            // 使用反射获取CraftBukkit类
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
            
            java.util.UUID uuid = java.util.UUID.randomUUID();
            
            // 通过UNSAFE构造 AddEntity 包（不创建实体实例）
            Object addPacket = createAddEntityPacketUnsafe(
                entityId, uuid,
                playerLocation.getX(), playerLocation.getY(), playerLocation.getZ(),
                (byte) 0, (byte) 0, (byte) 0,
                EntityType.TEXT_DISPLAY, 0,
                0, 0, 0
            );
            
            // 构造必要的元数据（不创建实体，直接使用静态访问器）
            List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();
            
            // 文本内容：支持材质包背景
            String bubbleText = bubble.getBubbleText();
            String messageText = " " + bubble.getMessage();
            Component fullComponent;
            
            if (bubbleText != null && !bubbleText.isEmpty()) {
                // 使用材质包背景
                ResourceLocation fontLocation = createResourceLocation("chatbubble", "default");
                Component backgroundComponent = Component.literal(bubbleText)
                    .withStyle(Style.EMPTY.withFont(fontLocation));
                Component messageComponent = Component.literal(messageText);
                fullComponent = Component.empty().append(backgroundComponent).append(messageComponent);
            } else {
                // 回退到普通文本
                fullComponent = Component.literal("§f" + bubble.getMessage());
            }
            
            // Text - ID: 23
            Object dataTextAccessor = createEntityDataAccessor(23, EntityDataSerializers.COMPONENT);
            Object dataTextValue = createDataValue(dataTextAccessor, fullComponent);
            dataValues.add((SynchedEntityData.DataValue<?>) dataTextValue);
            
            // Billboard 约束（始终面向观察者）- ID: 15
            Object billboardAccessor = createEntityDataAccessor(15, EntityDataSerializers.BYTE);
            Object billboardValue = createDataValue(billboardAccessor, (byte) 3);
            dataValues.add((SynchedEntityData.DataValue<?>) billboardValue);
            
            // 背景颜色（透明）- ID: 25
            Object bgColorAccessor = createEntityDataAccessor(25, EntityDataSerializers.INT);
            Object bgColorValue = createDataValue(bgColorAccessor, 0x00000000);
            dataValues.add((SynchedEntityData.DataValue<?>) bgColorValue);
            
            // 行宽 - ID: 24
            Object lineWidthAccessor = createEntityDataAccessor(24, EntityDataSerializers.INT);
            Object lineWidthValue = createDataValue(lineWidthAccessor, 220);
            dataValues.add((SynchedEntityData.DataValue<?>) lineWidthValue);
            
            // Translation（用于微调；垂直为0，避免再次抬升导致跳变）- ID: 11
            double offsetX = 0.01;
            double offsetY = 0;
            double offsetZ = 0.01;
            Object translationAccessor = createEntityDataAccessor(11, EntityDataSerializers.VECTOR3);
            Object translationVector = createVector3f(offsetX, offsetY, offsetZ);
            Object translationValue = createDataValue(translationAccessor, translationVector);
            dataValues.add((SynchedEntityData.DataValue<?>) translationValue);
            
            Object craftPlayer = craftPlayerClass.cast(viewer);
            Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
            ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);
            
            // 准备所有要发送的包
            List<Object> packets = new ArrayList<>();
            packets.add(addPacket);
            
            // 添加数据包
            if (!dataValues.isEmpty()) {
                ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues);
                packets.add(dataPacket);
            }
            
            // 使用bundle包发送（1.20.2+）
            if (viewer.getProtocolVersion() >= 764) { // 1.20.2+
                try {
                    Object bundlePacket = createBundlePacket(packets);
                    serverPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) bundlePacket);
                } catch (Exception e) {
                    plugin.getLogger().warning("发送Bundle包失败，回退到单独发送: " + e.getMessage());
                    // 回退到单独发送
                    for (Object packet : packets) {
                        serverPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) packet);
                    }
                }
            } else {
                // 1.20.2以下版本单独发送
                for (Object packet : packets) {
                    serverPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) packet);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("显示气泡给玩家时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void removeBubble(ChatBubble bubble) {
        Player player = bubble.getPlayer();
        String playerName = player.getName();
        
        try {
            Integer entityId = bubbleEntityIds.remove(playerName);
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
            
            plugin.getLogger().info("移除玩家 " + playerName + " 的气泡");
            
        } catch (Exception e) {
            plugin.getLogger().severe("移除气泡时发生错误: " + e.getMessage());
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

            String craftBukkitPackage = Bukkit.getServer().getClass().getPackageName();
            Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");

            // 只负责：确保乘客关系存在 + 必要时刷新Translation（例如蹲伏变化）
            double offsetX = 0.01;
            double offsetY = (player.isSneaking() ? -0.3 : 0.0);
            double offsetZ = 0.01;

            List<SynchedEntityData.DataValue<?>> runtimeDataValues = new ArrayList<>();
            Object translationAccessor = createEntityDataAccessor(11, EntityDataSerializers.VECTOR3);
            Object translationVector = createVector3f(offsetX, offsetY, offsetZ);
            Object translationValue = createDataValue(translationAccessor, translationVector);
            runtimeDataValues.add((SynchedEntityData.DataValue<?>) translationValue);

            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Object craftPlayer = craftPlayerClass.cast(onlinePlayer);
                Method getHandleMethod2 = craftPlayerClass.getMethod("getHandle");
                ServerPlayer serverPlayer = (ServerPlayer) getHandleMethod2.invoke(craftPlayer);

                try {
                    int[] passengers = new int[]{entityId};
                    Object setPassengersPacket = createSetPassengersPacket(player.getEntityId(), passengers);
                    serverPlayer.connection.send((net.minecraft.network.protocol.Packet<?>) setPassengersPacket);
                } catch (Exception e) {
                    plugin.getLogger().warning("设置乘客时出错: " + e.getMessage());
                }

                if (!runtimeDataValues.isEmpty()) {
                    ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, runtimeDataValues);
                    serverPlayer.connection.send(dataPacket);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("更新气泡位置时发生错误: " + e.getMessage());
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
    
    // 辅助方法：使用UNSAFE直接构造ClientboundAddEntityPacket
    private Object createAddEntityPacketUnsafe(int entityId, java.util.UUID uuid, 
                                               double x, double y, double z,
                                               byte yRot, byte xRot, byte headYRot,
                                               EntityType<?> entityType, int data,
                                               int xa, int ya, int za) throws Exception {
        Class<?> addEntityPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
        
        // 使用UNSAFE创建实例
        java.lang.reflect.Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Object packet = unsafe.getClass().getMethod("allocateInstance", Class.class)
                .invoke(unsafe, addEntityPacketClass);
        
        // 设置各个字段
        java.lang.reflect.Field[] fields = {
            addEntityPacketClass.getDeclaredField("id"),
            addEntityPacketClass.getDeclaredField("uuid"),
            addEntityPacketClass.getDeclaredField("x"),
            addEntityPacketClass.getDeclaredField("y"),
            addEntityPacketClass.getDeclaredField("z"),
            addEntityPacketClass.getDeclaredField("yRot"),
            addEntityPacketClass.getDeclaredField("xRot"),
            addEntityPacketClass.getDeclaredField("type"),
            addEntityPacketClass.getDeclaredField("data"),
            addEntityPacketClass.getDeclaredField("xa"),
            addEntityPacketClass.getDeclaredField("ya"),
            addEntityPacketClass.getDeclaredField("za")
        };
        
        Object[] values = {entityId, uuid, x, y, z, yRot, xRot, entityType, data, xa, ya, za};
        
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            fields[i].set(packet, values[i]);
        }
        
        return packet;
    }
    
    // 辅助方法：获取静态访问器
    private Object getStaticAccessor(String className, String fieldName) throws Exception {
        Class<?> clazz = Class.forName(className);
        java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }
    
    // 辅助方法：创建 Bundle 包
    private Object createBundlePacket(List<Object> packets) throws Exception {
        Class<?> bundlePacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundBundlePacket");
        return bundlePacketClass.getConstructor(Iterable.class).newInstance(packets);
    }

    // 辅助方法：使用反射创建ResourceLocation
    private ResourceLocation createResourceLocation(String namespace, String path) throws Exception {
        try {
            // 尝试使用ResourceLocation.of()方法（如果可用）
            Method ofMethod = ResourceLocation.class.getMethod("of", String.class, String.class);
            return (ResourceLocation) ofMethod.invoke(null, namespace, path);
        } catch (NoSuchMethodException e) {
            // 如果of方法不存在，使用反射创建实例
            java.lang.reflect.Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            ResourceLocation resourceLocation = (ResourceLocation) unsafe.getClass()
                .getMethod("allocateInstance", Class.class)
                .invoke(unsafe, ResourceLocation.class);
            
            // 设置namespace和path字段
            java.lang.reflect.Field namespaceField = ResourceLocation.class.getDeclaredField("namespace");
            java.lang.reflect.Field pathField = ResourceLocation.class.getDeclaredField("path");
            namespaceField.setAccessible(true);
            pathField.setAccessible(true);
            namespaceField.set(resourceLocation, namespace);
            pathField.set(resourceLocation, path);
            
            return resourceLocation;
        }
    }
}
