package com.gc.mcp.server.connector;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SystemPromptValidator {

    private static final Logger logger = LoggerFactory.getLogger(SystemPromptValidator.class);

    private final ConnectorProperties properties;
    private final Counter rejectedCounter;

    public SystemPromptValidator(ConnectorProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.rejectedCounter = meterRegistry.counter("connector.prompts.rejected");
    }

    public ValidatedPrompt validate(String prompt) {
        String sanitized = prompt == null ? "" : prompt.trim();
        logPrompt("received", sanitized);

        if (sanitized.length() > properties.getSystemPromptMaxLength()) {
            return reject("systemPrompt too long");
        }

        String lower = sanitized.toLowerCase(Locale.ROOT);
        for (String forbidden : properties.getForbiddenTokens()) {
            if (lower.contains(forbidden.toLowerCase(Locale.ROOT))) {
                return reject("Forbidden directive detected: " + forbidden);
            }
        }

        if (!properties.getAllowedPatterns().isEmpty()) {
            boolean matched = properties.getAllowedPatterns().stream()
                    .map(Pattern::compile)
                    .anyMatch(pattern -> pattern.matcher(sanitized).find());
            if (!matched) {
                return reject("systemPrompt did not match allowlist");
            }
        }

        String forwarded = buildForwardedPrompt(sanitized);
        return new ValidatedPrompt(sanitized, forwarded);
    }

    private ValidatedPrompt reject(String reason) {
        rejectedCounter.increment();
        logger.warn("Rejected systemPrompt: {}", reason);
        throw new IllegalArgumentException(reason);
    }

    private String buildForwardedPrompt(String sanitized) {
        if (sanitized.isBlank()) {
            return properties.getEnforcedGuard();
        }
        return properties.getEnforcedGuard() + " " + sanitized;
    }

    private void logPrompt(String action, String prompt) {
        String preview = prompt.length() > 160 ? prompt.substring(0, 160) + "..." : prompt;
        logger.info("System prompt {}: {}", action, preview);
    }

    public record ValidatedPrompt(String sanitizedPrompt, String forwardedPrompt) {
    }
}
