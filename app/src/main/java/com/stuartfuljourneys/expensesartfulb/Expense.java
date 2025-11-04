package com.stuartfuljourneys.expensesartfulb;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Expense {
    // These variable names MUST match the field names in your Firestore documents
    private String paidBy_nickname;
    private String category;
    private double finalAmount;
    @ServerTimestamp // This tells Firestore to convert the server timestamp into a Java Date object
    private Date timestamp;
    private String note; // For V2, when we make items clickable

    // IMPORTANT: You need an empty constructor for Firestore's automatic data conversion
    public Expense() {}

    public Expense(String paidBy_nickname, String category, double finalAmount, Date timestamp, String note) {
        this.paidBy_nickname = paidBy_nickname;
        this.category = category;
        this.finalAmount = finalAmount;
        this.timestamp = timestamp;
        this.note = note;
    }

    // --- Getters ---
    public String getPaidBy_nickname() {
        return paidBy_nickname;
    }

    public String getCategory() {
        return category;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getNote() {
        return note;
    }
}
