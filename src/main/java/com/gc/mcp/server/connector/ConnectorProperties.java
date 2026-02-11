package com.gc.mcp.server.connector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "connector")
public class ConnectorProperties {

    /**
     * Short enforced guard that is always prepended to any forwarded system prompt.
     */
    private String enforcedGuard = "Return strictly structured JSON with fields meta and data. Avoid narrative.";

    /**
     * Max characters allowed for incoming systemPrompt values.
     */
    private int systemPromptMaxLength = 1000;

    /**
     * Case-insensitive tokens that should cause a prompt to be rejected.
     */
    private List<String> forbiddenTokens = new ArrayList<>(List.of(
            "render",
            "display as table",
            "use emojis",
            "say",
            "format as markdown",
            "respond with markdown",
            "html"
    ));

    /**
     * Optional allowlist of regex patterns; if set, the prompt must match at least one.
     */
    private List<String> allowedPatterns = new ArrayList<>();

    /**
     * Simple throttle guard per session/user for connector calls.
     */
    private Duration throttleWindow = Duration.ofSeconds(1);

    private int throttleMaxRequests = 10;

    public String getEnforcedGuard() {
        return enforcedGuard;
    }

    public void setEnforcedGuard(String enforcedGuard) {
        this.enforcedGuard = enforcedGuard;
    }

    public int getSystemPromptMaxLength() {
        return systemPromptMaxLength;
    }

    public void setSystemPromptMaxLength(int systemPromptMaxLength) {
        this.systemPromptMaxLength = systemPromptMaxLength;
    }

    public List<String> getForbiddenTokens() {
        return forbiddenTokens;
    }

    public void setForbiddenTokens(List<String> forbiddenTokens) {
        this.forbiddenTokens = forbiddenTokens;
    }

    public List<String> getAllowedPatterns() {
        return allowedPatterns;
    }

    public void setAllowedPatterns(List<String> allowedPatterns) {
        this.allowedPatterns = allowedPatterns;
    }

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
