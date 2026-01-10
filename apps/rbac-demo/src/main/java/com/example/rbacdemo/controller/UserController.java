package com.example.rbacdemo.controller;

import com.example.rbacdemo.dto.UserInfo;
import com.example.rbacdemo.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final CurrentUserService currentUserService;

    public UserController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER', 'VIEWER')")
    public ResponseEntity<UserInfo> getCurrentUser() {
        return ResponseEntity.ok(currentUserService.getCurrentUser());
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<Map<String, Object>> getProfile() {
        UserInfo user = currentUserService.getCurrentUser();
        Map<String, Object> profile = new HashMap<>();
        profile.put("user", user);
        profile.put("accessLevel", determineAccessLevel(user));
        return ResponseEntity.ok(profile);
    }

    private String determineAccessLevel(UserInfo user) {
        if (user.getRoles().stream().anyMatch(r -> r.contains("ADMIN"))) {
            return "FULL_ACCESS";
        } else if (user.getRoles().stream().anyMatch(r -> r.contains("MANAGER"))) {
            return "ELEVATED_ACCESS";
        } else if (user.getRoles().stream().anyMatch(r -> r.contains("USER"))) {
            return "STANDARD_ACCESS";
        }
        return "READ_ONLY";
    }
}
