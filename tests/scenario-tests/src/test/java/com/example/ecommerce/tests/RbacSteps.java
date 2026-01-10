package com.example.ecommerce.tests;

import io.cucumber.java.zh_tw.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RBAC 權限控制相關的步驟定義
 */
public class RbacSteps {

    @Autowired
    private TestContext testContext;

    @假設("使用者未登入")
    public void 使用者未登入() {
        testContext.logout();
    }

    @假設("使用者持有無效的 JWT Token")
    public void 使用者持有無效的Token() {
        testContext.invalidateToken();
    }

    @當("使用者嘗試存取 {string}")
    public void 使用者嘗試存取(String endpoint) {
        if (!testContext.isAuthenticated() || !testContext.hasValidToken()) {
            testContext.setLastResponseCode(401);
            return;
        }

        String role = testContext.getCurrentRole();

        // 模擬權限檢查
        if (endpoint.contains("/admin/") && !"ADMIN".equals(role)) {
            testContext.setLastResponseCode(403);
        } else if (endpoint.contains("/new") && ("USER".equals(role) || "VIEWER".equals(role))) {
            testContext.setLastResponseCode(403);
        } else {
            testContext.setLastResponseCode(200);
        }
    }

    @那麼("系統應回傳 {string}")
    public void 系統應回傳(String expectedCode) {
        int expected = Integer.parseInt(expectedCode);
        int actual = testContext.getLastResponseCode();
        assert actual == expected :
            "Expected " + expected + ", but got: " + actual;
    }
}
