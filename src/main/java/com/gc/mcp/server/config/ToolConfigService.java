package com.gc.mcp.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ToolConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ToolConfigService.class);
    private static final String DEFAULT_RESOURCE = "tools.yaml";

    private final Map<String, ToolConfig> toolsByName;

    public ToolConfigService() {
        this.toolsByName = load(DEFAULT_RESOURCE);
    }

    public Optional<ToolConfig> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public String descriptionOrDefault(String name, String fallback) {
        return find(name).map(ToolConfig::getDescription).filter(s -> !s.isBlank()).orElse(fallback);
    }

    public Map<String, Object> metaOrDefault(String name) {
        return find(name).map(ToolConfig::getMeta).orElse(Map.of());
    }

    private Map<String, ToolConfig> load(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            logger.warn("Tool config resource {} not found; using defaults", path);
            return Map.of();
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            ToolConfigList wrapper = mapper.readValue(resource.getInputStream(), ToolConfigList.class);
            if (wrapper == null || wrapper.tools == null) {
                logger.warn("Tool config resource {} is empty", path);
                return Map.of();
            }
            return wrapper.tools.stream()
                    .collect(Collectors.toUnmodifiableMap(ToolConfig::getName, t -> t));
        } catch (IOException e) {
            logger.error("Failed to load tool config from {}", path, e);
            return Map.of();
        }
    }

    private static class ToolConfigList {
        public List<ToolConfig> tools;
    }
}
