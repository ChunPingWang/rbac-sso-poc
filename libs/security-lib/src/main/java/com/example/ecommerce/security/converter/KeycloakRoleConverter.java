package com.example.ecommerce.security.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 從 Keycloak JWT Token 提取角色並轉換為 Spring Security GrantedAuthority
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 從 realm_access 提取 realm roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            authorities.addAll(
                roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toSet())
            );
        }

        // 從 realm_roles claim 提取 (自訂 mapper)
        List<String> realmRoles = jwt.getClaimAsStringList("realm_roles");
        if (realmRoles != null) {
            authorities.addAll(
                realmRoles.stream()
                    .map(role -> {
                        String normalized = role.startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase();
                        return new SimpleGrantedAuthority(normalized);
                    })
                    .collect(Collectors.toSet())
            );
        }

        return authorities;
    }
}
