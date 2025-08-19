# ChatBubble 贴图文件设置指南

## 贴图文件放置位置

**重要：** 贴图文件需要在**开发阶段**放置在项目中，编译后打包到jar文件中。

请将你的气泡背景贴图文件放置在以下目录中：

```
src/main/resources/textures/
```

## 需要的贴图文件

根据配置文件中的 `font_images` 设置，你需要准备以下贴图文件：

### 13px 高度气泡 (13px height bubbles)
- `chatl_13.png` - 左侧边框
- `chatm_13.png` - 中间部分
- `chatr_13.png` - 右侧边框
- `chatt_13.png` - 尾部箭头

### 23px 高度气泡 (23px height bubbles)
- `chatl_23.png` - 左侧边框
- `chatm_23.png` - 中间部分
- `chatr_23.png` - 右侧边框
- `chatt_23.png` - 尾部箭头

### 33px 高度气泡 (33px height bubbles)
- `chatl_33.png` - 左侧边框
- `chatm_33.png` - 中间部分
- `chatr_33.png` - 右侧边框
- `chatt_33.png` - 尾部箭头

## 贴图文件要求

1. **格式**: PNG格式
2. **透明度**: 支持透明背景
3. **尺寸**: 建议使用合适的尺寸，避免过大或过小
4. **命名**: 必须严格按照上述文件名命名

## 目录结构示例

### 开发阶段 (Development)
```
ChatBubble/
├── src/
│   └── main/
│       └── resources/
│           ├── config.yml
│           ├── plugin.yml
│           └── textures/
│               ├── chatl_13.png
│               ├── chatm_13.png
│               ├── chatr_13.png
│               ├── chatt_13.png
│               ├── chatl_23.png
│               ├── chatm_23.png
│               ├── chatr_23.png
│               ├── chatt_23.png
│               ├── chatl_33.png
│               ├── chatm_33.png
│               ├── chatr_33.png
│               └── chatt_33.png
└── build.gradle.kts
```

### 运行阶段 (Runtime)
```
plugins/ChatBubble/
├── ChatBubble.jar (包含贴图文件)
├── config.yml
└── ResourcePack/ (自动生成)
    ├── pack.mcmeta
    └── assets/
        └── chatbubble/
            ├── font/
            │   └── default.json
            └── textures/
                └── font/
                    └── bubbles/
                        └── (从jar中复制的贴图文件)
```

## 工作流程

1. **开发阶段**：
   - 将贴图文件放入 `src/main/resources/textures/` 目录
   - 编译项目：`./gradlew build`
   - 贴图文件会被打包到jar文件中

2. **部署阶段**：
   - 将生成的jar文件放入服务器的 `plugins/` 目录
   - 启动服务器

3. **运行阶段**：
   - 插件启动时自动从jar文件中读取贴图
   - 生成材质包到 `plugins/ChatBubble/ResourcePack/` 目录
   - 或使用命令 `/chatbubble resourcepack` 手动重新生成

## 生成材质包

1. 将贴图文件放置到正确位置后
2. 编译项目：`./gradlew build`
3. 部署jar文件到服务器
4. 重启服务器或使用命令 `/chatbubble resourcepack` 重新生成材质包
5. 插件会自动在 `plugins/ChatBubble/ResourcePack/` 目录中生成材质包

## 注意事项

- **重要**：贴图文件必须在编译前放入 `src/main/resources/textures/` 目录
- 确保贴图文件存在且可读
- 如果贴图文件不存在，插件会在日志中显示警告信息
- 材质包会在插件启动时自动生成
- 可以使用 `/chatbubble resourcepack` 命令手动重新生成材质包
- 生成的材质包可以直接作为服务器材质包使用
