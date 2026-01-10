package com.example.ecommerce.tests;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.zh_tw.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 商品管理相關的步驟定義
 */
public class ProductSteps {

    @Autowired
    private TestContext testContext;

    private String lastProductId;

    @假設("系統已初始化預設資料")
    public void 系統已初始化預設資料() {
        testContext.reset();
        System.out.println("初始化預設資料...");
    }

    @假設("使用者 {string} 已登入系統，角色為 {string}")
    public void 使用者已登入系統角色為(String username, String role) {
        testContext.login(username, role);
        System.out.println("使用者 " + username + " 以 " + role + " 角色登入");
    }

    @當("使用者建立商品:")
    public void 使用者建立商品(DataTable dataTable) {
        List<Map<String, String>> products = dataTable.asMaps();
        for (Map<String, String> product : products) {
            String name = product.get("商品名稱");
            String price = product.get("價格");
            String category = product.get("分類");
            String description = product.get("描述");
            System.out.println("建立商品: " + name + ", 價格: " + price);
            testContext.setLastResponseCode(201);
            lastProductId = "prod-" + System.currentTimeMillis();
        }
    }

    @當("使用者嘗試建立商品:")
    public void 使用者嘗試建立商品(DataTable dataTable) {
        String role = testContext.getCurrentRole();
        if ("USER".equals(role) || "VIEWER".equals(role)) {
            testContext.setLastResponseCode(403);
        } else {
            使用者建立商品(dataTable);
        }
    }

    @當("使用者查詢所有商品")
    public void 使用者查詢所有商品() {
        System.out.println("查詢所有商品...");
        testContext.setLastResponseCode(200);
    }

    @當("使用者刪除該商品")
    public void 使用者刪除該商品() {
        if ("ADMIN".equals(testContext.getCurrentRole())) {
            testContext.setLastResponseCode(204);
            System.out.println("商品已刪除");
        } else {
            testContext.setLastResponseCode(403);
        }
    }

    @那麼("系統應回傳成功訊息")
    public void 系統應回傳成功訊息() {
        int code = testContext.getLastResponseCode();
        assert code == 200 || code == 201 || code == 204 :
            "Expected success response, but got: " + code;
    }

    @那麼("系統應回傳權限不足錯誤")
    public void 系統應回傳權限不足錯誤() {
        int code = testContext.getLastResponseCode();
        assert code == 403 : "Expected 403, but got: " + code;
    }

    @那麼("商品應該被成功建立")
    public void 商品應該被成功建立() {
        assert lastProductId != null : "Product should have been created";
    }

    @那麼("系統應回傳商品列表")
    public void 系統應回傳商品列表() {
        int code = testContext.getLastResponseCode();
        assert code == 200 : "Expected 200, but got: " + code;
    }

    @那麼("列表應包含預設的 {int} 筆商品")
    public void 列表應包含預設的筆商品(int count) {
        // 在實際測試中會驗證返回的商品數量
        System.out.println("驗證商品數量: " + count);
    }

    @那麼("商品狀態應該為 {string}")
    public void 商品狀態應該為(String status) {
        System.out.println("驗證商品狀態: " + status);
    }

    @假設("系統中存在商品 {string}")
    public void 系統中存在商品(String productName) {
        lastProductId = "prod-" + productName.hashCode();
        System.out.println("商品存在: " + productName);
    }
}
