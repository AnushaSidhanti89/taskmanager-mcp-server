package com.example.taskmanager.mcp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class McpService {

    private final TaskService taskService;

    /**
     * Generates a JSON Schema representation of the Task entity
     * for AI agents to understand the database structure
     */
    public Map<String, Object> generateTaskSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");
        schema.put("title", "Task");
        schema.put("description", "Task Management Database Schema");

        // Define properties
        Map<String, Object> properties = new HashMap<>();

        // ID field
        properties.put("id", Map.of(
                "type", "integer",
                "description", "Unique identifier (auto-generated, omit in POST requests)",
                "readOnly", true
        ));

        // Title field
        properties.put("title", Map.of(
                "type", "string",
                "maxLength", 255,
                "description", "Task title (required)",
                "example", "Complete project documentation"
        ));

        // Description field
        properties.put("description", Map.of(
                "type", "string",
                "maxLength", 1000,
                "description", "Detailed task description (optional)",
                "example", "Write comprehensive documentation for the new feature"
        ));

        // Status field
        properties.put("status", Map.of(
                "type", "string",
                "enum", List.of("TODO", "IN_PROGRESS", "DONE", "CANCELLED", "ON_HOLD"),
                "description", "Current status of the task (required)",
                "example", "TODO"
        ));

        // Priority field
        properties.put("priority", Map.of(
                "type", "string",
                "enum", List.of("LOW", "MEDIUM", "HIGH", "URGENT"),
                "description", "Task priority level (required)",
                "example", "MEDIUM"
        ));

        // Due date field
        properties.put("dueDate", Map.of(
                "type", "string",
                "format", "date-time",
                "pattern", "yyyy-MM-dd HH:mm:ss",
                "description", "Task due date and time (optional)",
                "example", "2025-01-15 14:30:00"
        ));

        // Assigned to field
        properties.put("assignedTo", Map.of(
                "type", "string",
                "maxLength", 100,
                "description", "Person assigned to the task (optional)",
                "example", "john.doe@example.com"
        ));

        // Tags field
        properties.put("tags", Map.of(
                "type", "string",
                "maxLength", 500,
                "description", "Comma-separated tags (optional)",
                "example", "backend,database,urgent"
        ));

        // Created at (read-only)
        properties.put("createdAt", Map.of(
                "type", "string",
                "format", "date-time",
                "description", "Creation timestamp (auto-generated)",
                "readOnly", true
        ));

        // Updated at (read-only)
        properties.put("updatedAt", Map.of(
                "type", "string",
                "format", "date-time",
                "description", "Last update timestamp (auto-generated)",
                "readOnly", true
        ));

        schema.put("properties", properties);
        schema.put("required", List.of("title", "status", "priority"));

        // Add example task
        schema.put("example", Map.of(
                "title", "Implement user authentication",
                "description", "Add JWT-based authentication system with login and registration",
                "status", "TODO",
                "priority", "HIGH",
                "dueDate", "2025-01-20 17:00:00",
                "assignedTo", "jane.smith@example.com",
                "tags", "security,authentication,backend"
        ));

        return schema;
    }

    /**
     * Generates comprehensive summary statistics about tasks in the database
     * Delegates all business logic to TaskService
     */
    public Map<String, Object> generateTaskSummary() {
        // Get all data from TaskService
        long totalTasks = taskService.getTotalTaskCount();
        long overdueTasks = taskService.getOverdueTasksCount();
        Map<String, Long> statusDistribution = taskService.getStatusDistribution();
        Map<String, Long> priorityDistribution = taskService.getPriorityDistribution();

        // Build comprehensive summary
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTasks", totalTasks);
        summary.put("overdueTasks", overdueTasks);
        summary.put("statusDistribution", statusDistribution);
        summary.put("priorityDistribution", priorityDistribution);

        // Calculate percentages using TaskService
        if (totalTasks > 0) {
            summary.put("statusPercentages", taskService.getStatusPercentages());
            summary.put("priorityPercentages", taskService.getPriorityPercentages());
        }

        // Generate insights using TaskService
        summary.put("insights", taskService.generateTaskInsights());

        return summary;
    }
}