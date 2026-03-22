package com.example.homiefinanceapp.models;

public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String role;

    // Getter và Setter để Android có thể lấy dữ liệu ra hiển thị
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}