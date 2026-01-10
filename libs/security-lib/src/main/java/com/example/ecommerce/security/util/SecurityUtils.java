package com.example.ecommerce.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Security 工具類
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // 不允許實例化
    }

    /**
     * 取得當前使用者名稱
     */
    public static Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    /**
     * 取得當前 JWT Token
     */
    public static Optional<Jwt> getCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    /**
     * 從 JWT Token 取得租戶 ID
     */
    public static Optional<String> getCurrentTenantId() {
        return getCurrentJwt()
            .map(jwt -> jwt.getClaimAsString("tenant_id"));
    }

    /**
     * 檢查當前使用者是否有指定角色
     */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equalsIgnoreCase(roleWithPrefix));
    }

    /**
     * 檢查當前使用者是否為系統管理員
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
