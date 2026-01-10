package com.example.rbacdemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final Map<Long, Map<String, Object>> resources = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public ResourceController() {
        // Initialize with sample data
        createSampleResource("Document A", "PUBLIC");
        createSampleResource("Document B", "INTERNAL");
        createSampleResource("Confidential Report", "CONFIDENTIAL");
    }

    private void createSampleResource(String name, String classification) {
        long id = idGenerator.getAndIncrement();
        Map<String, Object> resource = new HashMap<>();
        resource.put("id", id);
        resource.put("name", name);
        resource.put("classification", classification);
        resource.put("createdAt", LocalDateTime.now());
        resources.put(id, resource);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<List<Map<String, Object>>> listResources() {
        return ResponseEntity.ok(new ArrayList<>(resources.values()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getResource(@PathVariable Long id) {
        Map<String, Object> resource = resources.get(id);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resource);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<Map<String, Object>> createResource(@RequestBody Map<String, Object> request) {
        long id = idGenerator.getAndIncrement();
        Map<String, Object> resource = new HashMap<>(request);
        resource.put("id", id);
        resource.put("createdAt", LocalDateTime.now());
        resources.put(id, resource);
        return ResponseEntity.ok(resource);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> updateResource(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> existing = resources.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        existing.putAll(request);
        existing.put("id", id);
        existing.put("updatedAt", LocalDateTime.now());
        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteResource(@PathVariable Long id) {
        Map<String, Object> removed = resources.remove(id);
        if (removed == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("deleted", true);
        result.put("deletedAt", LocalDateTime.now());
        return ResponseEntity.ok(result);
    }
}
