package com.example.homiefinanceapp.models;

public class GroupRequest {
    private String name;

    public GroupRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}