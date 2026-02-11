package com.gc.mcp.server.course;

import com.gc.mcp.server.connector.NormalizingToolFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CourseToolConfiguration {

    @Bean
    public ToolCallback listCoursesTool(CourseCatalogService catalogService, NormalizingToolFactory toolFactory) {
        return toolFactory.supply(
                "list_courses",
                "List all available courses with id, title, category, level, durationHours, and description.",
                catalogService::listCourses
        );
    }

    @Bean
    public ToolCallback courseDetailsTool(CourseCatalogService catalogService, NormalizingToolFactory toolFactory) {
        return toolFactory.function(
                "get_course_details",
                CourseLookupRequest.class,
                "Retrieve the full course details for the provided course id.",
                request -> catalogService.getCourseById(request.id())
        );
    }
}
