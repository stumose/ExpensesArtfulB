package com.stuartfuljourneys.expensesartfulb;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Expense {
    // --- Member Variables ---
    // These names must exactly match your Firestore document fields.
    private String paidBy_nickname;
    private String category;
    private double finalAmount;
    @ServerTimestamp
    private Date timestamp;
    private String note;
    private Boolean isShared;



    // IMPORTANT: A public, empty constructor is REQUIRED for Firestore.
    public Expense() {}


    // --- Getters ---
    // These methods allow other parts of your app to read the private variables.

    public String getPaidBy_nickname() {return paidBy_nickname; }

    public String getCategory() {return category;  }

    public double getFinalAmount() {return finalAmount; }

    public Date getTimestamp() {return timestamp; }

    public String getNote() {return note; }

    public Boolean isShared() {return isShared; }
}
