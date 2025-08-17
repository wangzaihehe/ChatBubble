# Java版本管理说明

## 已安装的Java版本
- Java 21 (OpenJDK 21.0.8) - 通过Homebrew安装
- Java 22 (Oracle JDK 22.0.2) - 系统默认

## Java版本切换

### 方法1: 使用脚本
```bash
# 查看当前版本和可用版本
./java-switch.sh

# 切换到Java 21
./java-switch.sh 21

# 切换到Java 22
./java-switch.sh 22
```

### 方法2: 手动切换
```bash
# 切换到Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# 切换到Java 22
export JAVA_HOME=$(/usr/libexec/java_home -v 22)

# 验证当前版本
java -version
```

### 方法3: 永久设置（推荐）
将以下内容添加到你的 `~/.zshrc` 文件中：

```bash
# 设置默认Java版本为21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Java版本切换函数
jdk() {
    version=$1
    export JAVA_HOME=$(/usr/libexec/java_home -v"$version");
    java -version
}
```

然后重新加载配置：
```bash
source ~/.zshrc
```

使用函数切换版本：
```bash
jdk 21  # 切换到Java 21
jdk 22  # 切换到Java 22
```

## 项目构建
确保使用Java 21进行构建：
```bash
./gradlew clean build
```

构建成功后，插件文件将生成在 `build/libs/ChatBubble-1.0.0-dev.jar`
