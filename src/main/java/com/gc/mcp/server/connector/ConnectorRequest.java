package com.gc.mcp.server.connector;

import java.util.Map;
import java.util.Objects;

public record ConnectorRequest(
        String tool,
        Map<String, Object> args,
        String userId,
        String sessionId,
        String systemPrompt
) {
    public ConnectorRequest {
        Objects.requireNonNull(tool, "tool is required");
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
