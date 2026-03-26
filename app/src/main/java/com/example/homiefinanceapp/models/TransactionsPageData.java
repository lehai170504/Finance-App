package com.example.homiefinanceapp.models;

import java.util.List;

public class TransactionsPageData {
    private List<TransactionListItem> content;
    private int currentPage;
    private int totalPages;
    private int totalElements;

    public List<TransactionListItem> getContent() {
        return content;
    }

    public void setContent(List<TransactionListItem> content) {
        this.content = content;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }
}

