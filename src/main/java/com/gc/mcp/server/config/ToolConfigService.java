package com.gc.mcp.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private final SystemPromptSettings systemPromptSettings;
    private final String instructions;

    public ToolConfigService() {
        LoadedConfig loadedConfig = load(DEFAULT_RESOURCE);
        this.toolsByName = loadedConfig.toolsByName();
        this.systemPromptSettings = loadedConfig.systemPromptSettings();
        this.instructions = loadedConfig.instructions();
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

    public SystemPromptSettings systemPromptSettings() {
        return systemPromptSettings;
    }

    public String instructions() {
        return instructions;
    }

    private LoadedConfig load(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            logger.warn("Tool config resource {} not found; using defaults", path);
            return new LoadedConfig(Map.of(), new SystemPromptSettings(), defaultInstructions());
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            ToolConfigList wrapper = mapper.readValue(resource.getInputStream(), ToolConfigList.class);
            if (wrapper == null || wrapper.tools == null) {
                logger.warn("Tool config resource {} is empty", path);
                return new LoadedConfig(Map.of(), defaultSystemPromptSettings(wrapper), defaultInstructions(wrapper));
            }
            Map<String, ToolConfig> mapped = wrapper.tools.stream()
                    .collect(Collectors.toUnmodifiableMap(ToolConfig::getName, t -> t));
            return new LoadedConfig(mapped, defaultSystemPromptSettings(wrapper), defaultInstructions(wrapper));
        } catch (IOException e) {
            logger.error("Failed to load tool config from {}", path, e);
            return new LoadedConfig(Map.of(), new SystemPromptSettings(), defaultInstructions());
        }
    }

    private SystemPromptSettings defaultSystemPromptSettings(ToolConfigList wrapper) {
        if (wrapper != null && wrapper.systemPrompt != null) {
            return wrapper.systemPrompt;
        }
        return new SystemPromptSettings();
    }

    private String defaultInstructions(ToolConfigList wrapper) {
        if (wrapper != null && wrapper.instructions != null && !wrapper.instructions.isBlank()) {
            return wrapper.instructions;
        }
        return defaultInstructions();
    }

    private String defaultInstructions() {
        return "Provide course catalog information. Use the list_courses tool to discover courses and get_course_details to drill into a specific course by id.";
    }

    private static class ToolConfigList {
        public List<ToolConfig> tools;
        @JsonProperty("system_prompt")
        public SystemPromptSettings systemPrompt;
        public String instructions;
    }

    private record LoadedConfig(Map<String, ToolConfig> toolsByName,
                                SystemPromptSettings systemPromptSettings,
                                String instructions) {
    }
}
