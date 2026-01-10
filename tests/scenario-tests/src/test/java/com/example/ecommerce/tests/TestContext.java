package com.example.ecommerce.tests;

import org.springframework.stereotype.Component;

/**
 * 共用測試上下文，用於在不同 Step 類別間共享狀態
 */
@Component
public class TestContext {

    private String currentUser;
    private String currentRole;
    private String currentTenant;
    private int lastResponseCode;
    private boolean isAuthenticated = false;
    private boolean hasValidToken = true;
    private String lastResponseMessage;
    private Object lastResponseData;

    public void reset() {
        this.currentUser = null;
        this.currentRole = null;
        this.currentTenant = null;
        this.lastResponseCode = 0;
        this.isAuthenticated = false;
        this.hasValidToken = true;
        this.lastResponseMessage = null;
        this.lastResponseData = null;
    }

    public void login(String username, String role) {
        this.currentUser = username;
        this.currentRole = role;
        this.isAuthenticated = true;
        this.hasValidToken = true;
    }

    public void login(String username, String role, String tenant) {
        login(username, role);
        this.currentTenant = tenant;
    }

    public void logout() {
        this.currentUser = null;
        this.currentRole = null;
        this.isAuthenticated = false;
    }

    public void invalidateToken() {
        this.hasValidToken = false;
    }

    // Getters and Setters
    public String getCurrentUser() {
        return currentUser;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public String getCurrentTenant() {
        return currentTenant;
    }

    public void setCurrentTenant(String tenant) {
        this.currentTenant = tenant;
    }

    public int getLastResponseCode() {
        return lastResponseCode;
    }

    public void setLastResponseCode(int code) {
        this.lastResponseCode = code;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public boolean hasValidToken() {
        return hasValidToken;
    }

    public String getLastResponseMessage() {
        return lastResponseMessage;
    }

    public void setLastResponseMessage(String message) {
        this.lastResponseMessage = message;
    }

    public Object getLastResponseData() {
        return lastResponseData;
    }

    public void setLastResponseData(Object data) {
        this.lastResponseData = data;
    }
}
