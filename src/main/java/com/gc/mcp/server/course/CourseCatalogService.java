package com.gc.mcp.server.course;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CourseCatalogService {

    private final Map<String, Course> courses = new LinkedHashMap<>();

    public CourseCatalogService() {
        seedCourses();
    }

    public List<Course> listCourses() {
        return new ArrayList<>(courses.values());
    }

    public Course getCourseById(String id) {
        return Optional.ofNullable(courses.get(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown course id: " + id));
    }

    private void seedCourses() {
        courses.put("llm-101", new Course(
                "llm-101",
                "Foundations of LLMs",
                "AI",
                "Beginner",
                6,
                "Overview of transformer models, tokenization, prompting, and safety basics."
        ));

        courses.put("spring-ai", new Course(
                "spring-ai",
                "Building AI Apps with Spring AI",
                "Backend",
                "Intermediate",
                8,
                "Hands-on guide to Spring AI clients, tools, memory, and evaluation patterns."
        ));

        courses.put("reactive-netty", new Course(
                "reactive-netty",
                "Reactive APIs with Netty",
                "Backend",
                "Intermediate",
                5,
                "Practical Reactor Netty patterns, back-pressure, and streaming over SSE."
        ));

        courses.put("agents-201", new Course(
                "agents-201",
                "Agentic Systems",
                "AI",
                "Advanced",
                7,
                "Designing tool-using agents, orchestrating MCP servers, and evaluation."
        ));
    }
}
