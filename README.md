# ChatBubble Plugin

一个基于Paper 1.21.8的聊天气泡插件，使用NMS技术实现虚拟实体显示。

## 功能特性

- 🎯 **聊天气泡显示**: 玩家聊天时在头顶显示气泡
- 🔧 **NMS技术**: 使用虚拟ArmorStand实体，无需服务器注册
- 🎮 **多版本支持**: 支持1.21+客户端版本
- ⚙️ **可配置**: 丰富的配置选项
- 🚀 **高性能**: 优化的实体管理和数据包发送

## 技术架构

- **Paper API**: 1.21.8-R0.1-SNAPSHOT
- **Java版本**: 21
- **构建工具**: Gradle 8.8
- **NMS实现**: 虚拟实体 + 网络数据包

## 快速开始

### 构建项目

```bash
# 克隆项目
git clone https://github.com/wangzaihehe/ChatBubble.git
cd ChatBubble

# 构建插件
./gradlew build
```

### 安装插件

1. 将 `build/libs/chatbubblenms-1.0.0.jar` 复制到服务器的 `plugins` 文件夹
2. 重启服务器
3. 使用 `/chatbubble` 命令测试功能

## 配置说明

### 基础配置 (config.yml)

```yaml
# 调试模式
debug: false

# 气泡显示时间（秒）
bubble-duration: 5

# 气泡显示距离
view-distance: 50

# 气泡样式
bubble-style:
  prefix: "💬 "
  suffix: ""
```

### 命令

- `/chatbubble` - 显示帮助信息
- `/chatbubble toggle` - 切换气泡显示
- `/chatbubble reload` - 重载配置

## 开发说明

### 项目结构

```
src/main/java/com/sagecraft/chatbubble/
├── ChatBubblePlugin.java      # 主插件类
├── managers/                  # 管理器
│   ├── ConfigManager.java     # 配置管理
│   └── BubbleManager.java     # 气泡管理
├── nms/                      # NMS处理器
│   ├── NMSHandler.java       # NMS接口
│   └── NMSHandler_1_21.java  # 1.21版本实现
├── commands/                 # 命令处理
├── listeners/                # 事件监听
├── objects/                  # 数据对象
└── utils/                    # 工具类
```

### NMS实现

插件使用NMS技术创建虚拟ArmorStand实体来显示聊天气泡：

- 创建虚拟ArmorStand实体
- 设置实体属性（不可见、无重力、标记等）
- 发送网络数据包给客户端
- 管理实体生命周期

### 版本兼容性

- **服务器**: Paper 1.21.8+
- **客户端**: 1.21+ (通过ViaVersion支持更早版本)
- **Java**: 21+

## 许可证

本项目采用 MIT 许可证。

## 贡献

欢迎提交Issue和Pull Request！

## 更新日志

### v1.0.0
- 初始版本
- 基础聊天气泡功能
- NMS框架实现
- 配置系统
- 命令系统

## 联系方式

- GitHub: [wangzaihehe](https://github.com/wangzaihehe)
- 项目地址: [ChatBubble](https://github.com/wangzaihehe/ChatBubble)
