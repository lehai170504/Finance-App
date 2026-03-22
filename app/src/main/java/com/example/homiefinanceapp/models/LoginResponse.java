package com.example.homiefinanceapp.models;

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String username;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public String getUsername() { return username; }
}