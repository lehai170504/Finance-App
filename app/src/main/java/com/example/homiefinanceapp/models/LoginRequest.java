package com.example.homiefinanceapp.models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    // Đổi tên thành username để khớp với @NotBlank bên Spring Boot
    private String username;
    private String password;

    // Constructor: Truyền email của user vào biến username này
    public LoginRequest(String email, String password) {
        this.username = email;
        this.password = password;
    }

    // Getter/Setter (nếu cần)
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}