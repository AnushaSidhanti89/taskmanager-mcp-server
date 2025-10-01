package com.example.taskmanager.mcp;

import com.example.taskmanager.mcp.model.Task;
import com.example.taskmanager.mcp.repository.TaskRepository;
import com.example.taskmanager.mcp.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class McpControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService.deleteAllTasks();
    }

    @Test
    void testGetHelp() throws Exception {
        mockMvc.perform(get("/mcp/help"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.service").value("MCP Task Management Server"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.endpoints").exists())
                .andExpect(jsonPath("$.taskStatuses").isArray())
                .andExpect(jsonPath("$.taskPriorities").isArray());
    }

    @Test
    void testGetTaskSchema() throws Exception {
        mockMvc.perform(get("/mcp/schema/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("object"))
                .andExpect(jsonPath("$.title").value("Task"))
                .andExpect(jsonPath("$.properties").exists())
                .andExpect(jsonPath("$.properties.title").exists())
                .andExpect(jsonPath("$.properties.status").exists())
                .andExpect(jsonPath("$.properties.priority").exists())
                .andExpect(jsonPath("$.required").isArray())
                .andExpect(jsonPath("$.example").exists());
    }

    @Test
    void testInsertSingleTask() throws Exception {
        Task task = createSampleTask();
        String taskJson = objectMapper.writeValueAsString(List.of(task));

        mockMvc.perform(post("/mcp/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.insertedCount").value(1))
                .andExpect(jsonPath("$.firstTaskId").exists())
                .andExpect(jsonPath("$.lastTaskId").exists());
    }

    @Test
    void testInsertMultipleTasks() throws Exception {
        List<Task> tasks = List.of(
                createSampleTask("Task 1", Task.TaskStatus.TODO, Task.Priority.HIGH),
                createSampleTask("Task 2", Task.TaskStatus.IN_PROGRESS, Task.Priority.MEDIUM),
                createSampleTask("Task 3", Task.TaskStatus.DONE, Task.Priority.LOW)
        );

        String tasksJson = objectMapper.writeValueAsString(tasks);

        mockMvc.perform(post("/mcp/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tasksJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.insertedCount").value(3));
    }

    @Test
    void testInsertTaskWithInvalidData() throws Exception {
        Task invalidTask = new Task();
        // Missing required fields: title, status, priority
        String taskJson = objectMapper.writeValueAsString(List.of(invalidTask));

        mockMvc.perform(post("/mcp/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetTasksSummaryEmpty() throws Exception {
        mockMvc.perform(get("/mcp/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(0))
                .andExpect(jsonPath("$.statusDistribution").exists())
                .andExpect(jsonPath("$.priorityDistribution").exists())
                .andExpect(jsonPath("$.insights").exists());
    }

    @Test
    void testGetTasksSummaryWithData() throws Exception {
        // Insert test data using TaskService
        List<Task> tasks = List.of(
                createSampleTask("Task 1", Task.TaskStatus.TODO, Task.Priority.HIGH),
                createSampleTask("Task 2", Task.TaskStatus.DONE, Task.Priority.MEDIUM),
                createSampleTask("Task 3", Task.TaskStatus.IN_PROGRESS, Task.Priority.LOW)
        );
        taskService.saveBatch(tasks);

        mockMvc.perform(get("/mcp/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(3))
                .andExpect(jsonPath("$.statusDistribution.TODO").value(1))
                .andExpect(jsonPath("$.statusDistribution.DONE").value(1))
                .andExpect(jsonPath("$.statusDistribution.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.priorityDistribution.HIGH").value(1))
                .andExpect(jsonPath("$.priorityDistribution.MEDIUM").value(1))
                .andExpect(jsonPath("$.priorityDistribution.LOW").value(1))
                .andExpect(jsonPath("$.insights.completionRate").exists())
                .andExpect(jsonPath("$.insights.mostCommonStatus").exists());
    }

    @Test
    void testGetAllTasks() throws Exception {
        // Insert test data using TaskService
        List<Task> tasks = List.of(
                createSampleTask("Task 1", Task.TaskStatus.TODO, Task.Priority.HIGH),
                createSampleTask("Task 2", Task.TaskStatus.DONE, Task.Priority.MEDIUM)
        );
        taskService.saveBatch(tasks);

        mockMvc.perform(get("/mcp/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void testGetAllTasksWithPagination() throws Exception {
        // Insert test data using TaskService
        List<Task> tasks = List.of(
                createSampleTask("Task 1", Task.TaskStatus.TODO, Task.Priority.HIGH),
                createSampleTask("Task 2", Task.TaskStatus.DONE, Task.Priority.MEDIUM),
                createSampleTask("Task 3", Task.TaskStatus.IN_PROGRESS, Task.Priority.LOW)
        );
        taskService.saveBatch(tasks);

        mockMvc.perform(get("/mcp/tasks")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/mcp/help")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    private Task createSampleTask() {
        return createSampleTask("Sample Task", Task.TaskStatus.TODO, Task.Priority.MEDIUM);
    }

    private Task createSampleTask(String title, Task.TaskStatus status, Task.Priority priority) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription("Sample task description for testing");
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(LocalDateTime.now().plusDays(7));
        task.setAssignedTo("test.user@example.com");
        task.setTags("testing,automation,mcp");
        return task;
    }
}