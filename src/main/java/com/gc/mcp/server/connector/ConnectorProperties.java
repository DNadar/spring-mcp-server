package com.gc.mcp.server.connector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "connector")
public class ConnectorProperties {

    /**
     * Simple throttle guard per session/user for connector calls.
     */
    private Duration throttleWindow = Duration.ofSeconds(1);

    private int throttleMaxRequests = 10;

    public Duration getThrottleWindow() {
        return throttleWindow;
    }

    public void setThrottleWindow(Duration throttleWindow) {
        this.throttleWindow = throttleWindow;
    }

    public int getThrottleMaxRequests() {
        return throttleMaxRequests;
    }

    public void setThrottleMaxRequests(int throttleMaxRequests) {
        this.throttleMaxRequests = throttleMaxRequests;
    }
}
