package com.example.taskmanager.mcp.repository;

import com.example.taskmanager.mcp.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Count tasks by status
    long countByStatus(Task.TaskStatus status);

    // Count tasks by priority
    long countByPriority(Task.Priority priority);

    // Custom query to get status distribution
    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> getStatusDistribution();

    // Custom query to get priority distribution
    @Query("SELECT t.priority, COUNT(t) FROM Task t GROUP BY t.priority")
    List<Object[]> getPriorityDistribution();

    // Count overdue tasks
    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate < CURRENT_TIMESTAMP AND t.status != 'DONE' AND t.status != 'CANCELLED'")
    long countOverdueTasks();

    // Find tasks by assigned user
    List<Task> findByAssignedToIgnoreCase(String assignedTo);

    // Find tasks containing specific tag
    @Query("SELECT t FROM Task t WHERE t.tags LIKE %:tag%")
    List<Task> findByTagContaining(String tag);
}