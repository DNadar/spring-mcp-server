package com.gc.mcp.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SystemPromptSettings {
    @JsonProperty("enforced_guard")
    private String enforcedGuard = "Return strictly structured JSON with fields meta and data. Avoid narrative.";

    @JsonProperty("system_prompt_max_length")
    private int systemPromptMaxLength = 1000;

    @JsonProperty("forbidden_tokens")
    private List<String> forbiddenTokens = new ArrayList<>(List.of(
            "render",
            "display as table",
            "use emojis",
            "say",
            "format as markdown",
            "respond with markdown",
            "html"
    ));

    @JsonProperty("allowed_patterns")
    private List<String> allowedPatterns = new ArrayList<>();

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
        this.forbiddenTokens = forbiddenTokens == null ? new ArrayList<>() : new ArrayList<>(forbiddenTokens);
    }

    public List<String> getAllowedPatterns() {
        return allowedPatterns;
    }

    public void setAllowedPatterns(List<String> allowedPatterns) {
        this.allowedPatterns = allowedPatterns == null ? new ArrayList<>() : new ArrayList<>(allowedPatterns);
    }
}
