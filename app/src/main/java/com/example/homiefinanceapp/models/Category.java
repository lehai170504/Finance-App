package com.example.homiefinanceapp.models;

public class Category {
    private String id;
    private String name;
    private String type; // "INCOME" hoặc "EXPENSE"
    private String icon; // "ic_default", v.v.

    public Category(String name, String type, String icon) {
        this.name = name;
        this.type = type;
        this.icon = icon;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}