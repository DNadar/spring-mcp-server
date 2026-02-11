package com.gc.mcp.server.connector;

import com.gc.mcp.server.course.Course;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ConnectorResponseMapper {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorResponseMapper.class);
    private static final Set<String> NARRATIVE_FIELDS = Set.of("assistant_text", "message", "narrative", "text");
    private static final Set<String> ALLOWED_VIEWS = Set.of("summary", "detailed", "compact");

    public ConnectorResponse normalize(String tool, Object raw) {
        rejectNarrativeFields(raw, "response");

        if (raw instanceof Map<?, ?> rawMap && rawMap.containsKey("meta") && rawMap.containsKey("data")) {
            Map<String, Object> meta = ensureMap(rawMap.get("meta"), "meta");
            Map<String, Object> diagnostics = ensureOptionalMap(rawMap.get("diagnostics"), "diagnostics");
            rejectNarrative(meta, "meta");
            Object data = rawMap.get("data");
            validateView(meta);
            ensureSource(meta, tool);
            return new ConnectorResponse(meta, data, diagnostics);
        }

        if (raw instanceof Course course) {
            return mapCourseDetail(tool, course);
        }

        if (raw instanceof Collection<?> collection) {
            return mapCollection(tool, collection);
        }

        if (raw == null) {
            return new ConnectorResponse(defaultMeta(tool, "unknown", "compact", 0), Map.of(), diagnostics(tool));
        }

        // fallback: wrap any object in the contract
        return new ConnectorResponse(defaultMeta(tool, "unknown", "summary", null), raw, diagnostics(tool));
    }

    private void rejectNarrativeFields(Object payload, String location) {
        if (payload instanceof Map<?, ?> map) {
            rejectNarrative(map, location);
            map.values().forEach(value -> rejectNarrativeFields(value, location));
        } else if (payload instanceof Collection<?> collection) {
            collection.forEach(item -> rejectNarrativeFields(item, location));
        } else if (payload instanceof String) {
            throw new IllegalArgumentException("Narrative text responses are not permitted (" + location + ")");
        }
    }

    private ConnectorResponse mapCollection(String tool, Collection<?> collection) {
        var meta = defaultMeta(tool, intentForTool(tool), "summary", collection.size());
        meta.put("view", pickView("summary"));
        meta.put("count", collection.size());
        return new ConnectorResponse(meta, new ArrayList<>(collection), diagnostics(tool));
    }

    private ConnectorResponse mapCourseDetail(String tool, Course course) {
        var meta = defaultMeta(tool, "course_details", "detailed", 1);
        meta.put("highlight_ids", List.of(course.id()));
        meta.put("view", pickView("detailed"));
        return new ConnectorResponse(meta, course, diagnostics(tool));
    }

    private Map<String, Object> defaultMeta(String tool, String intent, String view, Integer count) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("intent", intent);
        meta.put("view", pickView(view));
        meta.put("source", tool);
        if (count != null) {
            meta.put("count", count);
        }
        return meta;
    }

    private Map<String, Object> diagnostics(String tool) {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("source", tool);
        return diagnostics;
    }

    private void ensureSource(Map<String, Object> meta, String tool) {
        meta.putIfAbsent("source", tool);
    }

    private void validateView(Map<String, Object> meta) {
        Object view = meta.get("view");
        if (view == null) {
            meta.put("view", pickView("summary"));
            return;
        }
        String viewValue = view.toString().toLowerCase(Locale.ROOT);
       /* if (!ALLOWED_VIEWS.contains(viewValue)) {
            throw new IllegalArgumentException("Unsupported meta.view: " + viewValue);
        }*/
        meta.put("view", viewValue);
    }

    private Map<String, Object> ensureMap(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Missing " + field + " object");
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(field + " must be an object");
        }
        rejectNarrative(map, field);
        Map<String, Object> copy = new LinkedHashMap<>();
        map.forEach((k, v) -> copy.put(String.valueOf(k), v));
        return copy;
    }

    private Map<String, Object> ensureOptionalMap(Object value, String field) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        return ensureMap(value, field);
    }

    private void rejectNarrative(Map<?, ?> map, String fieldName) {
        for (String forbidden : NARRATIVE_FIELDS) {
            for (Object key : map.keySet()) {
                if (forbidden.equalsIgnoreCase(String.valueOf(key))) {
                    logger.warn("Rejecting downstream {} because it contained narrative field {}", fieldName, forbidden);
                    throw new IllegalArgumentException("Narrative fields are not permitted in downstream responses");
                }
            }
        }
    }

    private String pickView(String requested) {
        String view = requested == null ? "summary" : requested.toLowerCase(Locale.ROOT);
        if (!ALLOWED_VIEWS.contains(view)) {
            return "summary";
        }
        return view;
    }

    private String intentForTool(String tool) {
        return switch (tool) {
            case "list_courses" -> "list_courses";
            case "get_course_details" -> "course_details";
            default -> "unknown";
        };
    }
}
