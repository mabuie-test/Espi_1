package com.espi.streamer;

public class MemoryManager {
    public long getFreeMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        long free = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        return free / (1024 * 1024);
    }

    public boolean shouldThrottle(long thresholdMb) {
        return getFreeMemoryMb() < thresholdMb;
    }

    public String cpuHint() {
        long free = getFreeMemoryMb();
        if (free < 64) {
            return "high";
        }
        if (free < 128) {
            return "medium";
        }
        return "low";
    }
}
