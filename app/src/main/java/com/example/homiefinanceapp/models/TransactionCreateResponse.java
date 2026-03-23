package com.example.homiefinanceapp.models;

public class TransactionCreateResponse {
    private String id;
    private Double amount;
    private String note;
    private String date;
    private Category category;
    private String receiptUrl;
    private Wallet wallet;
    private Group groupSpace;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public Group getGroupSpace() {
        return groupSpace;
    }

    public void setGroupSpace(Group groupSpace) {
        this.groupSpace = groupSpace;
    }
}

