#!/bin/bash

# Java版本切换脚本
# 使用方法: ./java-switch.sh [版本号]
# 例如: ./java-switch.sh 21 或 ./java-switch.sh 22

if [ $# -eq 0 ]; then
    echo "当前Java版本:"
    java -version
    echo ""
    echo "可用的Java版本:"
    /usr/libexec/java_home -V
    echo ""
    echo "使用方法: $0 [版本号]"
    echo "例如: $0 21 或 $0 22"
    exit 1
fi

version=$1
export JAVA_HOME=$(/usr/libexec/java_home -v "$version" 2>/dev/null)

if [ $? -eq 0 ]; then
    echo "已切换到Java $version"
    java -version
else
    echo "错误: 找不到Java版本 $version"
    echo "可用的版本:"
    /usr/libexec/java_home -V
    exit 1
fi
