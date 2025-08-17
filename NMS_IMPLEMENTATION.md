# NMS 实现说明文档

## 概述

本文档详细说明了 ChatBubble 插件中 NMS（Net Minecraft Server）技术的实现原理、最佳实践和注意事项。

## NMS 技术原理

### 什么是 NMS？
NMS 是 Net Minecraft Server 的缩写，它允许插件直接访问 Minecraft 服务器的内部类和方法，绕过 Bukkit API 的限制，实现更高级的功能。

### 为什么使用 NMS？
1. **性能优势**: 直接操作网络数据包，避免实体注册开销
2. **功能扩展**: 实现 Bukkit API 无法提供的功能
3. **精确控制**: 对客户端显示进行精确控制

## 实现架构

### 1. 接口设计
```java
public interface NMSHandler {
    void showBubble(ChatBubble bubble);
    void showBubbleToPlayer(ChatBubble bubble, Player viewer);
    void removeBubble(ChatBubble bubble);
    void updateBubblePosition(ChatBubble bubble);
    String getVersion();
}
```

### 2. 版本适配器模式
- 每个 Minecraft 版本都有对应的 NMS 实现
- 通过接口统一调用方式
- 便于添加新版本支持

## 核心实现细节

### 1. 虚拟实体创建
```java
// 创建 ArmorStand 作为气泡载体
ArmorStand armorStand = new ArmorStand(serverLevel, x, y, z);

// 设置实体属性
armorStand.setInvisible(true);        // 不可见
armorStand.setNoGravity(true);        // 无重力
armorStand.setInvulnerable(true);     // 无敌
armorStand.setCustomName(message);    // 自定义名称
armorStand.setCustomNameVisible(true); // 显示名称
armorStand.setSmall(true);            // 小型实体
armorStand.setMarker(true);           // 标记实体（无碰撞）
armorStand.setSilent(true);           // 静音
```

### 2. 数据包发送
```java
// 创建添加实体数据包
ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(armorStand);

// 创建实体数据同步包
ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, entityData, true);

// 发送给客户端
player.connection.send(addPacket);
player.connection.send(dataPacket);
```

### 3. 实体ID管理
```java
// 使用原子计数器生成唯一ID
private final AtomicInteger entityIdCounter = new AtomicInteger(1000000);

// 使用并发HashMap管理气泡ID映射
private final ConcurrentHashMap<String, Integer> bubbleEntityIds = new ConcurrentHashMap<>();

// 获取或创建实体ID
private int getOrCreateEntityId(String bubbleId) {
    return bubbleEntityIds.computeIfAbsent(bubbleId, k -> entityIdCounter.incrementAndGet());
}
```

## 性能优化

### 1. 实体ID冲突避免
- 从 1000000 开始分配ID，避免与真实实体冲突
- 使用 UUID 的字符串表示作为键，确保唯一性

### 2. 并发安全
- 使用 `ConcurrentHashMap` 管理实体ID映射
- 使用 `AtomicInteger` 生成唯一ID
- 所有操作都是线程安全的

### 3. 错误处理
```java
try {
    // NMS 操作
    player.connection.send(packet);
} catch (Exception e) {
    plugin.getPluginLogger().warning("发送数据包时出错: " + e.getMessage());
}
```

### 4. 在线状态检查
```java
if (!player.isOnline()) {
    return; // 跳过离线玩家
}
```

## 数据包类型

### 1. ClientboundAddEntityPacket
- **用途**: 通知客户端创建新实体
- **参数**: 实体对象
- **时机**: 显示气泡时

### 2. ClientboundSetEntityDataPacket
- **用途**: 同步实体数据（名称、属性等）
- **参数**: 实体ID、实体数据、是否完整更新
- **时机**: 显示气泡时

### 3. ClientboundRemoveEntitiesPacket
- **用途**: 通知客户端移除实体
- **参数**: 实体ID数组
- **时机**: 移除气泡时

### 4. ClientboundTeleportEntityPacket
- **用途**: 更新实体位置
- **参数**: 实体ID、新位置、旋转角度
- **时机**: 玩家移动时更新气泡位置

## 版本兼容性

### 当前支持的版本
- **Paper 1.21.8**: `NMSHandler_1_21.java`

### 添加新版本支持
1. 创建新的处理器类（如 `NMSHandler_1_22.java`）
2. 实现 `NMSHandler` 接口
3. 在主插件类中添加版本检测逻辑

```java
private void initializeNMSHandler() {
    String version = Bukkit.getServer().getBukkitVersion();
    if (version.contains("1.21")) {
        nmsHandler = new NMSHandler_1_21();
    } else if (version.contains("1.22")) {
        nmsHandler = new NMSHandler_1_22();
    } else {
        // 使用默认处理器
        nmsHandler = new NMSHandler_1_21();
    }
}
```

## 安全考虑

### 1. 异常处理
- 所有 NMS 操作都包含在 try-catch 块中
- 记录详细的错误日志
- 优雅降级，避免插件崩溃

### 2. 资源清理
- 及时清理实体ID映射
- 移除气泡时清理相关资源
- 插件禁用时清理所有气泡

### 3. 权限检查
- 检查玩家在线状态
- 验证世界和位置的有效性
- 限制数据包发送范围

## 调试和监控

### 1. 调试模式
```yaml
debug:
  enabled: true
  log-level: "INFO"
```

### 2. 日志记录
- 实体创建和移除
- 数据包发送状态
- 错误和异常信息

### 3. 性能监控
- 气泡数量统计
- 数据包发送频率
- 内存使用情况

## 最佳实践

### 1. 代码组织
- 将 NMS 代码分离到独立的包中
- 使用接口定义统一的API
- 保持代码的可读性和可维护性

### 2. 错误处理
- 始终包含异常处理
- 提供有意义的错误消息
- 实现优雅降级机制

### 3. 性能优化
- 避免不必要的对象创建
- 使用适当的数据结构
- 实现并发安全

### 4. 版本兼容
- 为每个版本创建独立的处理器
- 使用版本检测自动选择合适的实现
- 保持向后兼容性

## 注意事项

### 1. 版本更新
- NMS 代码在 Minecraft 版本更新时可能失效
- 需要及时更新对应的处理器
- 测试所有支持的版本

### 2. 性能影响
- NMS 操作可能影响服务器性能
- 监控内存和CPU使用情况
- 实现适当的限制和优化

### 3. 兼容性
- 与其他插件的兼容性
- 不同 Paper 版本的兼容性
- 客户端版本的兼容性

## 故障排除

### 常见问题

1. **气泡不显示**
   - 检查实体ID是否冲突
   - 验证数据包是否正确发送
   - 确认玩家在线状态

2. **性能问题**
   - 检查气泡数量是否过多
   - 验证数据包发送频率
   - 监控内存使用情况

3. **版本兼容性**
   - 确认服务器版本支持
   - 检查 NMS 类路径是否正确
   - 验证数据包格式是否匹配

### 调试步骤

1. 启用调试模式
2. 检查服务器日志
3. 验证 NMS 类是否正确加载
4. 测试数据包发送功能
5. 监控性能指标

## 总结

NMS 实现为 ChatBubble 插件提供了强大的功能扩展能力，通过直接操作网络数据包实现了高效的聊天气泡显示。通过合理的架构设计、完善的错误处理和性能优化，确保了插件的稳定性和可维护性。
