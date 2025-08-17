#!/bin/bash

echo "=== ChatBubble 插件项目测试 ==="
echo

# 检查项目结构
echo "1. 检查项目结构..."
if [ -d "src/main/java/com/sagecraft/chatbubble" ]; then
    echo "✓ Java 源码目录存在"
else
    echo "✗ Java 源码目录不存在"
fi

if [ -d "src/main/resources" ]; then
    echo "✓ 资源目录存在"
else
    echo "✗ 资源目录不存在"
fi

if [ -f "build.gradle" ]; then
    echo "✓ build.gradle 文件存在"
else
    echo "✗ build.gradle 文件不存在"
fi

if [ -f "src/main/resources/plugin.yml" ]; then
    echo "✓ plugin.yml 文件存在"
else
    echo "✗ plugin.yml 文件不存在"
fi

echo

# 检查主要类文件
echo "2. 检查主要类文件..."
java_files=(
    "src/main/java/com/sagecraft/chatbubble/ChatBubblePlugin.java"
    "src/main/java/com/sagecraft/chatbubble/managers/ConfigManager.java"
    "src/main/java/com/sagecraft/chatbubble/managers/BubbleManager.java"
    "src/main/java/com/sagecraft/chatbubble/nms/NMSHandler.java"
    "src/main/java/com/sagecraft/chatbubble/nms/NMSHandler_1_21.java"
    "src/main/java/com/sagecraft/chatbubble/objects/ChatBubble.java"
    "src/main/java/com/sagecraft/chatbubble/listeners/ChatListener.java"
    "src/main/java/com/sagecraft/chatbubble/commands/ChatBubbleCommand.java"
    "src/main/java/com/sagecraft/chatbubble/utils/TextUtils.java"
)

for file in "${java_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ $file (缺失)"
    fi
done

echo

# 检查配置文件
echo "3. 检查配置文件..."
config_files=(
    "src/main/resources/config.yml"
    "gradle.properties"
    "gradle/wrapper/gradle-wrapper.properties"
)

for file in "${config_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ $file (缺失)"
    fi
done

echo

# 检查文档文件
echo "4. 检查文档文件..."
doc_files=(
    "README.md"
    "PROJECT_SUMMARY.md"
    "resourcepack/README.md"
)

for file in "${doc_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ $file (缺失)"
    fi
done

echo

# 统计文件数量
echo "5. 项目统计..."
java_count=$(find src/main/java -name "*.java" | wc -l)
resource_count=$(find src/main/resources -type f | wc -l)
total_files=$(find . -type f -not -path "./.git/*" -not -path "./.gradle/*" | wc -l)

echo "Java 文件数量: $java_count"
echo "资源文件数量: $resource_count"
echo "总文件数量: $total_files"

echo
echo "=== 测试完成 ==="
echo
echo "项目已成功创建！"
echo "下一步："
echo "1. 安装 Java 17+ 和 Gradle"
echo "2. 运行 './gradlew build' 构建项目"
echo "3. 将生成的 JAR 文件放入服务器 plugins 文件夹"
echo "4. 可选：安装 ViaVersion 插件以支持更多客户端版本"
echo "5. 重启服务器测试插件"
