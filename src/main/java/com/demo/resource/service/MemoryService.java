package com.demo.resource.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MemoryService {

    private static final Map<String, List<byte[]>> memoryStore = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    private Counter loadCounter;
    private Counter stableCounter;

    private final MeterRegistry meterRegistry;

    public MemoryService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        loadCounter = Counter.builder("memory.load.calls")
                .description("Number of memory load endpoint calls")
                .register(meterRegistry);
        stableCounter = Counter.builder("memory.stable.calls")
                .description("Number of memory stable endpoint calls")
                .register(meterRegistry);
    }

    /**
     * Memory accumulation operation with minimal CPU usage
     */
    public Map<String, Object> accumulateMemory(int objectCount, int sizeMB) {
        loadCounter.increment();
        
        long startTime = System.currentTimeMillis();
        
        String key = UUID.randomUUID().toString();
        List<byte[]> dataList = new ArrayList<>();
        
        for (int i = 0; i < objectCount; i++) {
            byte[] data = new byte[sizeMB * 1024 * 1024];
            random.nextBytes(data);
            dataList.add(data);
        }
        
        memoryStore.put(key, dataList);
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "memory-load");
        response.put("objectsCreated", objectCount);
        response.put("sizePerObjectMB", sizeMB);
        response.put("totalStoredMB", objectCount * sizeMB);
        response.put("totalKeysInStore", memoryStore.size());
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * Stable memory operation - no accumulation
     */
    public Map<String, Object> performStableMemoryOperation() {
        stableCounter.increment();
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "memory-stable");
        response.put("status", "ok");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Get current memory store size
     */
    public int getMemoryStoreSize() {
        return memoryStore.size();
    }

    /**
     * Clear accumulated memory
     */
    public void clearMemory() {
        memoryStore.clear();
    }
}
