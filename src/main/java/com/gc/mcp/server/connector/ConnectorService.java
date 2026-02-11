package com.gc.mcp.server.connector;

import com.gc.mcp.server.course.CourseCatalogService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ConnectorService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorService.class);

    private final CourseCatalogService courseCatalogService;
    private final SystemPromptValidator promptValidator;
    private final ConnectorResponseMapper responseMapper;
    private final ConnectorRateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;

    public ConnectorService(CourseCatalogService courseCatalogService,
                            SystemPromptValidator promptValidator,
                            ConnectorResponseMapper responseMapper,
                            ConnectorRateLimiter rateLimiter,
                            MeterRegistry meterRegistry) {
        this.courseCatalogService = courseCatalogService;
        this.promptValidator = promptValidator;
        this.responseMapper = responseMapper;
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
    }

    public ConnectorResponse handle(ConnectorRequest request) {
        meterRegistry.counter("connector.calls.total").increment();
        String throttleKey = request.sessionId() != null
                ? request.sessionId()
                : request.userId() != null ? request.userId() : "anonymous";
        if (!rateLimiter.allow(throttleKey)) {
            meterRegistry.counter("connector.calls.throttled").increment();
            throw new IllegalStateException("Request rate exceeded");
        }

        SystemPromptValidator.ValidatedPrompt validatedPrompt = promptValidator.validate(request.systemPrompt());
        Map<String, Object> outgoingPayload = buildOutgoingPayload(request, validatedPrompt.forwardedPrompt());
        logger.info("Forwarding connector payload with guard applied for tool {}", request.tool());

        Timer.Sample sample = Timer.start(meterRegistry);
        Object downstreamResult = callTool(request.tool(), request.args());
        ConnectorResponse normalized = responseMapper.normalize(request.tool(), downstreamResult);
        long elapsedMs = Duration.ofNanos(sample.stop(Timer.builder("connector.downstream.latency")
                        .description("Latency for downstream call + normalization")
                        .register(meterRegistry)))
                .toMillis();

        Map<String, Object> diagnostics = new LinkedHashMap<>(normalized.diagnostics());
        diagnostics.put("latency_ms", elapsedMs);
        diagnostics.put("system_prompt_applied", true);
        diagnostics.put("request_id", UUID.randomUUID().toString());
        diagnostics.put("system_prompt_guard", validatedPrompt.forwardedPrompt());
        diagnostics.put("outgoing_payload", outgoingPayload);

        return new ConnectorResponse(normalized.meta(), normalized.data(), diagnostics);
    }

    private Map<String, Object> buildOutgoingPayload(ConnectorRequest request, String forwardedPrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", request.tool());
        payload.put("args", request.args());
        payload.put("user_id", request.userId());
        payload.put("session_id", request.sessionId());
        payload.put("system_prompt", forwardedPrompt);
        return payload;
    }

    private Object callTool(String tool, Map<String, Object> args) {
        return switch (tool) {
            case "list_courses" -> courseCatalogService.listCourses();
            case "get_course_details" -> {
                Object id = args.get("id");
                if (id == null || id.toString().isBlank()) {
                    throw new IllegalArgumentException("get_course_details requires args.id");
                }
                yield courseCatalogService.getCourseById(id.toString());
            }
            default -> throw new IllegalArgumentException("Unknown tool: " + tool);
        };
    }
}
