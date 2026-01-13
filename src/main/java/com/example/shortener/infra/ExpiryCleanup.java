package com.example.shortener.infra;

import com.example.shortener.core.service.ShortenerService;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ExpiryCleanup implements AutoCloseable {
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "expiry-cleaner");
        t.setDaemon(true);
        return t;
    });

    public ExpiryCleanup(ShortenerService service, Duration interval) {
        long sec = Math.max(1, interval.getSeconds());
        exec.scheduleAtFixedRate(() -> {
            try {
                int removed = service.cleanupExpiredNow();
                if (removed > 0) System.out.println("[CLEANUP] removed expired: " + removed);
            } catch (Exception e) {
                System.out.println("[CLEANUP] error: " + e.getMessage());
            }
        }, sec, sec, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        exec.shutdownNow();
    }
}