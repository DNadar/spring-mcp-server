package com.gc.mcp.server.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ConnectorRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorRateLimiter.class);

    private final Duration window;
    private final int maxRequests;
    private final Map<String, Window> state = new ConcurrentHashMap<>();

    public ConnectorRateLimiter(ConnectorProperties properties) {
        this.window = properties.getThrottleWindow();
        this.maxRequests = properties.getThrottleMaxRequests();
    }

    public boolean allow(String key) {
        Window current = state.computeIfAbsent(key, k -> new Window());
        synchronized (current) {
            Instant now = Instant.now();
            if (Duration.between(current.windowStart, now).compareTo(window) > 0) {
                current.windowStart = now;
                current.counter.set(0);
            }
            int next = current.counter.incrementAndGet();
            boolean allowed = next <= maxRequests;
            if (!allowed) {
                logger.warn("Throttling connector key {} after {} requests in {}", key, next, window);
            }
            return allowed;
        }
    }

    private static final class Window {
        private Instant windowStart = Instant.now();
        private final AtomicInteger counter = new AtomicInteger(0);
    }
}
