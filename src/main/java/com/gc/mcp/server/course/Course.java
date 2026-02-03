package com.gc.mcp.server.course;

public record Course(
        String id,
        String title,
        String category,
        String level,
        int durationHours,
        String description
) {
}
