package com.example.ecommerce.tenant.filter;

import com.example.ecommerce.tenant.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 租戶過濾器 - 從 JWT Token 中提取租戶 ID 並設置到 TenantContext
 */
@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String tenantId = extractTenantFromToken();
            TenantContext.setCurrentTenant(tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTenantFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // 嘗試從 JWT claims 中提取 tenant_id
            String tenantId = jwt.getClaimAsString("tenant_id");
            if (tenantId != null && !tenantId.isBlank()) {
                return tenantId;
            }

            // 如果有 ADMIN 角色，返回 system（可存取所有租戶）
            if (hasRole(jwt, "ADMIN")) {
                return "system";
            }

            // 從 groups claim 中提取租戶（格式：/tenantX）
            List<String> groups = jwt.getClaimAsStringList("groups");
            if (groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    if (group.startsWith("/tenant")) {
                        return group.substring(1); // 移除前導斜線
                    }
                }
            }
        }

        return "unknown";
    }

    private boolean hasRole(Jwt jwt, String role) {
        // 從 realm_access.roles 中檢查
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                return roles.stream().anyMatch(r ->
                    r.equalsIgnoreCase(role) || r.equalsIgnoreCase("ROLE_" + role)
                );
            }
        }

        // 從 realm_roles claim 中檢查（自訂 mapper）
        List<String> realmRoles = jwt.getClaimAsStringList("realm_roles");
        if (realmRoles != null) {
            return realmRoles.stream().anyMatch(r ->
                r.equalsIgnoreCase(role) || r.equalsIgnoreCase("ROLE_" + role)
            );
        }

        return false;
    }
}
