package com.example.ecommerce.tests;

import io.cucumber.java.zh_tw.*;

/**
 * RBAC 權限控制相關的步驟定義
 */
public class RbacSteps {

    private String currentUser;
    private String currentRole;
    private int lastResponseCode;
    private boolean isAuthenticated = false;
    private boolean hasValidToken = true;

    public void 使用者已登入系統角色為(String username, String role) {
        this.currentUser = username;
        this.currentRole = role;
        this.isAuthenticated = true;
        this.hasValidToken = true;
    }

    @假設("使用者未登入")
    public void 使用者未登入() {
        this.currentUser = null;
        this.currentRole = null;
        this.isAuthenticated = false;
    }

    @假設("使用者持有無效的 JWT Token")
    public void 使用者持有無效的Token() {
        this.hasValidToken = false;
    }

    @當("使用者嘗試存取 {string}")
    public void 使用者嘗試存取(String endpoint) {
        if (!isAuthenticated || !hasValidToken) {
            lastResponseCode = 401;
            return;
        }

        // 模擬權限檢查
        if (endpoint.contains("/admin/") && !"ADMIN".equals(currentRole)) {
            lastResponseCode = 403;
        } else if (endpoint.contains("/new") && ("USER".equals(currentRole) || "VIEWER".equals(currentRole))) {
            lastResponseCode = 403;
        } else {
            lastResponseCode = 200;
        }
    }

    @那麼("系統應回傳 {string}")
    public void 系統應回傳(String expectedCode) {
        int expected = Integer.parseInt(expectedCode);
        assert lastResponseCode == expected :
            "Expected " + expected + ", but got: " + lastResponseCode;
    }
}
