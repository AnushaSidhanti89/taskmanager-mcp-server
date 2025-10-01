package com.example.taskmanager.mcp.controller;

import com.example.taskmanager.mcp.model.Task;
import com.example.taskmanager.mcp.service.McpService;
import com.example.taskmanager.mcp.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow AI agents from any origin
@Tag(name = "MCP Server", description = "Model Context Protocol endpoints for AI agent interaction")
public class McpController {

    private final McpService mcpService;
    private final TaskService taskService;

    /**
     * Returns the database schema for the tasks table as a simplified JSON-Schema
     */
    @GetMapping("/schema/tasks")
    @Operation(
            summary = "Get Task Database Schema",
            description = "Returns the database schema for the tasks table as a JSON Schema. AI agents use this to understand the structure of task data before generating and inserting records."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved task schema",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Task Schema Response",
                                    value = """
                                    {
                                      "$schema": "http://json-schema.org/draft-07/schema#",
                                      "type": "object",
                                      "title": "Task",
                                      "properties": {
                                        "title": {
                                          "type": "string",
                                          "maxLength": 255,
                                          "description": "Task title (required)"
                                        },
                                        "status": {
                                          "type": "string",
                                          "enum": ["TODO", "IN_PROGRESS", "DONE", "CANCELLED", "ON_HOLD"],
                                          "description": "Current status of the task (required)"
                                        },
                                        "priority": {
                                          "type": "string",\s
                                          "enum": ["LOW", "MEDIUM", "HIGH", "URGENT"],
                                          "description": "Task priority level (required)"
                                        }
                                      },
                                      "required": ["title", "status", "priority"]
                                    }
                                   \s"""
                            )
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> getTaskSchema() {
        log.info("AI Agent requested task schema");
        Map<String, Object> schema = mcpService.generateTaskSchema();
        return ResponseEntity.ok(schema);
    }

