#!/bin/bash

# 简单的构建脚本
echo "开始构建 ChatBubble 插件..."

# 创建输出目录
mkdir -p build/classes
mkdir -p build/libs

# 设置类路径
CLASSPATH="lib/*:build/classes"

# 编译Java文件
echo "编译Java文件..."
find src/main/java -name "*.java" -exec javac -cp "$CLASSPATH" -d build/classes {} \;

if [ $? -eq 0 ]; then
    echo "编译成功！"
    
    # 复制资源文件
    echo "复制资源文件..."
    cp -r src/main/resources/* build/classes/
    
    # 创建JAR文件
    echo "创建JAR文件..."
    cd build/classes
    jar cf ../libs/ChatBubble-1.0.0.jar *
    cd ../..
    
    echo "构建完成！插件文件位于: build/libs/ChatBubble-1.0.0.jar"
else
    echo "编译失败！"
    exit 1
fi
