package com.gc.mcp.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

@Configuration
public class ToolsYamlPropertyBridge {

    public ToolsYamlPropertyBridge(ToolConfigService toolConfigService, ConfigurableEnvironment environment) {
        Map<String, Object> props = Map.of(
                "spring.ai.mcp.server.instructions",
                toolConfigService.instructions()
        );
        environment.getPropertySources().addLast(new MapPropertySource("toolsYamlInstructions", props));
    }
}
