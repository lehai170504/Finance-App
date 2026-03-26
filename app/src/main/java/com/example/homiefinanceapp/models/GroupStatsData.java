package com.example.homiefinanceapp.models;

import java.util.Map;

public class GroupStatsData {
    private Double totalExpense;
    private Map<String, Double> statsByCategory;
    private Map<String, Double> statsByUser;

    public Double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(Double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public Map<String, Double> getStatsByCategory() {
        return statsByCategory;
    }

    public void setStatsByCategory(Map<String, Double> statsByCategory) {
        this.statsByCategory = statsByCategory;
    }

    public Map<String, Double> getStatsByUser() {
        return statsByUser;
    }

    public void setStatsByUser(Map<String, Double> statsByUser) {
        this.statsByUser = statsByUser;
    }
}

