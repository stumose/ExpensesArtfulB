package com.stuartfuljourneys.expensesartfulb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Expense> expenseList;

    public TransactionAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    // This method is called when the RecyclerView needs a new "row" to display.
    // It inflates your item_transactions.xml layout.
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transactions, parent, false);
        return new TransactionViewHolder(view);
    }

    // This method is called for each row to bind the data from an Expense object
    // to the TextViews in that row.
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.bind(expense);
    }

    // This method simply tells the RecyclerView how many items are in the list.
    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    // This inner class represents a single row in your list. It holds references
    // to the TextViews within your item_transactions.xml layout.
    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private TextView textPayer, textCategory, textAmount, textTimestamp;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find the TextViews from the layout
            textPayer = itemView.findViewById(R.id.textPayer);
            textCategory = itemView.findViewById(R.id.textCategory);
            textAmount = itemView.findViewById(R.id.textAmount);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
        }

        // The helper method to actually set the text on the views
        public void bind(Expense expense) {
            textPayer.setText(expense.getPaidBy_nickname());
            textCategory.setText(expense.getCategory());
            textAmount.setText(String.format(Locale.US, "$%.2f", expense.getFinalAmount()));

            // Format the Date object into a readable string like "28 Oct 2024"
            if (expense.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                textTimestamp.setText(sdf.format(expense.getTimestamp()));
            } else {
                textTimestamp.setText("No date");
            }

            // For V2, we would add an OnClickListener here:
            // itemView.setOnClickListener(v -> { ... show details ... });
        }
    }
}
