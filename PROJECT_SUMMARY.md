# ChatBubble 插件项目总结

## 项目概述

这是一个基于 Paper 1.21.8 的聊天气泡插件，使用 NMS（Net Minecraft Server）技术实现玩家头顶显示聊天内容的气泡效果。项目采用 Gradle 构建系统，支持多版本客户端。

## 项目结构

```
chatbubblenms/
├── build.gradle                    # Gradle 构建配置
├── gradle.properties               # Gradle 属性配置
├── gradle/wrapper/                 # Gradle Wrapper 文件
│   ├── gradle-wrapper.properties   # Wrapper 配置
│   └── gradle-wrapper.jar          # Wrapper JAR
├── gradlew                         # Gradle Wrapper 脚本
├── build.sh                        # 备用构建脚本
├── src/main/
│   ├── java/com/sagecraft/chatbubble/
│   │   ├── ChatBubblePlugin.java   # 主插件类
│   │   ├── managers/               # 管理器类
│   │   │   ├── ConfigManager.java  # 配置管理器
│   │   │   └── BubbleManager.java  # 气泡管理器
│   │   ├── nms/                    # NMS 处理器
│   │   │   ├── NMSHandler.java     # NMS 接口
│   │   │   └── NMSHandler_1_21.java # 1.21 版本实现
│   │   ├── objects/                # 对象类
│   │   │   └── ChatBubble.java     # 气泡对象
│   │   ├── listeners/              # 监听器
│   │   │   └── ChatListener.java   # 聊天监听器
│   │   ├── commands/               # 命令处理器
│   │   │   └── ChatBubbleCommand.java # 主命令
│   │   └── utils/                  # 工具类
│   │       └── TextUtils.java      # 文本工具
│   └── resources/
│       ├── plugin.yml              # 插件配置文件
│       └── config.yml              # 默认配置
├── resourcepack/                   # 材质包资源
│   ├── pack.mcmeta                 # 材质包元数据
│   ├── README.md                   # 材质包说明
│   └── assets/minecraft/textures/gui/
│       └── chat_bubble.png         # 气泡材质
├── README.md                       # 项目说明文档
└── PROJECT_SUMMARY.md              # 项目总结（本文件）
```

## 核心功能

### 1. 聊天气泡显示
- 玩家发送聊天消息时在头顶显示气泡
- 气泡包含格式化的聊天内容
- 支持自定义显示时间和样式

### 2. NMS 技术实现
- 使用 NMS 直接操作网络数据包
- 创建虚拟 ArmorStand 实体作为气泡载体
- 无需在服务器注册实体，提高性能

### 3. 多版本支持
- 支持 1.21+ 所有客户端版本
- 可选安装 ViaVersion 来支持更多客户端版本
- 服务器运行在 1.21.8，客户端可以是更高版本

### 4. 配置系统
- 完整的 YAML 配置文件
- 支持气泡样式、显示时间、消息格式等配置
- 运行时重载配置功能

### 5. 权限控制
- 支持权限节点控制
- 管理员命令权限
- 可选的用户权限要求

## 技术特点

### NMS 实现
```java
// 创建虚拟实体
ArmorStand armorStand = new ArmorStand(serverPlayer.level(), x, y, z);
armorStand.setInvisible(true);
armorStand.setCustomName(message);
armorStand.setCustomNameVisible(true);

// 发送数据包
ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(armorStand);
ClientboundSetEntityDataPacket dataPacket = new ClientboundSetEntityDataPacket(entityId, armorStand.getEntityData(), true);
```

### 多版本兼容
- 使用接口设计模式支持不同版本的 NMS
- 可以轻松添加新版本的处理器
- 自动检测服务器版本并选择合适的处理器

### 性能优化
- 气泡自动过期清理
- 只向附近玩家发送数据包
- 异步处理聊天事件

## 构建和部署

### 使用 Gradle 构建
```bash
./gradlew build
```

### 使用备用脚本构建
```bash
./build.sh
```

### 部署步骤
1. 构建项目生成 JAR 文件
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 可选：安装 ViaVersion 插件以支持更多客户端版本
4. 重启服务器或重载插件

## 配置示例

```yaml
# 气泡设置
bubble:
  display-duration: 10        # 显示时间（秒）
  height-offset: 2.5          # 高度偏移
  scale: 1.0                  # 大小
  enable-animation: true      # 动画效果

# 消息设置
messages:
  format: "&7[&f%player%&7] &f%message%"
  show-player-name: true
  max-length: 50

# 权限设置
permissions:
  require-permission: false
  permission-node: "chatbubble.use"
```

## 命令和权限

### 命令
- `/chatbubble reload` - 重载配置
- `/chatbubble toggle` - 切换个人开关
- `/chatbubble info` - 显示插件信息

### 权限
- `chatbubble.admin` - 管理员权限
- `chatbubble.use` - 使用气泡功能

## 扩展性

### 添加新版本支持
1. 创建新的 NMS 处理器类（如 `NMSHandler_1_22.java`）
2. 实现 `NMSHandler` 接口
3. 在主插件类中添加版本检测逻辑

### 自定义气泡样式
1. 修改材质包中的 `chat_bubble.png` 文件
2. 调整配置中的 `scale` 和 `height-offset` 参数
3. 可以添加更多样式选项到配置中

## 注意事项

1. **版本兼容性**: 确保使用正确的 Paper 版本（1.21.8+）
2. **可选依赖**: ViaVersion 是可选的，用于支持更多客户端版本
3. **Java 版本**: 需要 Java 17 或更高版本
4. **性能考虑**: 大量玩家同时使用可能影响性能，建议调整配置参数

## 未来改进

1. 添加更多气泡样式选项
2. 支持表情符号和特殊字符
3. 添加气泡动画效果
4. 支持自定义材质包
5. 添加统计和日志功能
6. 优化性能和内存使用

## 许可证

本项目采用 MIT 许可证，允许自由使用、修改和分发。
