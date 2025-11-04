package com.stuartfuljourneys.expensesartfulb;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Expense> expenseList;

    public TransactionAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transactions, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView textPayer, textCategory, textAmount, textTimestamp;
        private ConstraintLayout transactionRowLayout;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textPayer = itemView.findViewById(R.id.textPayer);
            textCategory = itemView.findViewById(R.id.textCategory);
            textAmount = itemView.findViewById(R.id.textAmount);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            transactionRowLayout = itemView.findViewById(R.id.transaction_row_layout);
        }

        public void bind(Expense expense) {
            // --- Set Text Data ---
            textPayer.setText(expense.getPaidBy_nickname());
            textCategory.setText(expense.getCategory());
            textAmount.setText(String.format(Locale.US, "$%.2f", expense.getFinalAmount()));

            if (expense.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                textTimestamp.setText(sdf.format(expense.getTimestamp()));
            } else {
                textTimestamp.setText("No date");
            }

            // ===================================================================
            // ================== ADD THIS ONE DEBUGGING LINE ==================
            // ===================================================================
            Log.d("AdapterDebug", "Expense: " + expense.getCategory() + ", Amount: " + expense.getFinalAmount() + ", isShared Value: " + expense.isShared());
            // ===================================================================

            // ===================================================================
            // === FINAL, SIMPLIFIED LOGIC TO FIX THE RED COLORING ISSUE ===
            // ===================================================================
            // This single 'if' condition checks for the one case where we want red.
            // It reads: "If the isShared field is NOT NULL and its value is FALSE..."
            if (expense.isShared() != null && !expense.isShared()) {
                // The expense is explicitly marked as "Not Shared". Color it red.
                transactionRowLayout.setBackgroundColor(itemView.getContext().getColor(R.color.soft_red));
            } else {
                // In ALL other cases (isShared is true, or isShared is null/missing for old data),
                // use the default transparent background. This is crucial for recycled views.
                transactionRowLayout.setBackgroundColor(itemView.getContext().getColor(android.R.color.transparent));
            }
            // ===================================================================

            // --- Handle Click Listener to Show Note ---
            itemView.setOnClickListener(v -> {
                String note = expense.getNote();
                if (note != null && !note.trim().isEmpty()) {
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Transaction Note")
                            .setMessage(note)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    Toast.makeText(itemView.getContext(), "No note for this transaction.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
