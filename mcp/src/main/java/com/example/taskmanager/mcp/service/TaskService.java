package com.example.taskmanager.mcp.service;

import com.example.taskmanager.mcp.model.Task;
import com.example.taskmanager.mcp.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    /**
     * Get priority distribution as a map
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getPriorityDistribution() {
        List<Object[]> priorityDistribution = taskRepository.getPriorityDistribution();
        return priorityDistribution.stream()
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(),
                        arr -> ((Number) arr[1]).longValue()
                ));
    }

    /**
     * Get status percentages
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getStatusPercentages() {
        long totalTasks = getTotalTaskCount();
        if (totalTasks == 0) return Map.of();

        Map<String, Long> statusCounts = getStatusDistribution();
        return statusCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Math.round((entry.getValue() * 100.0 / totalTasks) * 100.0) / 100.0
                ));
    }

    /**
     * Get priority percentages
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getPriorityPercentages() {
        long totalTasks = getTotalTaskCount();
        if (totalTasks == 0) return Map.of();

        Map<String, Long> priorityCounts = getPriorityDistribution();
        return priorityCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Math.round((entry.getValue() * 100.0 / totalTasks) * 100.0) / 100.0
                ));
    }

    /**
     * Generate task insights and analytics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateTaskInsights() {
        long totalTasks = getTotalTaskCount();
        long overdueTasks = getOverdueTasksCount();
        Map<String, Long> statusCounts = getStatusDistribution();
        Map<String, Long> priorityCounts = getPriorityDistribution();

        Map<String, Object> insights = new HashMap<>();

        if (totalTasks == 0) {
            insights.put("message", "No tasks in the database");
            return insights;
        }

        // Completion rate
        long completedTasks = statusCounts.getOrDefault("DONE", 0L);
        double completionRate = Math.round((completedTasks * 100.0 / totalTasks) * 100.0) / 100.0;
        insights.put("completionRate", completionRate + "%");

        // Overdue rate
        double overdueRate = Math.round((overdueTasks * 100.0 / totalTasks) * 100.0) / 100.0;
        insights.put("overdueRate", overdueRate + "%");

        // Most common status
        String mostCommonStatus = statusCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        insights.put("mostCommonStatus", mostCommonStatus);

        // Most common priority
        String mostCommonPriority = priorityCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        insights.put("mostCommonPriority", mostCommonPriority);

        // Health indicators
        insights.put("healthIndicators", Map.of(
                "lowCompletionRate", completionRate < 30,
                "highOverdueRate", overdueRate > 20,
                "tooManyInProgress", statusCounts.getOrDefault("IN_PROGRESS", 0L) > totalTasks * 0.5
        ));

        return insights;
    }

    /**
     * Get total count of tasks
     */
    @Transactional(readOnly = true)
    public long getTotalTaskCount() {
        return taskRepository.count();
    }

    /**
     * Get status distribution as a map
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatusDistribution() {
        List<Object[]> statusDistribution = taskRepository.getStatusDistribution();
        return statusDistribution.stream()
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(),
                        arr -> ((Number) arr[1]).longValue()
                ));
    }


    /**
     * Save a single task
     */
    public Task save(Task task) {
        log.debug("Saving task: {}", task.getTitle());
        return taskRepository.save(task);
    }

    /**
     * Save multiple tasks in batch
     * This method is optimized for bulk inserts from AI agents
     */
    public List<Task> saveBatch(List<Task> tasks) {
        log.info("Saving batch of {} tasks", tasks.size());

        // Clear IDs to ensure new records are created
        tasks.forEach(task -> task.setId(null));

        // Validate required fields
        validateTasks(tasks);

        try {
            List<Task> savedTasks = taskRepository.saveAll(tasks);
            log.info("Successfully saved {} tasks", savedTasks.size());
            return savedTasks;
        } catch (Exception e) {
            log.error("Error saving batch tasks: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save tasks batch: " + e.getMessage(), e);
        }
    }

    /**
     * Get all tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        log.debug("Retrieving all tasks");
        return taskRepository.findAll();
    }

    /**
     * Get task by ID
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(Long id) {
        log.debug("Retrieving task with ID: {}", id);
        return taskRepository.findById(id);
    }

    /**
     * Delete task by ID
     */
    public void deleteTask(Long id) {
        log.debug("Deleting task with ID: {}", id);
        taskRepository.deleteById(id);
    }

    /**
     * Get tasks by status
     */
    @Transactional(readOnly = true)
    public long countByStatus(Task.TaskStatus status) {
        return taskRepository.countByStatus(status);
    }

    /**
     * Get tasks by priority
     */
    @Transactional(readOnly = true)
    public long countByPriority(Task.Priority priority) {
        return taskRepository.countByPriority(priority);
    }

    /**
     * Get tasks assigned to a specific person
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByAssignee(String assignedTo) {
        log.debug("Retrieving tasks assigned to: {}", assignedTo);
        return taskRepository.findByAssignedToIgnoreCase(assignedTo);
    }

    /**
     * Get tasks containing a specific tag
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByTag(String tag) {
        log.debug("Retrieving tasks with tag: {}", tag);
        return taskRepository.findByTagContaining(tag);
    }

    /**
     * Get count of overdue tasks
     */
    @Transactional(readOnly = true)
    public long getOverdueTasksCount() {
        return taskRepository.countOverdueTasks();
    }

    /**
     * Delete all tasks (useful for testing)
     */
    public void deleteAllTasks() {
        log.warn("Deleting all tasks");
        taskRepository.deleteAll();
    }

    /**
     * Validate tasks before saving
     */
    private void validateTasks(List<Task> tasks) {
        for (Task task : tasks) {
            if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Task title is required");
            }
            if (task.getStatus() == null) {
                throw new IllegalArgumentException("Task status is required");
            }
            if (task.getPriority() == null) {
                throw new IllegalArgumentException("Task priority is required");
            }

            // Validate enum values
            try {
                Task.TaskStatus.valueOf(task.getStatus().toString());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid task status: " + task.getStatus());
            }

            try {
                Task.Priority.valueOf(task.getPriority().toString());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid task priority: " + task.getPriority());
            }
        }
    }
}