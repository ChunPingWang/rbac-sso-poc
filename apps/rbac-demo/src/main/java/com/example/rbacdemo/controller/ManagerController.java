package com.example.rbacdemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ManagerController {

    @GetMapping("/team")
    public ResponseEntity<List<Map<String, Object>>> getTeamMembers() {
        List<Map<String, Object>> team = List.of(
            Map.of("id", 1, "name", "John Doe", "role", "Developer", "status", "ACTIVE"),
            Map.of("id", 2, "name", "Jane Smith", "role", "Designer", "status", "ACTIVE"),
            Map.of("id", 3, "name", "Bob Johnson", "role", "QA Engineer", "status", "ON_LEAVE")
        );
        return ResponseEntity.ok(team);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> getReports() {
        List<Map<String, Object>> reports = List.of(
            Map.of("id", 1, "title", "Q4 Performance Report", "status", "COMPLETED", "date", LocalDateTime.now().minusDays(7)),
            Map.of("id", 2, "title", "Sprint Review", "status", "IN_PROGRESS", "date", LocalDateTime.now()),
            Map.of("id", 3, "title", "Resource Allocation", "status", "PENDING", "date", LocalDateTime.now().plusDays(7))
        );
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/reports")
    public ResponseEntity<Map<String, Object>> createReport(@RequestBody Map<String, Object> report) {
        Map<String, Object> result = new HashMap<>(report);
        result.put("id", System.currentTimeMillis());
        result.put("status", "CREATED");
        result.put("createdAt", LocalDateTime.now());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getTeamMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("teamSize", 10);
        metrics.put("tasksCompleted", 45);
        metrics.put("tasksInProgress", 12);
        metrics.put("averageVelocity", 28);
        metrics.put("sprintProgress", 65);
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/team/{memberId}/assign")
    public ResponseEntity<Map<String, Object>> assignTask(
            @PathVariable Long memberId,
            @RequestBody Map<String, Object> task) {
        Map<String, Object> result = new HashMap<>();
        result.put("memberId", memberId);
        result.put("taskId", task.get("taskId"));
        result.put("assigned", true);
        result.put("assignedAt", LocalDateTime.now());
        return ResponseEntity.ok(result);
    }
}
