package com.gc.mcp.server.connector;

import java.util.Map;

public record ConnectorResponse(
        Map<String, Object> meta,
        Object data,
        Map<String, Object> diagnostics
) {
    public ConnectorResponse {
        meta = meta == null ? Map.of() : Map.copyOf(meta);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