    /**
     * Accepts a JSON array of Task objects and inserts them into the DB
     */
    @PostMapping("/tasks")
    @Operation(
            summary = "Insert Tasks in Bulk",
            description = "Accepts a JSON array of Task objects and inserts them into the database. Optimized for AI agents to insert large datasets (e.g., 1000 tasks) efficiently."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks inserted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Successful Insertion",
                                    value = """
                                    {
                                      "success": true,
                                      "message": "Tasks inserted successfully",
                                      "insertedCount": 1000,
                                      "firstTaskId": 1,
                                      "lastTaskId": 1000
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid task data provided",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                    {
                                      "success": false,
                                      "message": "Error inserting tasks: Task title is required",
                                      "insertedCount": 0
                                    }
                                    """
                            )
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> insertTasks(
            @Valid @RequestBody
            @Parameter(
                    description = "Array of task objects to insert",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Sample Tasks",
                                    value = """
                                    [
                                      {
                                        "title": "Implement user authentication",
                                        "description": "Add JWT-based authentication system",
                                        "status": "TODO",
                                        "priority": "HIGH",
                                        "dueDate": "2025-01-20 17:00:00",
                                        "assignedTo": "john.doe@example.com",
                                        "tags": "security,authentication,backend"
                                      },
                                      {
                                        "title": "Design landing page",
                                        "description": "Create responsive landing page design",
                                        "status": "IN_PROGRESS",\s
                                        "priority": "MEDIUM",
                                        "dueDate": "2025-01-15 12:00:00",
                                        "assignedTo": "jane.smith@example.com",
                                        "tags": "frontend,design,ui"
                                      }
                                    ]
                                   \s"""
                            )
                    )
            )
            List<Task> tasks) {
        log.info("AI Agent requested insertion of {} tasks", tasks.size());

        try {
            List<Task> savedTasks = taskService.saveBatch(tasks);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tasks inserted successfully");
            response.put("insertedCount", savedTasks.size());
            if (!savedTasks.isEmpty()) {
                response.put("firstTaskId", savedTasks.get(0).getId());
                response.put("lastTaskId", savedTasks.get(savedTasks.size() - 1).getId());
            }


            log.info("Successfully inserted {} tasks", savedTasks.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error inserting tasks: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error inserting tasks: " + e.getMessage(),
                    "insertedCount", 0
            );

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Returns summary statistics (e.g., task counts per status)
     */
    @GetMapping("/tasks/summary")
    @Operation(
            summary = "Get Task Analytics Summary",
            description = "Returns comprehensive summary statistics including task counts by status, priority distribution, completion rates, and actionable insights."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved task summary",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Task Summary Response",
                            value = """
                            {
                              "totalTasks": 1000,
                              "overdueTasks": 45,
                              "statusDistribution": {
                                "TODO": 250,
                                "IN_PROGRESS": 200,
                                "DONE": 350,
                                "CANCELLED": 100,
                                "ON_HOLD": 100
                              },
                              "priorityDistribution": {
                                "LOW": 200,
                                "MEDIUM": 400,
                                "HIGH": 300,
                                "URGENT": 100
                              },
                              "statusPercentages": {
                                "TODO": 25.0,
                                "IN_PROGRESS": 20.0,
                                "DONE": 35.0
                              },
                              "insights": {
                                "completionRate": "35.0%",
                                "overdueRate": "4.5%",
                                "mostCommonStatus": "DONE",
                                "mostCommonPriority": "MEDIUM"
                              }
                            }
                            """
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> getTasksSummary() {
        log.info("AI Agent requested tasks summary");
        Map<String, Object> summary = mcpService.generateTaskSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Returns a short, agent-readable description of available endpoints
     */
    @GetMapping("/help")
    @Operation(
            summary = "Get MCP Server Help Information",
            description = "Returns comprehensive help information about available MCP endpoints, usage examples, and data formats for AI agents."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved help information"
    )
    public ResponseEntity<Map<String, Object>> getHelp() {
        log.info("AI Agent requested help information");

        Map<String, Object> helpInfo = Map.of(
                "service", "MCP Task Management Server",
                "version", "1.0.0",
                "description", "AI-powered data injection service for Task Management",
                "endpoints", Map.of(
                        "GET /mcp/schema/tasks", "Get the database schema for tasks table as JSON-Schema",
                        "POST /mcp/tasks", "Insert an array of Task objects into the database",
                        "GET /mcp/tasks/summary", "Get summary statistics of all tasks",
                        "GET /mcp/help", "Get this help information",
                        "GET /mcp/tasks", "Get all tasks with pagination"
                ),
                "taskStatuses", List.of("TODO", "IN_PROGRESS", "DONE", "CANCELLED", "ON_HOLD"),
                "taskPriorities", List.of("LOW", "MEDIUM", "HIGH", "URGENT"),
                "exampleUsage", Map.of(
                        "step1", "GET /mcp/schema/tasks - Inspect the schema",
                        "step2", "POST /mcp/tasks - Insert task data as JSON array",
                        "step3", "GET /mcp/tasks/summary - Verify insertion results"
                ),
                "notes", List.of(
                        "All timestamps should be in 'yyyy-MM-dd HH:mm:ss' format",
                        "Tags should be comma-separated strings",
                        "Created and updated timestamps are auto-generated",
                        "ID field is auto-generated and should be omitted in POST requests"
                )
        );

        return ResponseEntity.ok(helpInfo);
    }

    /**
     * Additional endpoint to get all tasks (useful for AI agent verification)
     */
    @GetMapping("/tasks")
    @Operation(
            summary = "Get All Tasks",
            description = "Retrieves all tasks with simple pagination. Useful for AI agents to verify inserted data or analyze existing tasks."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved tasks",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Tasks Response",
                                    value = """
                                    {
                                      "tasks": [
                                        {
                                          "id": 1,
                                          "title": "Sample Task",
                                          "status": "TODO",
                                          "priority": "HIGH",
                                          "createdAt": "2025-01-15 10:00:00"
                                        }
                                      ],
                                      "totalCount": 1000,
                                      "page": 0,
                                      "size": 100,
                                      "hasMore": true
                                    }
                                    """
                            )
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> getAllTasks(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "100")
            @RequestParam(defaultValue = "100") int size) {
        log.info("AI Agent requested all tasks (page={}, size={})", page, size);

        try {
            Map<String, Object> response = getStringObjectMap(page, size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving tasks: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error retrieving tasks: " + e.getMessage()
            ));
        }
    }

    private Map<String, Object> getStringObjectMap(int page, int size) {
        List<Task> tasks = taskService.getAllTasks();
        int totalTasks = tasks.size();

        // Simple pagination
        int start = Math.min(page * size, totalTasks);
        int end = Math.min(start + size, totalTasks);
        List<Task> paginatedTasks = tasks.subList(start, end);

        Map<String, Object> response = Map.of(
                "tasks", paginatedTasks,
                "totalCount", totalTasks,
                "page", page,
                "size", size,
                "hasMore", end < totalTasks
        );
        return response;
    }
}
