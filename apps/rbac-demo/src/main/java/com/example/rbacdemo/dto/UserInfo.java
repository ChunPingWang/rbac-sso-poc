package com.example.rbacdemo.dto;

import java.util.Collections;
import java.util.List;

public class UserInfo {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private String subject;
    private boolean authenticated;

    private UserInfo() {}

    public static UserInfo anonymous() {
        UserInfo info = new UserInfo();
        info.username = "anonymous";
        info.roles = Collections.emptyList();
        info.authenticated = false;
        return info;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return username;
    }

    public static class Builder {
        private final UserInfo userInfo = new UserInfo();

        public Builder username(String username) {
            userInfo.username = username;
            return this;
        }

        public Builder email(String email) {
            userInfo.email = email;
            return this;
        }

        public Builder firstName(String firstName) {
            userInfo.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            userInfo.lastName = lastName;
            return this;
        }

        public Builder roles(List<String> roles) {
            userInfo.roles = roles != null ? roles : Collections.emptyList();
            return this;
        }

        public Builder subject(String subject) {
            userInfo.subject = subject;
            return this;
        }

        public UserInfo build() {
            userInfo.authenticated = true;
            return userInfo;
        }
    }
}
