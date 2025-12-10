package com.demo.resource.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class CpuService {

    private static final Map<String, List<byte[]>> dataStore = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    private Counter loadCounter;
    private Counter stableCounter;

    private final MeterRegistry meterRegistry;

    public CpuService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        loadCounter = Counter.builder("cpu.load.calls")
                .description("Number of CPU load endpoint calls")
                .register(meterRegistry);
        stableCounter = Counter.builder("cpu.stable.calls")
                .description("Number of CPU stable endpoint calls")
                .register(meterRegistry);
    }

    /**
     * CPU-intensive operation that also accumulates memory
     */
    public Map<String, Object> performCpuAndMemoryLoad(int iterations, int dataSize) {
        loadCounter.increment();
        
        long startTime = System.currentTimeMillis();
        
        // CPU-intensive: Calculate primes
        List<Long> primes = calculatePrimes(iterations * 100);
        
        // CPU-intensive: Parallel stream operations
        List<Integer> numbers = IntStream.range(0, iterations * 1000)
                .boxed()
                .collect(Collectors.toList());
        
        List<Integer> processed = numbers.parallelStream()
                .filter(n -> n % 2 == 0)
                .map(n -> n * n)
                .sorted(Comparator.reverseOrder())
                .limit(iterations * 100)
                .collect(Collectors.toList());
        
        // CPU-intensive: Matrix multiplication
        int[][] result = matrixMultiplication(50, 50);
        
        // Memory accumulation: Store large byte arrays
        String key = UUID.randomUUID().toString();
        List<byte[]> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            byte[] data = new byte[dataSize * 1024 * 1024]; // dataSize MB
            random.nextBytes(data);
            dataList.add(data);
        }
        dataStore.put(key, dataList);
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "cpu-memory-load");
        response.put("primesCalculated", primes.size());
        response.put("numbersProcessed", processed.size());
        response.put("matrixSize", "50x50");
        response.put("dataStoredMB", dataSize * 10);
        response.put("totalKeysInStore", dataStore.size());
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * CPU-intensive operation without memory accumulation (stateless)
     */
    public Map<String, Object> performCpuOnlyLoad(int iterations) {
        stableCounter.increment();
        
        long startTime = System.currentTimeMillis();
        
        // CPU-intensive: Fibonacci sequence
        List<Long> fibonacci = new ArrayList<>();
        for (int i = 0; i < iterations * 10; i++) {
            fibonacci.add(calculateFibonacci(30));
        }
        
        // CPU-intensive: String operations
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iterations * 1000; i++) {
            sb.append(UUID.randomUUID().toString());
            if (sb.length() > 10000) {
                sb = new StringBuilder(); // Reset to avoid accumulation
            }
        }
        
        // CPU-intensive: Complex sorting
        List<Integer> largeList = IntStream.range(0, iterations * 5000)
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(largeList);
        largeList.sort(Comparator.reverseOrder());
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "cpu-only-load");
        response.put("fibonacciCalculations", fibonacci.size());
        response.put("itemsSorted", largeList.size());
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * Minimal CPU operation (stable baseline)
     */
    public Map<String, Object> performStableCpuOperation() {
        stableCounter.increment();
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "cpu-stable");
        response.put("status", "ok");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Get current data store size
     */
    public int getDataStoreSize() {
        return dataStore.size();
    }

    /**
     * Clear accumulated data
     */
    public void clearData() {
        dataStore.clear();
    }

    // Helper methods
    
    private List<Long> calculatePrimes(int limit) {
        return IntStream.range(2, limit)
                .filter(this::isPrime)
                .mapToLong(i -> (long) i)
                .boxed()
                .collect(Collectors.toList());
    }

    private boolean isPrime(int number) {
        if (number <= 1) return false;
        if (number <= 3) return true;
        if (number % 2 == 0 || number % 3 == 0) return false;
        
        for (int i = 5; i * i <= number; i += 6) {
            if (number % i == 0 || number % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    private long calculateFibonacci(int n) {
        if (n <= 1) return n;
        
        long a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            long temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }

    private int[][] matrixMultiplication(int size1, int size2) {
        int[][] matrix1 = new int[size1][size2];
        int[][] matrix2 = new int[size2][size1];
        int[][] result = new int[size1][size1];
        
        // Fill matrices
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size2; j++) {
                matrix1[i][j] = random.nextInt(10);
            }
        }
        
        for (int i = 0; i < size2; i++) {
            for (int j = 0; j < size1; j++) {
                matrix2[i][j] = random.nextInt(10);
            }
        }
        
        // Multiply
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size1; j++) {
                for (int k = 0; k < size2; k++) {
                    result[i][j] += matrix1[i][k] * matrix2[k][j];
                }
            }
        }
        
        return result;
    }
}
