# 贴图文件放置说明

## 🎯 正确的文件放置位置

**请将你的贴图文件放在这里：**
```
src/main/resources/textures/
```

## 📁 需要的文件

你需要准备以下12个PNG文件：

### 13px 高度气泡
- `chatl_13.png` - 左侧边框
- `chatm_13.png` - 中间部分  
- `chatr_13.png` - 右侧边框
- `chatt_13.png` - 尾部箭头

### 23px 高度气泡
- `chatl_23.png` - 左侧边框
- `chatm_23.png` - 中间部分
- `chatr_23.png` - 右侧边框
- `chatt_23.png` - 尾部箭头

### 33px 高度气泡
- `chatl_33.png` - 左侧边框
- `chatm_33.png` - 中间部分
- `chatr_33.png` - 右侧边框
- `chatt_33.png` - 尾部箭头

## 🔄 工作流程

1. **放置文件** → 将贴图放入 `src/main/resources/textures/`
2. **编译项目** → `./gradlew build`
3. **部署jar** → 将jar放入服务器的 `plugins/` 目录
4. **启动服务器** → 插件会自动生成材质包

## ⚠️ 重要提醒

- 贴图文件必须在**编译前**放入项目目录
- 编译后贴图会被打包到jar文件中
- 运行时插件从jar中读取贴图并生成材质包
- 生成的材质包在 `plugins/ChatBubble/ResourcePack/` 目录

## 🎮 使用材质包

将生成的 `ResourcePack` 目录作为材质包应用到你的Minecraft服务器即可！
