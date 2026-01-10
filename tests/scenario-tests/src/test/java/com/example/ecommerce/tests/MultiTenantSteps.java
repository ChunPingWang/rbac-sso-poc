package com.example.ecommerce.tests;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_tw.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 多租戶相關的步驟定義
 */
public class MultiTenantSteps {

    @Autowired
    private TestContext testContext;

    private Map<String, List<String>> tenantProducts = new HashMap<>();
    private List<String> lastQueryResults = new ArrayList<>();

    @假設("系統中存在兩個租戶:")
    public void 系統中存在兩個租戶(DataTable dataTable) {
        tenantProducts.clear();
        List<Map<String, String>> tenants = dataTable.asMaps();
        for (Map<String, String> tenant : tenants) {
            String tenantId = tenant.get("租戶 ID");
            tenantProducts.put(tenantId, new ArrayList<>());
            System.out.println("初始化租戶: " + tenantId);
        }
    }

    @假設("租戶 {string} 有商品 {string} 和 {string}")
    public void 租戶有兩個商品(String tenantId, String product1, String product2) {
        tenantProducts.computeIfAbsent(tenantId, k -> new ArrayList<>());
        tenantProducts.get(tenantId).add(product1);
        tenantProducts.get(tenantId).add(product2);
    }

    @假設("租戶 {string} 有商品 {string}")
    public void 租戶有商品(String tenantId, String product) {
        tenantProducts.computeIfAbsent(tenantId, k -> new ArrayList<>());
        tenantProducts.get(tenantId).add(product);
    }

    @假設("租戶 {string} 有商品 {string}，ID 為 {string}")
    public void 租戶有商品ID為(String tenantId, String product, String productId) {
        tenantProducts.computeIfAbsent(tenantId, k -> new ArrayList<>());
        tenantProducts.get(tenantId).add(product + ":" + productId);
    }

    @當("租戶 {string} 的使用者查詢商品列表")
    public void 租戶使用者查詢商品列表(String tenantId) {
        testContext.setCurrentTenant(tenantId);
        lastQueryResults = new ArrayList<>(tenantProducts.getOrDefault(tenantId, Collections.emptyList()));
        testContext.setLastResponseCode(200);
    }

    @當("系統管理員查詢所有商品")
    public void 系統管理員查詢所有商品() {
        testContext.setCurrentTenant("system");
        lastQueryResults = new ArrayList<>();
        for (List<String> products : tenantProducts.values()) {
            lastQueryResults.addAll(products);
        }
        testContext.setLastResponseCode(200);
    }

    @當("租戶 {string} 的使用者嘗試存取商品 {string}")
    public void 租戶使用者嘗試存取商品(String tenantId, String productId) {
        testContext.setCurrentTenant(tenantId);
        boolean found = false;
        for (String product : tenantProducts.getOrDefault(tenantId, Collections.emptyList())) {
            if (product.contains(productId)) {
                found = true;
                break;
            }
        }
        testContext.setLastResponseCode(found ? 200 : 404);
    }

    @那麼("只應看到屬於 {string} 的商品")
    public void 只應看到屬於的商品(String tenantId) {
        List<String> expectedProducts = tenantProducts.getOrDefault(tenantId, Collections.emptyList());
        assert lastQueryResults.containsAll(expectedProducts) :
            "Expected to see tenant's products";
    }

    @那麼("不應看到 {string} 的商品")
    public void 不應看到的商品(String tenantId) {
        List<String> otherProducts = tenantProducts.getOrDefault(tenantId, Collections.emptyList());
        for (String product : otherProducts) {
            assert !lastQueryResults.contains(product) :
                "Should not see other tenant's products";
        }
    }

    @那麼("應看到所有租戶的商品")
    public void 應看到所有租戶的商品() {
        int totalProducts = tenantProducts.values().stream()
            .mapToInt(List::size)
            .sum();
        assert lastQueryResults.size() == totalProducts :
            "Expected to see all products";
    }

    @那麼("系統應回傳資源不存在錯誤")
    public void 系統應回傳資源不存在錯誤() {
        int code = testContext.getLastResponseCode();
        assert code == 404 : "Expected 404, but got: " + code;
    }
}
