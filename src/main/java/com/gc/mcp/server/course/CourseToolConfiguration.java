package com.gc.mcp.server.course;

import com.gc.mcp.server.config.ToolConfigService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CourseToolConfiguration {

    @Bean
    public ToolCallback listCoursesTool(CourseCatalogService catalogService, ToolConfigService toolConfigService) {
        return FunctionToolCallback.builder("list_courses", catalogService::listCourses)
                .description(toolConfigService.descriptionOrDefault(
                        "list_courses",
                        "List all available courses with id, title, category, level, durationHours, and description."
                ))
                .build();
    }

    @Bean
    public ToolCallback courseDetailsTool(CourseCatalogService catalogService, ToolConfigService toolConfigService) {
        return FunctionToolCallback.builder("get_course_details",
                        (CourseLookupRequest request) -> catalogService.getCourseById(request.id()))
                .description(toolConfigService.descriptionOrDefault(
                        "get_course_details",
                        "Retrieve the full course details for the provided course id."
                ))
                .inputType(CourseLookupRequest.class)
                .build();
    }
}
