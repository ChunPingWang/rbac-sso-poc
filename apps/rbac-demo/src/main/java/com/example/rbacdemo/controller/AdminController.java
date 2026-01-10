package com.example.rbacdemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalUsers", 150);
        dashboard.put("activeUsers", 42);
        dashboard.put("totalRoles", 4);
        dashboard.put("systemStatus", "HEALTHY");
        dashboard.put("lastUpdated", LocalDateTime.now());
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listAllUsers() {
        List<Map<String, Object>> users = List.of(
            Map.of("id", 1, "username", "admin", "role", "ADMIN", "status", "ACTIVE"),
            Map.of("id", 2, "username", "manager", "role", "MANAGER", "status", "ACTIVE"),
            Map.of("id", 3, "username", "user", "role", "USER", "status", "ACTIVE"),
            Map.of("id", 4, "username", "viewer", "role", "VIEWER", "status", "ACTIVE")
        );
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, Object>> changeUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        String newRole = request.get("role");
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("newRole", newRole);
        result.put("success", true);
        result.put("message", "Role updated successfully");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("deleted", true);
        result.put("message", "User deleted successfully");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs() {
        List<Map<String, Object>> logs = List.of(
            Map.of("id", 1, "action", "LOGIN", "user", "admin", "timestamp", LocalDateTime.now().minusHours(1)),
            Map.of("id", 2, "action", "ROLE_CHANGE", "user", "admin", "timestamp", LocalDateTime.now().minusMinutes(30)),
            Map.of("id", 3, "action", "USER_CREATE", "user", "admin", "timestamp", LocalDateTime.now())
        );
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/system/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("database", "UP");
        health.put("keycloak", "UP");
        health.put("ldap", "UP");
        health.put("memory", Map.of("used", "512MB", "total", "2048MB"));
        health.put("uptime", "24h 35m");
        return ResponseEntity.ok(health);
    }
}
