package com.sagecraft.chatbubble.utils;

public class CharacterUtils {
    
    /**
     * 将字符转换为Unicode字符串
     * @param character 要转换的字符
     * @return Unicode字符串
     */
    public static String char2Unicode(char character) {
        return String.valueOf(character);
    }
    
    /**
     * 将字符串转换为Unicode字符串
     * @param str 要转换的字符串
     * @return Unicode字符串
     */
    public static String char2Unicode(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str;
    }
    
    /**
     * 获取指定索引的Unicode字符
     * @param index 字符索引
     * @return Unicode字符
     */
    public static char getUnicodeChar(int index) {
        // 使用私有使用区域 (Private Use Area) 的字符
        // 范围: U+E000 到 U+F8FF
        if (index >= 0 && index < 6400) {
            return (char) (0xE000 + index);
        }
        return '\uE000'; // 默认返回第一个私有使用字符
    }
    
    /**
     * 获取指定索引的Unicode字符串
     * @param index 字符索引
     * @return Unicode字符串
     */
    public static String getUnicodeString(int index) {
        return String.valueOf(getUnicodeChar(index));
    }
}
