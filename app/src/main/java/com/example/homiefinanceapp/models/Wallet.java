package com.example.homiefinanceapp.models;

public class Wallet {
    private String id;
    private String name;
    private Double balance;
    private String color;

    // Getter và Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}