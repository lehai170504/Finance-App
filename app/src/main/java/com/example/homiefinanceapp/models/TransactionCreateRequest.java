package com.example.homiefinanceapp.models;

public class TransactionCreateRequest {
    private Double amount;
    private String note;
    private String date; // format yyyy-MM-dd

    public TransactionCreateRequest(Double amount, String note, String date) {
        this.amount = amount;
        this.note = note;
        this.date = date;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

