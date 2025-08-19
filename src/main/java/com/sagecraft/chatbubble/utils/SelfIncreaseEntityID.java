package com.sagecraft.chatbubble.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class SelfIncreaseEntityID {
    
    private static final AtomicInteger counter = new AtomicInteger(1000000); // 从1000000开始，避免与实体ID冲突
    
    public static int getAndIncrease() {
        return counter.getAndIncrement();
    }
    
    public static int get() {
        return counter.get();
    }
    
    public static void reset() {
        counter.set(1000000);
    }
}
