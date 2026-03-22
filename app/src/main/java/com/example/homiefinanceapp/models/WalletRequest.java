package com.example.homiefinanceapp.models;

public class WalletRequest {
    private String name;
    private Double balance;
    private String color;

    public WalletRequest(String name, Double balance, String color) {
        this.name = name;
        this.balance = balance;
        this.color = color;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}