package com.gc.mcp.server.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class ToolConfig {
    private String name;
    private String description;
    private Map<String, Object> meta = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta == null ? new LinkedHashMap<>() : new LinkedHashMap<>(meta);
    }
}
