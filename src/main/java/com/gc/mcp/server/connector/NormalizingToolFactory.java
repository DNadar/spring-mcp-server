package com.gc.mcp.server.connector;

import com.gc.mcp.server.config.ToolConfigService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class NormalizingToolFactory {

    private final ToolConfigService toolConfigService;
    private final ConnectorResponseMapper responseMapper;

    public NormalizingToolFactory(ToolConfigService toolConfigService, ConnectorResponseMapper responseMapper) {
        this.toolConfigService = toolConfigService;
        this.responseMapper = responseMapper;
    }

    public ToolCallback supply(String name, String fallbackDescription, Supplier<?> handler) {
        return FunctionToolCallback.builder(name, () -> wrap(name, handler.get()))
                .description(toolConfigService.descriptionOrDefault(name, fallbackDescription))
                .build();
    }

    public <T> ToolCallback function(String name,
                                     Class<T> inputType,
                                     String fallbackDescription,
                                     Function<T, ?> handler) {
        return FunctionToolCallback.builder(name, (Function<T, Object>) input -> wrap(name, handler.apply(input)))
                .description(toolConfigService.descriptionOrDefault(name, fallbackDescription))
                .inputType(inputType)
                .build();
    }

    private ConnectorResponse wrap(String tool, Object rawResult) {
        ConnectorResponse normalized = responseMapper.normalize(tool, rawResult);
        Map<String, Object> mergedMeta = new LinkedHashMap<>(normalized.meta());
        mergedMeta.putAll(toolConfigService.metaOrDefault(tool));
        return new ConnectorResponse(mergedMeta, normalized.data(), normalized.diagnostics());
    }
}
