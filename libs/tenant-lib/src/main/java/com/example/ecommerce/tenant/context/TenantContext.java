package com.example.ecommerce.tenant.context;

/**
 * 租戶上下文 - 使用 ThreadLocal 存儲當前請求的租戶 ID
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // 不允許實例化
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * 檢查是否為系統級租戶（可存取所有資料）
     */
    public static boolean isSystemTenant() {
        String tenant = getCurrentTenant();
        return "system".equals(tenant);
    }
}
