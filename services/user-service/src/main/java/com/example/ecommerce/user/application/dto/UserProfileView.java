package com.example.ecommerce.user.application.dto;

import java.util.List;

public record UserProfileView(
    String username,
    String email,
    String firstName,
    String lastName,
    String tenantId,
    List<String> roles,
    List<String> groups
) {}
