package com.example.ecommerce.user.application.service;

import com.example.ecommerce.user.application.dto.UserProfileView;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    public UserProfileView getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        String username = auth.getName();
        String email = null;
        String firstName = null;
        String lastName = null;
        String tenantId = "default";
        List<String> groups = Collections.emptyList();

        // Extract info from JWT if available
        if (auth.getPrincipal() instanceof Jwt jwt) {
            email = jwt.getClaimAsString("email");
            firstName = jwt.getClaimAsString("given_name");
            lastName = jwt.getClaimAsString("family_name");
            tenantId = jwt.getClaimAsString("tenant_id");
            if (tenantId == null) {
                tenantId = "default";
            }

            // Extract groups from JWT
            List<String> jwtGroups = jwt.getClaimAsStringList("groups");
            if (jwtGroups != null) {
                groups = jwtGroups;
            }
        }

        List<String> roles = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        return new UserProfileView(
            username,
            email,
            firstName,
            lastName,
            tenantId,
            roles,
            groups
        );
    }
}
