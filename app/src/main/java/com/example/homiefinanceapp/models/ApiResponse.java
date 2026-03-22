package com.example.homiefinanceapp.models;

public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}