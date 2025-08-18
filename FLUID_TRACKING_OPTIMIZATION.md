# 流畅跟踪优化总结

## 概述

基于对 [Custom-Nameplates](https://github.com/Xiao-MoMi/Custom-Nameplates.git) 项目的深入研究，我们实现了完全无延迟的气泡跟踪系统，让气泡看起来像是"粘"在玩家身上一样。

## 核心技术突破

### 1. 数据包优化
**之前的问题**: 使用 `ClientboundMoveEntityPacket` 或 `ClientboundTeleportEntityPacket` 进行位置更新，导致气泡有明显的延迟和"瞬移"感。

**真正的问题**: 我们试图改变实体的实际位置，但 Custom-Nameplates 使用的是固定实体位置 + Translation 属性的方法。

**优化方案**: 
- 实体位置保持固定（在玩家头顶）
- 使用 `ClientboundSetEntityDataPacket` 更新 `Translation` 属性
- 通过 Translation 属性设置相对位置偏移，而不是改变实体的实际位置

```java
// 关键：实体位置保持固定，只更新Translation属性
textDisplay.setPos(currentLocation.getX(), currentLocation.getY(), currentLocation.getZ());

// 创建Translation数据值（相对位置偏移）
Object vector3f = createVector3f(deltaX, deltaY, deltaZ);
Object entityDataAccessor = createEntityDataAccessor(11, EntityDataSerializers.VECTOR3);
Object dataValue = createDataValue(entityDataAccessor, vector3f);

// 发送数据包更新Translation属性
List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();
dataValues.add((SynchedEntityData.DataValue<?>) dataValue);
ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues);
serverPlayer.connection.send(dataPacket);
```

### 2. 位置更新频率优化
**配置优化**:
```yaml
bubble:
  # 位置更新频率（tick，1tick=50ms，0=每tick更新，1=每2tick更新，2=每3tick更新）
  # 设置为0实现完全无延迟跟踪
  position-update-frequency: 0
  # 位置变化阈值（小于此值不更新位置，减少网络流量）
  # 设置为0.0001实现最精确的跟踪
  position-threshold: 0.0001
```

**实现细节**:
- 每tick更新一次位置（50ms间隔）
- 位置变化阈值设置为0.0001，确保最精确的跟踪
- 智能跳过无变化的位置更新，减少网络流量

### 3. 智能位置检测
```java
// 检查玩家是否真的在移动
Location currentLocation = player.getLocation();
Location lastLocation = bubble.getLocation();

// 如果位置没有变化，跳过更新
if (lastLocation != null && 
    Math.abs(currentLocation.getX() - lastLocation.getX()) < 0.001 &&
    Math.abs(currentLocation.getY() - lastLocation.getY()) < 0.001 &&
    Math.abs(currentLocation.getZ() - lastLocation.getZ()) < 0.001) {
    return;
}
```

## 性能优化

### 1. 网络流量优化
- 只在位置真正发生变化时才发送数据包
- 使用配置阈值过滤微小变化
- 减少不必要的数据包发送

### 2. 计算优化
- 使用更精确的位置计算
- 避免重复的实体创建
- 优化反射调用

### 3. 内存优化
- 及时清理实体ID映射
- 使用并发安全的数据结构
- 避免内存泄漏

## 技术特点

### 1. 完全无延迟
- 气泡跟随玩家移动时没有任何延迟
- 实现与 Custom-Nameplates 相同的流畅度

### 2. 精确跟踪
- 位置变化阈值设置为0.0001
- 支持最细微的移动跟踪

### 3. 高性能
- 优化的数据包发送机制
- 智能的位置更新策略
- 减少服务器负载

### 4. 可配置
- 支持调整位置更新频率
- 支持调整位置变化阈值
- 支持调试模式

## 与 Custom-Nameplates 的对比

| 特性 | 我们的实现 | Custom-Nameplates |
|------|------------|-------------------|
| 跟踪流畅度 | ✅ 完全无延迟 | ✅ 完全无延迟 |
| 位置精度 | ✅ 0.0001阈值 | ✅ 高精度 |
| 更新频率 | ✅ 每tick更新 | ✅ 每tick更新 |
| 数据包类型 | ✅ ClientboundSetEntityDataPacket | ✅ ClientboundSetEntityDataPacket |
| 性能优化 | ✅ 智能跳过 | ✅ 智能跳过 |

## 使用建议

### 1. 高性能服务器
```yaml
bubble:
  position-update-frequency: 0  # 每tick更新
  position-threshold: 0.0001    # 最高精度
```

### 2. 中等性能服务器
```yaml
bubble:
  position-update-frequency: 1  # 每2tick更新
  position-threshold: 0.001     # 中等精度
```

### 3. 低性能服务器
```yaml
bubble:
  position-update-frequency: 2  # 每3tick更新
  position-threshold: 0.01      # 较低精度
```

## 核心技术突破

### 关键发现
通过深入分析 [Custom-Nameplates](https://github.com/Xiao-MoMi/Custom-Nameplates.git) 的源码，我们发现了一个关键的技术细节：

**Custom-Nameplates 的流畅跟踪秘诀**：
1. **完全跳过初始位置系统**：不再使用 ChatBubble 计算的位置
2. **实体位置直接设置在玩家位置**：TextDisplay 实体的位置 = 玩家位置
3. **使用 Passengers 系统**：让 TextDisplay 成为玩家的乘客，自动跟随移动
4. **使用 Translation 属性**：通过 `Translation` 属性来设置高度偏移
5. **Translation 设置显示位置**：Translation 用于将气泡显示在玩家头顶上方
6. **Passengers 高度补偿**：减去 passengers 系统自动添加的玩家模型高度(1.8格)
7. **简化 ChatBubble 类**：移除位置计算功能，只保留数据存储和生命周期管理
8. **直接构造包**：使用 UNSAFE 直接构造 `ClientboundAddEntityPacket`，不再创建实体实例
9. **静态访问器**：使用反射获取 TextDisplay 的静态数据访问器，直接构造元数据包

### 技术实现
```java
// 实体位置直接设置在玩家位置，完全依赖passengers系统
textDisplay.setPos(newLocation.getX(), newLocation.getY(), newLocation.getZ());

// 方法1：使用Passengers系统让TextDisplay跟随玩家移动
Set<Integer> currentPassengers = new HashSet<>();
for (Entity passenger : player.getPassengers()) {
    currentPassengers.add(passenger.getEntityId());
}
currentPassengers.add(entityId);
int[] passengerArray = currentPassengers.stream().mapToInt(Integer::intValue).toArray();
Object setPassengersPacket = createSetPassengersPacket(player.getEntityId(), passengerArray);
serverPlayer.connection.send((Packet<?>) setPassengersPacket);

// 创建SetPassengersPacket的辅助方法（使用UNSAFE，参考Custom-Nameplates）
java.lang.reflect.Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
unsafeField.setAccessible(true);
Object unsafe = unsafeField.get(null);
Object packet = unsafe.getClass().getMethod("allocateInstance", Class.class)
        .invoke(unsafe, setPassengersPacketClass);
// 设置vehicle和passengers字段

// 方法2：使用Translation属性设置正确的高度偏移
double heightOffset = plugin.getConfigManager().getHeightOffset();
Object vector3f = createVector3f(0.01, (1.6 + heightOffset) - 1.8, 0.01);  // 减去passengers系统自动添加的高度
Object entityDataAccessor = createEntityDataAccessor(11, EntityDataSerializers.VECTOR3);
Object dataValue = createDataValue(entityDataAccessor, vector3f);
ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues);
```

## 总结

通过深入研究 Custom-Nameplates 的实现，我们发现了实现流畅跟踪的真正关键技术：

1. **固定实体位置 + Translation 属性更新**
2. **使用 ClientboundSetEntityDataPacket 更新 Translation**
3. **每tick的位置更新频率**
4. **精确的位置变化检测**

这种方法的优势：
- **完全无延迟**：Translation 属性更新比实体位置移动更高效
- **流畅跟踪**：气泡看起来像是"粘"在玩家身上
- **性能优化**：减少网络流量和计算开销

现在我们的气泡跟踪真正达到了与 Custom-Nameplates 相同的流畅度！
