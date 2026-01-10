package com.example.ecommerce.user.adapter.inbound.rest;

import com.example.ecommerce.common.dto.ApiResponse;
import com.example.ecommerce.user.application.dto.UserProfileView;
import com.example.ecommerce.user.application.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "使用者相關 API")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "取得當前使用者資訊", description = "傳回已登入使用者的個人資料")
    public ApiResponse<UserProfileView> getCurrentUser() {
        UserProfileView profile = userProfileService.getCurrentUserProfile();
        return ApiResponse.success(profile);
    }
}
